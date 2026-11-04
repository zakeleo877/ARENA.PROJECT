package com.zakeleo877.saeedcore.data.remote

import com.zakeleo877.saeedcore.model.PendingOrder
import com.zakeleo877.saeedcore.model.Product
import com.zakeleo877.saeedcore.model.StoreSettings
import com.zakeleo877.saeedcore.util.Validation
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

private val wireJson = Json { ignoreUnknownKeys = true }

fun anonymousHttpClient(engine: HttpClientEngine? = null): HttpClient {
    val selectedEngine = engine ?: OkHttp.create()
    return HttpClient(selectedEngine) {
        expectSuccess = false
        install(ContentNegotiation) { json(wireJson) }
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 20_000
        }
        // No request/response logging: orders contain private customer data.
    }
}

enum class ApiFailureKind { UNAUTHORIZED, INVALID_ORDER, RATE_LIMITED, SERVER, OTHER }

/** Deliberately excludes the response body and customer details from exception messages. */
class ApiFailure(val status: Int, val kind: ApiFailureKind) : Exception("PostgREST request failed ($status, $kind)")

@Serializable
private data class ProductWire(
    val id: String,
    val name: String,
    val price: JsonElement,
    val description: String,
    @SerialName("image_url") val imageUrl: String,
    val category: String,
    val available: Boolean,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("created_at") val createdAt: String,
) {
    fun model(): Product {
        val decimal = price.jsonPrimitive.contentOrNull?.toBigDecimalOrNull()
        require(decimal != null && decimal.signum() >= 0) { "Invalid catalog price." }
        val timestamp: Instant = OffsetDateTime.parse(createdAt).toInstant()
        return Product(id, name, decimal, description, imageUrl, category, available, sortOrder, timestamp)
    }
}

@Serializable
private data class SettingWire(val key: String, val value: String)

@Serializable
private data class OrderItemWire(@SerialName("product_id") val productId: String, val qty: Int)

@Serializable
private data class OrderRequestWire(
    @SerialName("p_customer") val customer: String,
    @SerialName("p_phone") val phone: String,
    @SerialName("p_notes") val notes: String,
    @SerialName("p_location") val customerLocationUrl: String,
    @SerialName("p_items") val items: List<OrderItemWire>,
    @SerialName("p_client_id") val clientId: String,
)

@Serializable
private data class PostgrestError(val message: String? = null)

class CatalogApi(private val client: HttpClient, private val config: SupabasePublicConfig) {
    suspend fun products(): List<Product> {
        val response = client.get("${config.baseUrl}/rest/v1/products") {
            publicHeaders()
            parameter("select", "id,name,price,description,image_url,category,available,sort_order,created_at")
            parameter("order", "sort_order.asc,created_at.desc")
        }
        ensureSuccess(response)
        return response.body<List<ProductWire>>().map(ProductWire::model)
    }

    suspend fun settings(): StoreSettings {
        val response = client.get("${config.baseUrl}/rest/v1/settings") {
            publicHeaders()
            parameter("select", "key,value")
        }
        ensureSuccess(response)
        val values = response.body<List<SettingWire>>().associate { it.key to it.value }
        fun required(key: String): String = requireNotNull(values[key]) { "Required public setting missing: $key" }
        return StoreSettings(
            storeName = required("storeName"),
            whatsapp = required("whatsapp"),
            currency = required("currency"),
            footerNote = required("footerNote"),
        )
    }

    /** The wire payload carries IDs and quantities, never a client-supplied price or total. */
    suspend fun placeOrder(order: PendingOrder): Long {
        require(Validation.customerName(order.customer) && Validation.phone(order.phone)) { "Invalid customer or phone." }
        require(Validation.notes(order.notes) && Validation.locationUrl(order.customerLocationUrl)) { "Invalid notes or location." }
        require(order.items.size in 1..50 && order.items.all { Validation.quantity(it.quantity) }) { "Invalid items." }
        try {
            UUID.fromString(order.clientId)
            order.items.forEach { UUID.fromString(it.productId) }
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid client or product ID.")
        }
        val payload = OrderRequestWire(
            customer = order.customer.trim(),
            phone = order.phone,
            notes = order.notes,
            customerLocationUrl = order.customerLocationUrl,
            items = order.items.map { OrderItemWire(it.productId, it.quantity) },
            clientId = order.clientId,
        )
        val response = client.post("${config.baseUrl}/rest/v1/rpc/place_order") {
            publicHeaders()
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        ensureSuccess(response)
        return response.bodyAsText().trim().toLongOrNull()?.takeIf { it > 0 }
            ?: throw IllegalArgumentException("Invalid order number returned by server.")
    }

    private fun io.ktor.client.request.HttpRequestBuilder.publicHeaders() {
        header("apikey", config.publicKey)
        header(HttpHeaders.Accept, ContentType.Application.Json)
        // Publishable keys are not JWTs; only the old anon JWT belongs in Bearer.
        if (config.legacyAnonJwt) header(HttpHeaders.Authorization, "Bearer ${config.publicKey}")
    }

    private suspend fun ensureSuccess(response: HttpResponse) {
        if (response.status.isSuccess()) return
        val message = try {
            wireJson.decodeFromString<PostgrestError>(response.bodyAsText()).message
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) { null }
        val kind = when {
            response.status.value == 401 || response.status.value == 403 -> ApiFailureKind.UNAUTHORIZED
            message == "rate_limited" -> ApiFailureKind.RATE_LIMITED
            message in setOf("invalid_items", "invalid_customer", "invalid_phone", "invalid_notes", "invalid_location") -> ApiFailureKind.INVALID_ORDER
            response.status.value >= 500 -> ApiFailureKind.SERVER
            else -> ApiFailureKind.OTHER
        }
        throw ApiFailure(response.status.value, kind)
    }
}
