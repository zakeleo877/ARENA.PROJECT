package com.zakeleo877.saeedcore.data.remote

import com.zakeleo877.saeedcore.model.Order
import com.zakeleo877.saeedcore.model.OrderChannel
import com.zakeleo877.saeedcore.model.OrderItem
import com.zakeleo877.saeedcore.model.OrderStatus
import com.zakeleo877.saeedcore.model.Product
import com.zakeleo877.saeedcore.model.StoreSettings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
private data class ProductUpsertWire(
    val name: String,
    val price: String,
    val description: String,
    @SerialName("image_url") val imageUrl: String,
    val category: String,
    val available: Boolean,
    @SerialName("sort_order") val sortOrder: Int,
    /** Null clears the discount; the column allows null by design. */
    @SerialName("original_price") val originalPrice: String? = null,
    @SerialName("rating") val rating: String? = null,
)

@Serializable
private data class OrderItemDetailWire(
    @SerialName("product_id") val productId: String? = null,
    val name: String,
    val price: String,
    val qty: Int,
)

@Serializable
private data class OrderWire(
    val id: String,
    val number: Long,
    val customer: String,
    val phone: String,
    val notes: String,
    /** Contact-only map link for shop-to-customer contact. */
    @SerialName("location") val customerLocationUrl: String,
    val channel: String,
    val status: String,
    val details: List<OrderItemDetailWire>,
    val total: String,
    @SerialName("client_id") val clientId: String? = null,
    @SerialName("created_at") val createdAt: String,
) {
    fun model(): Order = Order(
        id = id,
        number = number,
        customer = customer,
        phone = phone,
        notes = notes,
        customerLocationUrl = customerLocationUrl,
        channel = if (channel == OrderChannel.MANUAL.databaseValue) OrderChannel.MANUAL else OrderChannel.APP,
        status = runCatching { OrderStatus.fromDatabase(status) }.getOrDefault(OrderStatus.NEW),
        items = details.map {
            OrderItem(it.productId.orEmpty(), it.name, it.price.toBigDecimal(), it.qty)
        },
        total = total.toBigDecimal(),
        clientId = clientId,
        createdAt = OffsetDateTime.parse(createdAt).toInstant(),
    )
}

@Serializable
private data class SettingUpsertWire(val key: String, val value: String)

@Serializable
private data class ProductImageWire(
    @SerialName("path") val path: String? = null,
    @SerialName("publicUrl") val publicUrl: String? = null,
)

/**
 * Admin-side PostgREST access. Every call carries the admin's own bearer token,
 * so RLS alone decides what is visible. The customer app never uses this class.
 */
/** How many orders one screen asks for, and the ceiling for a caller. */
private const val ORDERS_PAGE_SIZE = 100
private const val MAX_ORDERS_PAGE_SIZE = 500

