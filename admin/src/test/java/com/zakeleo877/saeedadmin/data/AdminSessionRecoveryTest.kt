package com.zakeleo877.saeedadmin.data

import com.zakeleo877.saeedcore.data.remote.AdminApi
import com.zakeleo877.saeedcore.data.remote.AdminSession
import com.zakeleo877.saeedcore.data.remote.AuthManager
import com.zakeleo877.saeedcore.data.remote.SessionStore
import com.zakeleo877.saeedcore.data.remote.SupabasePublicConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminSessionRecoveryTest {
    private val expired = AdminSession("admin-id", "test@example.invalid", "old-access", "old-refresh", 0)
    private val config = SupabasePublicConfig("https://project.example", "sb_publishable_test_key")
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private class MemoryStore(var stored: AdminSession?) : SessionStore {
        var clears = 0
        override fun load(): AdminSession? = stored
        override fun save(session: AdminSession) { stored = session }
        override fun clear() { clears++; stored = null }
    }

    private fun client(engine: MockEngine) = HttpClient(engine) {
        expectSuccess = false
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private fun repository(http: HttpClient, store: SessionStore): AdminRepository =
        AdminRepository(http, config, AdminApi(http, config), AuthManager(http, config), store)

    @Test
    fun `background restore refreshes the token before checking admin status`() = runTest {
        val store = MemoryStore(expired)
        val requests = mutableListOf<String>()
        val http = client(MockEngine { request ->
            requests += request.url.encodedPath
            if (request.url.encodedPath.contains("/auth/v1/token")) {
                respond("""{"access_token":"new-access","refresh_token":"new-refresh","expires_in":3600}""", HttpStatusCode.OK, jsonHeaders)
            } else {
                assertEquals("Bearer new-access", request.headers[HttpHeaders.Authorization])
                respond("true", HttpStatusCode.OK, jsonHeaders)
            }
        })
        try {
            val repo = repository(http, store)
            assertTrue(repo.restoreSession())
            assertEquals("new-access", repo.sessionHolder.session.value?.accessToken)
            assertEquals("new-refresh", store.stored?.refreshToken)
            assertEquals(0, store.clears)
            assertTrue(requests.first().contains("/auth/v1/token"))
        } finally { http.close() }
    }

    @Test
    fun `a temporary refresh outage does not erase the encrypted session`() = runTest {
        val store = MemoryStore(expired)
        val http = client(MockEngine { respond("{}", HttpStatusCode.ServiceUnavailable, jsonHeaders) })
        try {
            val repo = repository(http, store)
            assertFalse(repo.restoreSession())
            assertEquals(expired, store.stored)
            assertEquals(0, store.clears)
            assertNull(repo.sessionHolder.session.value)
        } finally { http.close() }
    }

    @Test
    fun `a failed authorization check preserves a newly rotated token`() = runTest {
        val store = MemoryStore(expired)
        val http = client(MockEngine { request ->
            if (request.url.encodedPath.contains("/auth/v1/token")) {
                respond("""{"access_token":"new-access","refresh_token":"new-refresh","expires_in":3600}""", HttpStatusCode.OK, jsonHeaders)
            } else {
                respond("{}", HttpStatusCode.ServiceUnavailable, jsonHeaders)
            }
        })
        try {
            val repo = repository(http, store)
            assertFalse(repo.restoreSession())
            assertEquals("new-refresh", store.stored?.refreshToken)
            assertEquals(0, store.clears)
            assertNull(repo.sessionHolder.session.value)
        } finally { http.close() }
    }

    @Test
    fun `a revoked refresh token is cleared and requires sign in`() = runTest {
        val store = MemoryStore(expired)
        val http = client(MockEngine { respond("{}", HttpStatusCode.Unauthorized, jsonHeaders) })
        try {
            val repo = repository(http, store)
            assertFalse(repo.restoreSession())
            assertNull(store.stored)
            assertEquals(1, store.clears)
        } finally { http.close() }
    }

    @Test
    fun `concurrent worker and foreground restoration rotate only once`() = runTest {
        val store = MemoryStore(expired)
        var refreshes = 0
        val http = client(MockEngine { request ->
            if (request.url.encodedPath.contains("/auth/v1/token")) {
                refreshes++
                respond("""{"access_token":"new-access","refresh_token":"new-refresh","expires_in":3600}""", HttpStatusCode.OK, jsonHeaders)
            } else {
                respond("true", HttpStatusCode.OK, jsonHeaders)
            }
        })
        try {
            val repo = repository(http, store)
            val first = async { repo.restoreSession() }
            val second = async { repo.restoreSession() }
            assertTrue(first.await())
            assertTrue(second.await())
            assertEquals(1, refreshes)
            assertNotNull(repo.sessionHolder.session.value)
        } finally { http.close() }
    }
}
