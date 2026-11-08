package com.zakeleo877.saeedcore

import com.zakeleo877.saeedcore.data.remote.AdminApi
import com.zakeleo877.saeedcore.data.remote.SupabasePublicConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A product write is only a success when the row is really there.
 *
 * PostgREST answers 201 with no body when it writes, and 200 with no body when
 * the filter matched nothing. Treating both as success is what makes "the
 * product I added does not appear" possible: the app said it saved, and the
 * table never changed.
 */
class ProductWriteVerificationTest {

    private val config = SupabasePublicConfig("https://project.example", "sb_publishable_test_key")

    private fun api(writeStatus: HttpStatusCode, readBody: String): AdminApi {
        val engine = MockEngine { request ->
            if (request.method == HttpMethod.Get) {
                respond(readBody, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respond("", writeStatus, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        return AdminApi(client, config)
    }

    private suspend fun create(api: AdminApi, price: String = "100") = api.upsertProduct(
        token = "t", id = null, name = "منتج", price = BigDecimal(price),
        description = "", imageUrl = "", category = "عام", available = true, sortOrder = 0,
    )

    @Test
    fun `a write that changed nothing is reported as a failure`() = runTest {
        val api = api(HttpStatusCode.Created, "[]")
        assertTrue("the save must not be reported as a success", !create(api))
        assertNotNull("the owner must be told why", api.lastFailure)
    }

    @Test
    fun `a write that landed is reported as a success`() = runTest {
        val api = api(HttpStatusCode.Created, """[{"id":"p1","name":"منتج","price":100.00}]""")
        assertTrue("the row is in the table", create(api))
        assertNull(api.lastFailure)
    }

    @Test
    fun `a write answered with a body is not read back`() = runTest {
        // A body means the caller asked for a representation, so the answer is
        // already proof; no extra round trip is spent on it.
        var reads = 0
        val engine = MockEngine { request ->
            if (request.method == HttpMethod.Get) {
                reads++
                respond("[]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respond("""[{"id":"p1"}]""", HttpStatusCode.Created, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        assertTrue(create(AdminApi(client, config)))
        assertEquals("no verification round trip", 0, reads)
    }

    @Test
    fun `a read that fails does not turn a save into a loss`() = runTest {
        // The write may have landed; a read that failed is not proof it did not.
        val engine = MockEngine { request ->
            if (request.method == HttpMethod.Get) {
                respond("{}", HttpStatusCode.ServiceUnavailable, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respond("", HttpStatusCode.Created, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        assertTrue(create(AdminApi(client, config)))
    }
}
