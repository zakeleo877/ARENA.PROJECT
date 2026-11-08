package com.zakeleo877.saeedcore

import com.zakeleo877.saeedcore.data.remote.AdminApi
import com.zakeleo877.saeedcore.data.remote.SupabasePublicConfig
import com.zakeleo877.saeedcore.model.OrderStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every admin request must carry the admin's own bearer token.
 *
 * The database decides with `is_admin()`, which reads `auth.uid()` from the
 * token. A request without one is anonymous, so every write was rejected with
 * 403: adding, editing and deleting products all failed silently, and orders
 * could not change status. An earlier fix covered two reads and the image
 * upload, but nothing asserted that the writes carried it, so the rest stayed
 * broken.
 *
 * These tests fail the moment a new admin call forgets the token.
 */
class AdminAuthHeaderTest {

    private val token = "admin-access-token"

    private fun apiWith(record: MutableList<Pair<HttpMethod, String?>>): AdminApi {
        val engine = MockEngine { request ->
            record += request.method to request.headers[HttpHeaders.Authorization]
            // A bare respondOk carries no Content-Type, and Ktor then refuses to
            // turn the body into a List: NoTransformationFoundException. The
            // header is part of a realistic answer, not decoration.
            respond(
                content = "[]",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return AdminApi(client, SupabasePublicConfig("https://project.example", "sb_publishable_test_key"))
    }

    private fun assertBearer(record: List<Pair<HttpMethod, String?>>, what: String) {
        assertTrue("$what must make a request", record.isNotEmpty())
        for ((method, header) in record) {
            assertEquals("$what ($method) must send the admin token", "Bearer $token", header)
        }
    }

    @Test
    fun `creating a product carries the token`() = runTest {
        val record = mutableListOf<Pair<HttpMethod, String?>>()
        apiWith(record).upsertProduct(
            token = token, id = null, name = "منتج", price = BigDecimal("100"),
            description = "", imageUrl = "", category = "عام", available = true, sortOrder = 0,
        )
        assertBearer(record, "creating a product")
    }

    @Test
    fun `editing a product carries the token`() = runTest {
        val record = mutableListOf<Pair<HttpMethod, String?>>()
        apiWith(record).upsertProduct(
            token = token, id = "p1", name = "منتج", price = BigDecimal("100"),
            description = "", imageUrl = "", category = "عام", available = true, sortOrder = 0,
        )
        assertBearer(record, "editing a product")
    }

    @Test
    fun `deleting a product carries the token`() = runTest {
        val record = mutableListOf<Pair<HttpMethod, String?>>()
        apiWith(record).deleteProduct(token, "p1")
        assertBearer(record, "deleting a product")
    }

    @Test
    fun `changing an order status carries the token`() = runTest {
        val record = mutableListOf<Pair<HttpMethod, String?>>()
        apiWith(record).updateOrderStatus(token, "o1", OrderStatus.HANDED_OVER)
        assertBearer(record, "changing an order status")
    }

    @Test
    fun `listing orders carries the token`() = runTest {
        val record = mutableListOf<Pair<HttpMethod, String?>>()
        apiWith(record).orders(token)
        assertBearer(record, "listing orders")
    }

    @Test
    fun `saving settings carries the token`() = runTest {
        val record = mutableListOf<Pair<HttpMethod, String?>>()
        apiWith(record).updateSettings(token, mapOf("storeName" to "متجري"))
        assertBearer(record, "saving settings")
    }

    @Test
    fun `uploading an image carries the token`() = runTest {
        val record = mutableListOf<Pair<HttpMethod, String?>>()
        val engine = MockEngine { request ->
            record += request.method to request.headers[HttpHeaders.Authorization]
            respond("{\"path\":\"a.webp\"}", headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        AdminApi(client, SupabasePublicConfig("https://project.example", "sb_publishable_test_key"))
            .uploadProductImage(token, "a.webp", ByteArray(4))
        assertBearer(record, "uploading an image")
    }
}
