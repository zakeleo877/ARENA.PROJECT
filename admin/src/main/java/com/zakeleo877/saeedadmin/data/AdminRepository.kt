package com.zakeleo877.saeedadmin.data

import com.zakeleo877.saeedcore.data.remote.AdminApi
import com.zakeleo877.saeedcore.data.remote.AdminSession
import com.zakeleo877.saeedcore.data.remote.AuthManager
import com.zakeleo877.saeedcore.data.remote.AuthResult
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
) {
    val sessionHolder = AdminSessionHolder()

    suspend fun signIn(email: String, password: String): AuthResult {
        val result = authManager.signIn(email, password)
        if (result is AuthResult.Success) {
            sessionHolder.setSession(result.session)
            val authorized = checkAuthorization(result.session)
            sessionHolder.setAuthorization(authorized)
            if (authorized !is AdminAuthorization.Authorized) {
                sessionHolder.clear()
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

    suspend fun products(session: AdminSession): List<Product> = api.products(session.accessToken)

    suspend fun orders(session: AdminSession, status: OrderStatus? = null): List<Order> =
        api.orders(status)

    suspend fun updateOrderStatus(session: AdminSession, orderId: String, status: OrderStatus): Boolean =
        api.updateOrderStatus(orderId, status)

    suspend fun deleteProduct(session: AdminSession, id: String): Boolean = api.deleteProduct(id)

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
        id, name, price, description, imageUrl, category, available, sortOrder,
        originalPrice = originalPrice,
        rating = rating,
    )

    suspend fun uploadImage(session: AdminSession, fileName: String, bytes: ByteArray): String? =
        api.uploadProductImage(session.accessToken, fileName, bytes)

    suspend fun updateSettings(session: AdminSession, values: Map<String, String>): Boolean =
        api.updateSettings(values)

    suspend fun insertManualOrder(
        session: AdminSession,
        customer: String,
        phone: String,
        notes: String,
        items: List<Pair<String, Int>>,
    ): Boolean = api.insertManualOrder(customer, phone, notes, items)

    /** Reads the newest order number so the background worker only reports new work. */
    suspend fun latestOrder(session: AdminSession): Long? =
        api.orders(status = OrderStatus.NEW).firstOrNull()?.number

    suspend fun newOrdersSince(session: AdminSession, lastSeenNumber: Long): List<Order> =
        api.orders(status = OrderStatus.NEW).filter { it.number > lastSeenNumber }

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