class AdminApi(
    private val client: HttpClient,
    private val config: SupabasePublicConfig,
) {
    /**
     * Admins may read hidden products too; the customer app only sees available ones.
     *
     * The token is required. The `products read` policy is
     * `available or is_admin()`, so an anonymous request silently returns the
     * available rows only: a product switched off would disappear from the
     * management list instead of showing as hidden, and could never be edited
     * back on.
     */
    suspend fun products(token: String): List<Product> {
        val response = client.get("${config.baseUrl}/rest/v1/products") {
            headers(token)
            parameter("select", "id,name,price,description,image_url,category,available,sort_order,created_at,original_price,rating")
            parameter("order", "sort_order.asc,created_at.desc")
        }
        ensureSuccess(response)
        return response.body<List<JsonObject>>().map { productFrom(it) }
    }

    /**
     * The newest orders, at most [limit] of them.
     *
     * The screen never needs the whole table, and the table grows without a
     * ceiling: a shop that takes 200 orders a day has 73000 rows after a year.
     * Asking for all of them meant an ever larger response, an ever longer
     * parse, and a list that the user had to scroll instead of search. The
     * server stops at [limit] rows and the index provides the order, so the
     * cost of one screen does not grow with the age of the shop.
     */
    suspend fun orders(token: String, status: OrderStatus? = null, limit: Int = ORDERS_PAGE_SIZE): List<Order> {
        val response = client.get("${config.baseUrl}/rest/v1/orders") {
            headers(token)
            parameter("select", "id,number,customer,phone,notes,location,channel,status,details,total,client_id,created_at")
            parameter("order", "created_at.desc")
            parameter("limit", limit.coerceIn(1, MAX_ORDERS_PAGE_SIZE))
            if (status != null) parameter("status", "eq.${status.databaseValue}")
        }
        ensureSuccess(response)
        return response.body<List<OrderWire>>().map(OrderWire::model)
    }

    suspend fun updateOrderStatus(token: String, orderId: String, status: OrderStatus): Boolean {
        val response = client.patch("${config.baseUrl}/rest/v1/orders") {
            headers(token)
            parameter("id", "eq.$orderId")
            contentType(ContentType.Application.Json)
            setBody(mapOf("status" to status.databaseValue))
        }
        return response.status.isSuccess()
    }

    suspend fun insertManualOrder(
        token: String,
        customer: String,
        phone: String,
        notes: String,
        items: List<Pair<String, Int>>,
    ): Boolean {
        val response = client.post("${config.baseUrl}/rest/v1/orders") {
            headers(token)
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "customer" to customer,
                    "phone" to phone,
                    "notes" to notes,
                    "channel" to OrderChannel.MANUAL.databaseValue,
                    "details" to items.map { (id, qty) ->
                        mapOf("product_id" to id, "qty" to qty)
                    },
                ),
            )
        }
        return response.status.isSuccess()
    }

    suspend fun upsertProduct(
        token: String,
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
    ): Boolean {
        val payload = ProductUpsertWire(
            name = name,
            price = price.toPlainString(),
            description = description,
            imageUrl = imageUrl,
            category = category,
            available = available,
            sortOrder = sortOrder,
            // An old price that is not above the current one is not a discount;
            // sending it would be rejected by the database check anyway.
            originalPrice = originalPrice?.takeIf { it > price }?.toPlainString(),
            rating = rating?.takeIf { it in 0.0..5.0 }?.let { String.format(java.util.Locale.US, "%.1f", it) },
        )
        val response = if (id == null) {
            client.post("${config.baseUrl}/rest/v1/products") {
                headers(token)
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
        } else {
            client.patch("${config.baseUrl}/rest/v1/products") {
                headers(token)
                parameter("id", "eq.$id")
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
        }
        return response.status.isSuccess()
    }

    suspend fun deleteProduct(token: String, id: String): Boolean {
        val response = client.delete("${config.baseUrl}/rest/v1/products") {
            headers(token)
            parameter("id", "eq.$id")
        }
        return response.status.isSuccess()
    }

    suspend fun updateSettings(token: String, values: Map<String, String>): Boolean {
        val response = client.post("${config.baseUrl}/rest/v1/settings") {
            headers(token)
            contentType(ContentType.Application.Json)
            header("Prefer", "resolution=merge-duplicates,return=minimal")
            setBody(values.map { (key, value) -> SettingUpsertWire(key, value) })
        }
        return response.status.isSuccess()
    }

    /**
     * Uploads a compressed product image to the bucket and returns its public URL.
     *
     * The bearer token is mandatory here: the bucket policies allow insert only
     * to `authenticated` users for whom `is_admin()` is true, so a request that
     * carries the publishable key alone is rejected with 403. The upload used to
     * be sent without it, which made every call fail even though the rest of the
     * chain was in place.
     */
    suspend fun uploadProductImage(token: String, fileName: String, bytes: ByteArray): String? {
        val response = client.post("${config.baseUrl}/storage/v1/object/product-images/$fileName") {
            headers(token)
            // Ktor has no WebP constant, so the MIME type is parsed explicitly.
            contentType(ContentType.parse("image/webp"))
            setBody(bytes)
        }
        if (!response.status.isSuccess()) return null
        val wire = runCatching { response.body<ProductImageWire>() }.getOrNull()
        return wire?.publicUrl
            ?: wire?.path?.let { "${config.baseUrl}/storage/v1/object/public/product-images/$it" }
            ?: "${config.baseUrl}/storage/v1/object/public/product-images/$fileName"
    }

    suspend fun deleteProductImage(token: String, path: String): Boolean {
        val response = client.delete("${config.baseUrl}/storage/v1/object/product-images/$path") {
            headers(token)
        }
        return response.status.isSuccess()
    }

    private fun io.ktor.client.request.HttpRequestBuilder.headers(token: String? = null) {
        header("apikey", config.publicKey)
        header(HttpHeaders.Accept, ContentType.Application.Json)
        if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
    }

    private suspend fun ensureSuccess(response: io.ktor.client.statement.HttpResponse) {
        if (response.status.isSuccess()) return
        val status = response.status.value
        throw ApiFailure(status, if (status == 401 || status == 403) ApiFailureKind.UNAUTHORIZED else ApiFailureKind.SERVER)
    }

    private fun productFrom(json: JsonObject): Product {
        val id = json.getValue("id").jsonPrimitive.content
        val name = json.getValue("name").jsonPrimitive.content
        val price = json.getValue("price").jsonPrimitive.contentOrNull?.toBigDecimalOrNull()
            ?: throw IllegalArgumentException("Invalid product price.")
        val description = json.getValue("description").jsonPrimitive.content
        val imageUrl = json.getValue("image_url").jsonPrimitive.content
        val category = json.getValue("category").jsonPrimitive.content
        val available = json.getValue("available").jsonPrimitive.content.toBoolean()
        val sortOrder = json.getValue("sort_order").jsonPrimitive.content.toInt()
        val createdAt = OffsetDateTime.parse(json.getValue("created_at").jsonPrimitive.content).toInstant()
        val originalPrice = json["original_price"].asDecimalOrNull()?.takeIf { it >= price }
        val rating = json["rating"].asDecimalOrNull()?.toDouble()?.takeIf { it in 0.0..5.0 }
        return Product(
            id, name, price, description, imageUrl, category, available, sortOrder, createdAt,
            originalPrice = originalPrice,
            rating = rating,
        )
    }

    /** Reads an optional numeric field returned as a string, a number, or null. */
    private fun JsonElement?.asDecimalOrNull(): BigDecimal? {
        val text = this?.jsonPrimitive?.contentOrNull ?: return null
        if (text.isBlank() || text == "null") return null
        return text.toBigDecimalOrNull()
    }
}
