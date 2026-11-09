package com.zakeleo877.saeedcore.data

import com.zakeleo877.saeedcore.data.local.PendingOrderDao
import com.zakeleo877.saeedcore.data.local.PendingOrderEntity
import com.zakeleo877.saeedcore.data.remote.ApiFailure
import com.zakeleo877.saeedcore.data.remote.ApiFailureKind
import com.zakeleo877.saeedcore.data.remote.CatalogApi
import com.zakeleo877.saeedcore.model.PendingOrder
import com.zakeleo877.saeedcore.model.PendingOrderItem
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Lifecycle of a queued order that could not be sent yet. */
enum class PendingState { WAITING, SENDING, FAILED }

data class PendingOrderRow(
    val clientId: String,
    val customer: String,
    val phone: String,
    val notes: String,
    /** Contact-only map link for shop-to-customer contact. */
    val customerLocationUrl: String,
    val items: List<PendingOrderItem>,
    val state: PendingState,
    val attemptCount: Int,
    val lastError: String?,
)

sealed interface SubmitOutcome {
    data class Sent(val orderNumber: Long) : SubmitOutcome
    data class Queued(val clientId: String) : SubmitOutcome
    data class Rejected(val reason: String) : SubmitOutcome
}

/**
 * Sends orders and keeps a durable local queue. An order is removed only after
 * a confirmed server number or an explicit customer deletion. The same client
 * ID is retained through retries, including an interrupted send.
 */
