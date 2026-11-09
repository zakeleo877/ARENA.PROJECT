package com.zakeleo877.saeedcore

import com.zakeleo877.saeedcore.data.remote.AdminApi
import com.zakeleo877.saeedcore.data.remote.SupabasePublicConfig
import com.zakeleo877.saeedcore.model.OrderStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/** Synthetic, in-memory responses shaped like PostgREST; never contacts the live shop. */
class AdminOrdersDecodingTest {
    private suspend fun decode(body: String): List<com.zakeleo877.saeedcore.model.Order> {
        val client = HttpClient(MockEngine {
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        return try {
            AdminApi(client, SupabasePublicConfig("https://project.example", "sb_publishable_test_key"))
                .orders("test-admin-token")
        } finally {
            client.close()
        }
    }

    @Test
    fun `reads numeric totals and line prices returned by PostgREST`() = runTest {
        val result = decode(
            """[{"id":"00000000-0000-4000-8000-000000000001","number":42,"customer":"عميل تجريبي","phone":"0000000000","notes":"","location":"","channel":"التطبيق","status":"جديد","details":[{"product_id":"10000000-0000-4000-8000-000000000001","name":"منتج اختباري","price":125.50,"qty":2}],"total":251.00,"client_id":"20000000-0000-4000-8000-000000000001","created_at":"2026-01-01T12:00:00+00:00"}]""",
        )
        assertEquals(1, result.size)
        assertEquals(42L, result.single().number)
        assertEquals(OrderStatus.NEW, result.single().status)
        assertEquals(0, result.single().total.compareTo(BigDecimal("251.00")))
        assertEquals(0, result.single().items.single().price.compareTo(BigDecimal("125.50")))
        assertEquals(2, result.single().items.single().quantity)
    }

    @Test
    fun `reads quoted prices and nullable optional fields without hiding the order`() = runTest {
        val result = decode(
            """[{"id":"00000000-0000-4000-8000-000000000002","number":43,"customer":"عميل تجريبي","phone":"0000000000","notes":null,"location":null,"channel":"التطبيق","status":"جديد","details":[{"product_id":null,"name":"منتج اختباري","price":"10.75","qty":1}],"total":"10.75","client_id":null,"created_at":"2026-01-01T12:00:00+00:00"}]""",
        )
        assertEquals(1, result.size)
        assertEquals("", result.single().notes)
        assertEquals("", result.single().customerLocationUrl)
        assertEquals("", result.single().items.single().productId)
        assertEquals(0, result.single().total.compareTo(BigDecimal("10.75")))
        assertEquals(0, result.single().items.single().price.compareTo(BigDecimal("10.75")))
    }
}
