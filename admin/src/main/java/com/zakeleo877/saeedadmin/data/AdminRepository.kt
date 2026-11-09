package com.zakeleo877.saeedadmin.data

import com.zakeleo877.saeedcore.data.remote.AdminApi
import com.zakeleo877.saeedcore.data.remote.AdminSession
import com.zakeleo877.saeedcore.data.remote.AuthManager
import com.zakeleo877.saeedcore.data.remote.AuthError
import com.zakeleo877.saeedcore.data.remote.AuthResult
import com.zakeleo877.saeedcore.data.remote.SessionStore
import com.zakeleo877.saeedcore.data.remote.SupabasePublicConfig
import com.zakeleo877.saeedcore.model.Order
import com.zakeleo877.saeedcore.model.OrderStatus
import com.zakeleo877.saeedcore.model.Product
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.client.statement.bodyAsText
import java.io.IOException
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Result of the authorization checks the admin app performs after signing in. */
sealed interface AdminAuthorization {
    data object Authorized : AdminAuthorization
    data object NotAuthorized : AdminAuthorization
}

/** Holds the current session and the authorization decision. */
class AdminSessionHolder {
    private val _session = MutableStateFlow<AdminSession?>(null)
    val session: StateFlow<AdminSession?> = _session.asStateFlow()

    private val _authorization = MutableStateFlow<AdminAuthorization?>(null)
    val authorization: StateFlow<AdminAuthorization?> = _authorization.asStateFlow()

    fun setSession(session: AdminSession?) {
        _session.value = session
    }

    fun setAuthorization(authorization: AdminAuthorization?) {
        _authorization.value = authorization
    }

    fun clear() {
        _session.value = null
        _authorization.value = null
    }
}

/**
 * Admin-side data access. All requests are authenticated with the admin's own
 * access token, so row-level security remains the only authority.
 */
