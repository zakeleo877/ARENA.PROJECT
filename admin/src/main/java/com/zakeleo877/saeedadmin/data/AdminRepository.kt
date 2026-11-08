package com.zakeleo877.saeedadmin.data

import com.zakeleo877.saeedcore.data.remote.AdminApi
import com.zakeleo877.saeedcore.data.remote.AdminSession
import com.zakeleo877.saeedcore.data.remote.AuthManager
import com.zakeleo877.saeedcore.data.remote.AuthResult
import com.zakeleo877.saeedcore.data.remote.SessionVault
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
import java.math.BigDecimal
import java.time.Instant
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
    private val vault: SessionVault? = null,
) {
    val sessionHolder = AdminSessionHolder()

    /**
     * Restores the session saved at the end of the last launch.
     *
     * Without it the manager typed an email and a password every time the app
     * was closed, which on a shop phone is several times an hour. The token may
     * be an hour old by now, and the first request refreshes it, so an expired
     * stored session is still worth restoring.
     *
     * A session the server no longer accepts is dropped by the refresh path, so
     * this cannot leave the app stuck behind a sign-in screen that never ends.
     */
    suspend fun restoreSession(): Boolean {
        val saved = vault?.load() ?: return false
        sessionHolder.setSession(saved)
        val authorization = checkAuthorization(saved)
        sessionHolder.setAuthorization(authorization)
        if (authorization !is AdminAuthorization.Authorized) {
            sessionHolder.clear()
            vault?.clear()
            return false
        }
        return true
    }

    fun signOut() {
        sessionHolder.clear()
        vault?.clear()
    }

    suspend fun signIn(email: String, password: String): AuthResult {
        val result = authManager.signIn(email, password)
        if (result is AuthResult.Success) {
            sessionHolder.setSession(result.session)
            val authorized = checkAuthorization(result.session)
            sessionHolder.setAuthorization(authorized)
            if (authorized !is AdminAuthorization.Authorized) {
                sessionHolder.clear()
                vault?.clear()
            } else {
                // Only a manager is remembered. Saving a refused session would
                // restore it on every launch and fail the check every time.
                vault?.save(result.session)
            }
        }
        return result
    }

    /** is_admin() decides; claim_first_admin() only succeeds once while admins is empty. */
    suspend fun checkAuthorization(session: AdminSession): AdminAuthorization {
        val isAdmin = rpcBoolean(session, "is_admin")
        if (isAdmin) return AdminAuthorization.Authorized
        val claimed = rpcBoolean(session, "claim_first_admin")
        return if (claimed) AdminAuthorization.Authorized else AdminAuthorization.NotAuthorized
    }


    /**
     * The session to use for a request, refreshed when it is about to expire.
     *
     * Supabase's access token lasts an hour, and `refresh()` existed without
     * every being called: at the hour mark every admin request started failing
     * with the same 401 as a missing token, and nothing told the manager why.
     * The refresh happens before the request rather than after the failure,
     * because a failed write has no way back.
     */
    private suspend fun liveSession(): AdminSession? {
        val current = sessionHolder.session.value ?: return null
        if (!current.isExpiring(System.currentTimeMillis())) return current
        return when (val result = authManager.refresh(current)) {
            is AuthResult.Success -> {
                sessionHolder.setSession(result.session)
                // Supabase rotates the refresh token, so the stored copy is
                // replaced or the next launch restores one that no longer works.
                vault?.save(result.session)
                result.session
            }
            // A refused refresh means the refresh token itself is gone, which
            // only a new sign-in can repair. Keeping the stale session would
            // keep sending a token that cannot work.
            is AuthResult.Failure -> {
                sessionHolder.clear()
                vault?.clear()
                null
            }
        }
    }

    suspend fun products(session: AdminSession): List<Product> =
        api.products((liveSession() ?: session).accessToken)

    suspend fun orders(session: AdminSession, status: OrderStatus? = null): List<Order> =
        api.orders((liveSession() ?: session).accessToken, status)

    suspend fun updateOrderStatus(session: AdminSession, orderId: String, status: OrderStatus): Boolean =
        api.updateOrderStatus((liveSession() ?: session).accessToken, orderId, status)

    suspend fun deleteProduct(session: AdminSession, id: String): Boolean =
        api.deleteProduct((liveSession() ?: session).accessToken, id)

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
        (liveSession() ?: session).accessToken,
        id, name, price, description, imageUrl, category, available, sortOrder,
        originalPrice = originalPrice,
        rating = rating,
    )

    suspend fun uploadImage(session: AdminSession, fileName: String, bytes: ByteArray): String? =
        api.uploadProductImage((liveSession() ?: session).accessToken, fileName, bytes)

    suspend fun updateSettings(session: AdminSession, values: Map<String, String>): Boolean =
        api.updateSettings((liveSession() ?: session).accessToken, values)

    suspend fun insertManualOrder(
        session: AdminSession,
        customer: String,
        phone: String,
        notes: String,
        items: List<Pair<String, Int>>,
    ): Boolean =
        api.insertManualOrder((liveSession() ?: session).accessToken, customer, phone, notes, items)

    /** Reads the newest order number so the background worker only reports new work. */
    suspend fun latestOrder(session: AdminSession): Long? =
        api.orders((liveSession() ?: session).accessToken, status = OrderStatus.NEW)
            .firstOrNull()?.number

    suspend fun newOrdersSince(session: AdminSession, lastSeenNumber: Long): List<Order> =
        api.orders((liveSession() ?: session).accessToken, status = OrderStatus.NEW)
            .filter { it.number > lastSeenNumber }

    /** Calls a boolean RPC with the admin's bearer token. */
    private suspend fun rpcBoolean(session: AdminSession, function: String): Boolean = try {
        val response = client.post("${config.baseUrl}/rest/v1/rpc/$function") {
            header("apikey", config.apiKey)
            header(HttpHeaders.Authorization, "Bearer ${session.accessToken}")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        if (!response.status.isSuccess()) {
            false
        } else {
            response.bodyAsText().trim().equals("true", ignoreCase = true)
        }
    } catch (_: Exception) {
        false
    }
}