class OrderRepository(
    private val api: CatalogApi,
    private val pendingDao: PendingOrderDao,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val json = Json { ignoreUnknownKeys = true }
    // Foreground, connectivity callback and WorkManager share this singleton.
    private val sendMutex = Mutex()

    fun observePending(): Flow<List<PendingOrderRow>> = pendingDao.observeAll().map { rows ->
        rows.map { row ->
            PendingOrderRow(
                clientId = row.clientId,
                customer = row.customer,
                phone = row.phone,
                notes = row.notes,
                customerLocationUrl = row.location,
                items = decodeItems(row.itemsJson),
                state = runCatching { PendingState.valueOf(row.state) }.getOrDefault(PendingState.WAITING),
                attemptCount = row.attemptCount,
                lastError = row.lastErrorCode,
            )
        }
    }

    /** Persist before scheduling or sending, even if the phone is online. */
    suspend fun submit(
        order: PendingOrder,
        online: Boolean,
        afterPersist: () -> Unit = {},
    ): SubmitOutcome = sendMutex.withLock {
        queue(order, if (online) PendingState.SENDING else PendingState.WAITING)
        // Work may start immediately, but it cannot enter this mutex until the
        // foreground send has finished. If the process dies, the row survives.
        afterPersist()
        if (online) sendLocked(order) else SubmitOutcome.Queued(order.clientId)
    }

    /** Call only with [sendMutex] held and an existing durable row. */
    private suspend fun sendLocked(order: PendingOrder): SubmitOutcome {
        return try {
            val number = api.placeOrder(order)
            try {
                pendingDao.delete(order.clientId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // The server already confirmed it. A leftover SENDING row can
                // be safely reconciled with the same client ID on the next run.
            }
            SubmitOutcome.Sent(number)
        } catch (cancelled: CancellationException) {
            // Leave SENDING in place; the next worker retries the same ID.
            throw cancelled
        } catch (failure: ApiFailure) {
            val reason = failure.kind.toReason()
            when (failure.kind) {
                ApiFailureKind.SERVER, ApiFailureKind.RATE_LIMITED -> keepWaiting(order.clientId, reason)
                else -> keepFailed(order.clientId, reason)
            }
        } catch (_: IOException) {
            keepWaiting(order.clientId, "network_error")
        } catch (_: SerializationException) {
            // The order might already exist remotely despite the bad response.
            keepWaiting(order.clientId, "invalid_response")
        } catch (_: IllegalArgumentException) {
            keepFailed(order.clientId, "invalid_request")
        } catch (_: Exception) {
            // Preserve customer data even for an unexpected local failure.
            keepFailed(order.clientId, "request_failed")
        }
    }

    suspend fun queue(order: PendingOrder, state: PendingState = PendingState.WAITING, error: String? = null) {
        pendingDao.insert(
            PendingOrderEntity(
                clientId = order.clientId,
                customer = order.customer,
                phone = order.phone,
                notes = order.notes,
                location = order.customerLocationUrl,
                itemsJson = encodeItems(order.items),
                state = state.name,
                attemptCount = 0,
                createdAtMillis = nowMillis(),
                lastErrorCode = error,
            ),
        )
    }

    suspend fun retry(clientId: String, afterPersist: () -> Unit = {}): SubmitOutcome =
        sendMutex.withLock { retryLocked(clientId, afterPersist) }

    private suspend fun retryLocked(clientId: String, afterPersist: () -> Unit = {}): SubmitOutcome {
        val entity = pendingDao.find(clientId) ?: return SubmitOutcome.Rejected("not_found")
        pendingDao.update(entity.copy(state = PendingState.SENDING.name, lastErrorCode = null))
        afterPersist()
        return sendLocked(
            PendingOrder(
                clientId = entity.clientId,
                customer = entity.customer,
                phone = entity.phone,
                notes = entity.notes,
                customerLocationUrl = entity.location,
                items = decodeItems(entity.itemsJson),
            ),
        )
    }

    suspend fun delete(clientId: String) = sendMutex.withLock { pendingDao.delete(clientId) }

    /**
     * Recover waiting/interrupted rows and transient failures saved by older
     * versions. Invalid orders remain visible for an explicit customer action.
     */
    suspend fun drainWaiting(): List<SubmitOutcome> = sendMutex.withLock {
        val retryableErrors = setOf("network_error", "server_error", "rate_limited", "invalid_response")
        val ids = pendingDao.observeAll().first()
            .filter {
                it.state == PendingState.WAITING.name || it.state == PendingState.SENDING.name ||
                    (it.state == PendingState.FAILED.name && it.lastErrorCode in retryableErrors)
            }
            .map { it.clientId }
        ids.map { retryLocked(it) }
    }

    private suspend fun keepWaiting(clientId: String, reason: String): SubmitOutcome {
        updateAfterFailure(clientId, PendingState.WAITING, reason)
        return SubmitOutcome.Queued(clientId)
    }

    private suspend fun keepFailed(clientId: String, reason: String): SubmitOutcome {
        updateAfterFailure(clientId, PendingState.FAILED, reason)
        return SubmitOutcome.Rejected(reason)
    }

    private suspend fun updateAfterFailure(clientId: String, state: PendingState, reason: String) {
        val existing = pendingDao.find(clientId) ?: return
        pendingDao.update(
            existing.copy(
                state = state.name,
                attemptCount = existing.attemptCount + 1,
                lastErrorCode = reason,
            ),
        )
    }

    @kotlinx.serialization.Serializable
    private data class PendingItemWire(val productId: String, val quantity: Int)

    private fun encodeItems(items: List<PendingOrderItem>): String =
        json.encodeToString(items.map { PendingItemWire(it.productId, it.quantity) })

    private fun decodeItems(raw: String): List<PendingOrderItem> = runCatching {
        json.decodeFromString<List<PendingItemWire>>(raw).map { PendingOrderItem(it.productId, it.quantity) }
    }.getOrDefault(emptyList())

    private fun ApiFailureKind.toReason(): String = when (this) {
        ApiFailureKind.UNAUTHORIZED -> "unauthorized"
        ApiFailureKind.INVALID_ORDER -> "invalid_order"
        ApiFailureKind.RATE_LIMITED -> "rate_limited"
        ApiFailureKind.SERVER -> "server_error"
        ApiFailureKind.OTHER -> "request_failed"
    }
}