class AdminRepository(
    private val client: HttpClient,
    private val config: SupabasePublicConfig,
    private val api: AdminApi,
    private val authManager: AuthManager,
    private val vault: SessionStore? = null,
) {
    val sessionHolder = AdminSessionHolder()
    // A rotating refresh token must never be refreshed by two simultaneous
    // foreground/background requests. Sign-in and sign-out use the same lock.
    private val sessionMutex = Mutex()

    /** Restore the encrypted session, refreshing it before authorization if needed. */
    suspend fun restoreSession(): Boolean = sessionMutex.withLock {
        if (sessionHolder.session.value != null) return@withLock true
        val saved = vault?.load() ?: return@withLock false
        val current = refreshIfNeededLocked(saved) ?: return@withLock false
        when (val authorization = checkAuthorization(current)) {
            AdminAuthorization.Authorized -> {
                sessionHolder.setAuthorization(authorization)
                sessionHolder.setSession(current)
                true
            }
            AdminAuthorization.NotAuthorized -> {
                sessionHolder.clear()
                sessionHolder.setAuthorization(authorization)
                vault?.clear()
                false
            }
            // A network/server failure is not evidence that the admin lost
            // access. Keep the encrypted token and try again later.
            null -> false
        }
    }

    /** The server's own words for the last refused write, for the screen. */
    val lastFailure: String? get() = api.lastFailure

    suspend fun signOut() = sessionMutex.withLock {
        sessionHolder.clear()
        vault?.clear()
    }

    suspend fun signIn(email: String, password: String): AuthResult = sessionMutex.withLock {
        val result = authManager.signIn(email, password)
        if (result !is AuthResult.Success) return@withLock result
        when (val authorization = checkAuthorization(result.session)) {
            AdminAuthorization.Authorized -> {
                vault?.save(result.session)
                sessionHolder.setAuthorization(authorization)
                sessionHolder.setSession(result.session)
                result
            }
            AdminAuthorization.NotAuthorized -> {
                sessionHolder.clear()
                sessionHolder.setAuthorization(authorization)
                vault?.clear()
                result
            }
            null -> AuthResult.Failure(AuthError.SERVER)
        }
    }

    /** Null means the authorization service could not be reached, not a denial. */
    suspend fun checkAuthorization(session: AdminSession): AdminAuthorization? {
        when (rpcBoolean(session, "is_admin")) {
            true -> return AdminAuthorization.Authorized
            null -> return null
            false -> Unit
        }
        return when (rpcBoolean(session, "claim_first_admin")) {
            true -> AdminAuthorization.Authorized
            false -> AdminAuthorization.NotAuthorized
            null -> null
        }
    }

    /** Must be called under [sessionMutex]. */
    private suspend fun refreshIfNeededLocked(current: AdminSession): AdminSession? {
        if (!current.isExpiring(System.currentTimeMillis())) return current
        return when (val result = authManager.refresh(current)) {
            is AuthResult.Success -> {
                // Refresh tokens rotate: persist the replacement even if the
                // following authorization check is temporarily unavailable.
                vault?.save(result.session)
                result.session
            }
            is AuthResult.Failure -> {
                if (result.reason == AuthError.INVALID_CREDENTIALS) {
                    sessionHolder.clear()
                    vault?.clear()
                }
                // Network, throttling and 5xx errors must not erase the vault.
                null
            }
        }
    }

    private suspend fun liveSession(): AdminSession? = sessionMutex.withLock {
        val current = sessionHolder.session.value ?: return@withLock null
        val refreshed = refreshIfNeededLocked(current) ?: return@withLock null
        if (refreshed != current) sessionHolder.setSession(refreshed)
        refreshed
    }

    private suspend fun tokenFor(expected: AdminSession): String {
        val current = liveSession() ?: throw IOException("Admin session is temporarily unavailable.")
        if (current.userId != expected.userId) throw IOException("Admin session has changed.")
        return current.accessToken
    }

    /**
     * Asks the project four questions that separate the four ways "nothing
     * appears" happens: wrong address, wrong key, unapplied schema, or an
     * account that is not an admin.
     */
    suspend fun diagnose(session: AdminSession): List<com.zakeleo877.saeedcore.data.remote.DiagnosticCheck> =
        api.diagnose(tokenFor(session))

    suspend fun products(session: AdminSession): List<Product> =
        api.products(tokenFor(session))

    suspend fun orders(session: AdminSession, status: OrderStatus? = null): List<Order> =
        api.orders(tokenFor(session), status)

    suspend fun updateOrderStatus(session: AdminSession, orderId: String, status: OrderStatus): Boolean =
        api.updateOrderStatus(tokenFor(session), orderId, status)

    suspend fun deleteProduct(session: AdminSession, id: String): Boolean =
        api.deleteProduct(tokenFor(session), id)

    suspend fun upsertProduct(
        session: AdminSession,
        id: String?,
        name: String,
        price: BigDecimal,
        description: String,
        imageUrl: String,
        category: String,
        available: Boolean,
        sortOrder: Int,
        originalPrice: BigDecimal? = null,
        rating: Double? = null,
    ): Boolean = api.upsertProduct(
        tokenFor(session),
        id, name, price, description, imageUrl, category, available, sortOrder,
        originalPrice = originalPrice,
        rating = rating,
    )

    suspend fun uploadImage(session: AdminSession, fileName: String, bytes: ByteArray): String? =
        api.uploadProductImage(tokenFor(session), fileName, bytes)

    suspend fun settings(session: AdminSession): Map<String, String> = api.settings(tokenFor(session))

    suspend fun updateSettings(session: AdminSession, values: Map<String, String>): Boolean =
        api.updateSettings(tokenFor(session), values)

    suspend fun insertManualOrder(
        session: AdminSession,
        customer: String,
        phone: String,
        notes: String,
        items: List<Pair<String, Int>>,
    ): Boolean =
        api.insertManualOrder(tokenFor(session), customer, phone, notes, items)

    /** Reads the newest order number so the background worker only reports new work. */
    suspend fun latestOrder(session: AdminSession): Long? =
        api.orders(tokenFor(session), status = OrderStatus.NEW)
            .firstOrNull()?.number

    suspend fun newOrderNumbersSince(session: AdminSession, lastSeenNumber: Long): List<Long> =
        api.newOrderNumbers(tokenFor(session), lastSeenNumber)

    /** Calls a boolean RPC with the admin's bearer token. */
    private suspend fun rpcBoolean(session: AdminSession, function: String): Boolean? = try {
        val response = client.post("${config.baseUrl}/rest/v1/rpc/$function") {
            header("apikey", config.apiKey)
            header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        if (!response.status.isSuccess()) {
            null
        } else {
            when (response.bodyAsText().trim().lowercase()) {
                "true" -> true
                "false" -> false
                else -> null
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
}
