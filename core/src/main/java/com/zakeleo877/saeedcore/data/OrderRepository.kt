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
 * Sends orders and keeps a durable local queue for offline use.
 * The clientId is created once per order and reused on every retry, so the
 * server returns the same invoice number and only one row is ever stored.
 */
class OrderRepository(
    private val api: CatalogApi,
    private val pendingDao: PendingOrderDao,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val json = Json { ignoreUnknownKeys = true }

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

    /**
     * Sends immediately when possible. If the device is offline the order is queued
     * and the caller shows the "will be sent automatically" message.
     */
    suspend fun submit(order: PendingOrder, online: Boolean): SubmitOutcome {
        if (!online) {
            queue(order, PendingState.WAITING, null)
            return SubmitOutcome.Queued(order.clientId)
        }
        // The order is stored before the first attempt, so a transient failure
        // never loses the customer order. The same clientId is reused on every
        // retry, so the server returns the same invoice number.
        queue(order, PendingState.SENDING, null)
        return send(order)
    }

    /** Sends a queued order. Keeps the same clientId so the server de-duplicates. */
    suspend fun send(order: PendingOrder): SubmitOutcome {
        return try {
            val number = api.placeOrder(order)
            pendingDao.delete(order.clientId)
            SubmitOutcome.Sent(number)
        } catch (failure: ApiFailure) {
            val reason = failure.kind.toReason()
            if (failure.kind == ApiFailureKind.INVALID_ORDER) {
                // A permanent rejection must not loop forever. The decision is
                // deliberate and a test holds it: retrying bad data can only
                // fail again.
                pendingDao.delete(order.clientId)
                SubmitOutcome.Rejected(reason)
            } else {
                markFailed(order.clientId, reason)
                SubmitOutcome.Rejected(reason)
            }
        } catch (_: IOException) {
            markFailed(order.clientId, "network_error")
            SubmitOutcome.Rejected("network_error")
        } catch (_: SerializationException) {
            // The order may have reached the server; keeping the row with the
            // same client id means a retry returns the original number rather
            // than placing a second order.
            markFailed(order.clientId, "invalid_response")
            SubmitOutcome.Rejected("invalid_response")
        } catch (_: IllegalArgumentException) {
            pendingDao.delete(order.clientId)
            SubmitOutcome.Rejected("invalid_request")
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

    suspend fun retry(clientId: String): SubmitOutcome {
        val entity = pendingDao.find(clientId) ?: return SubmitOutcome.Rejected("not_found")
        val order = PendingOrder(
            clientId = entity.clientId,
            customer = entity.customer,
            phone = entity.phone,
            notes = entity.notes,
            customerLocationUrl = entity.location,
            items = decodeItems(entity.itemsJson),
        )
        pendingDao.delete(clientId)
        return send(order)
    }

    suspend fun delete(clientId: String) {
        pendingDao.delete(clientId)
    }

    /**
     * Sends every waiting order once the device is online again.
     *
     * The offline banner tells the customer the order will be sent when the
     * connection returns, and nothing did that: the customer had to open the
     * list and tap retry. This is the promise being kept.
     *
     * Orders that failed are left alone. A failure may be a permanent rejection
     * the customer has to fix, and retrying it every few seconds would hide the
     * reason behind a spinner.
     */
    suspend fun drainWaiting(): List<SubmitOutcome> {
        val waiting = pendingDao.observeAll()
            .first()
            .filter { it.state == PendingState.WAITING.name }
        return waiting.map { entity ->
            val order = PendingOrder(
                clientId = entity.clientId,
                customer = entity.customer,
                phone = entity.phone,
                notes = entity.notes,
                customerLocationUrl = entity.location,
                items = decodeItems(entity.itemsJson),
            )
            send(order)
        }
    }

    /**
     * Records a failed attempt. When the order is not stored yet the caller
     * only knows the client id, so the entry is created through [queue] first.
     */
    private suspend fun markFailed(clientId: String, reason: String) {
        val existing = pendingDao.find(clientId) ?: return
        pendingDao.delete(clientId)
        pendingDao.insert(
            existing.copy(
                state = PendingState.FAILED.name,
                attemptCount = existing.attemptCount + 1,
                lastErrorCode = reason,
            ),
        )
    }

    /** Serializable shape of a queued item, so the snapshot survives a restart. */
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
