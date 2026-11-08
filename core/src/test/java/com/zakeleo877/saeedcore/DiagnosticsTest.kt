package com.zakeleo877.saeedcore

import com.zakeleo877.saeedcore.data.remote.AdminApi
import com.zakeleo877.saeedcore.data.remote.SupabasePublicConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The four ways "nothing appears" all look the same to the owner, so the
 * connection check has to tell them apart. Each test reproduces one of them.
 */
class DiagnosticsTest {

    private val config = SupabasePublicConfig("https://project.example", "sb_publishable_key")

    /** Answers by path, so a test states only the links it breaks. */
    private fun api(
        health: HttpStatusCode = HttpStatusCode.OK,
        keyedProducts: HttpStatusCode = HttpStatusCode.OK,
        admin: Boolean = true,
    ): AdminApi {
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.startsWith("/auth/v1/health") ->
                    respond("{}", health, headersOf(HttpHeaders.ContentType, "application/json"))
                path.startsWith("/rest/v1/rpc/") ->
                    respond(
                        if (admin) "true" else "false",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                // A request with a user token reads the table; one with only the
                // key is the "is this key accepted" probe.
                request.headers[HttpHeaders.Authorization] != null ->
                    respond("[]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                else ->
                    respond("[]", keyedProducts, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        return AdminApi(client, config)
    }

    @Test
    fun `a healthy project passes every link`() = runTest {
        val checks = api().diagnose("t")
        assertEquals(4, checks.size)
        assertTrue("every link must pass: $checks", checks.all { it.passed })
    }

    @Test
    fun `a key from another project is named as such`() = runTest {
        val checks = api(keyedProducts = HttpStatusCode.Unauthorized).diagnose("t")
        val key = checks.single { it.label == "المفتاح مقبول" }
        assertFalse(key.passed)
        assertTrue("the reason must mention the key: ${key.detail}", key.detail.contains("401"))
    }

    @Test
    fun `an unapplied schema is reported as a missing table`() = runTest {
        val checks = api(keyedProducts = HttpStatusCode.NotFound).diagnose("t")
        val key = checks.single { it.label == "المفتاح مقبول" }
        assertFalse(key.passed)
        assertTrue("the schema must be blamed: ${key.detail}", key.detail.contains("schema.sql"))
    }

    @Test
    fun `an account that is not an admin is reported`() = runTest {
        val checks = api(admin = false).diagnose("t")
        val admin = checks.single { it.label == "الحساب مدير" }
        assertFalse(admin.passed)
        assertTrue(admin.detail.contains("مدير"))
    }

    @Test
    fun `a project that does not answer is not hidden behind other checks`() = runTest {
        val checks = api(health = HttpStatusCode.ServiceUnavailable).diagnose("t")
        val reachable = checks.single { it.label == "المشروع يستجيب" }
        assertFalse(reachable.passed)
        assertTrue(reachable.detail.contains("503"))
    }

    @Test
    fun `a dead connection answers zero instead of throwing`() = runTest {
        val engine = MockEngine { throw IllegalStateException("no route to host") }
        val client = HttpClient(engine) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        val checks = AdminApi(client, config).diagnose("t")
        assertEquals(4, checks.size)
        assertFalse("nothing can pass without a connection", checks.all { it.passed })
        assertTrue(checks.first().detail.contains("HTTP 0"))
    }
}
