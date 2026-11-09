package com.zakeleo877.saeedcore

import com.zakeleo877.saeedcore.data.OrderRepository
import com.zakeleo877.saeedcore.data.SubmitOutcome
import com.zakeleo877.saeedcore.data.local.PendingOrderDao
import com.zakeleo877.saeedcore.data.local.PendingOrderEntity
import com.zakeleo877.saeedcore.data.remote.AuthError
import com.zakeleo877.saeedcore.data.remote.AdminApi
import com.zakeleo877.saeedcore.data.remote.AuthManager
import com.zakeleo877.saeedcore.data.remote.AuthResult
import com.zakeleo877.saeedcore.data.remote.CatalogApi
import com.zakeleo877.saeedcore.data.remote.SupabasePublicConfig
import com.zakeleo877.saeedcore.data.remote.anonymousHttpClient
import com.zakeleo877.saeedcore.model.PendingOrder
import com.zakeleo877.saeedcore.model.PendingOrderItem
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.DefaultJson
import io.ktor.serialization.kotlinx.json.json
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * The application code driven against a real backend.
 *
 * The backend is the isolated Supabase-compatible environment that
 * `supabase/tests` provides. It is a private PostgreSQL instance created from
 * `supabase/schema.sql`, so the row level security policies, the grants and
 * the `place_order` function under test are the real ones. The environment
 * listens on the loopback interface only and uses dummy data.
 *
 * The test is skipped when the backend is not running, so the ordinary unit
 * test task keeps working on a machine that does not start it.
 */
class IsolatedBackendTest {

    private var backendUrl: String? = null
    private var publicKey: String? = null

    @Before
    fun readBackend() {
        backendUrl = System.getenv("SAEED_TEST_BACKEND_URL")
        publicKey = System.getenv("SAEED_TEST_BACKEND_KEY")
        assumeTrue(
            "The isolated backend is not running, so this test is skipped.",
            !backendUrl.isNullOrBlank() && !publicKey.isNullOrBlank(),
        )
    }

    /**
     * A transport that forwards every request to the isolated backend over a
     * real socket. The client code under test is untouched; only the engine
     * is replaced.
     */
    private fun engine(): MockEngine = MockEngine { request ->
        val target = URL(
            "${backendUrl!!}${request.url.encodedPath}" +
                if (request.url.encodedQuery.isNotEmpty()) "?${request.url.encodedQuery}" else "",
        )
        val connection = target.openConnection() as HttpURLConnection
        connection.requestMethod = request.method.value
        connection.connectTimeout = 10_000
        connection.readTimeout = 20_000
        for (name in request.headers.names()) {
            val value = request.headers[name]
            if (value != null && !name.equals(HttpHeaders.Host, ignoreCase = true)) {
                connection.setRequestProperty(name, value)
            }
        }
        val body = when (val content = request.body) {
            is io.ktor.http.content.TextContent -> content.text.toByteArray()
            is io.ktor.http.content.ByteArrayContent -> content.bytes()
            else -> ByteArray(0)
        }
        if (body.isNotEmpty()) {
            connection.doOutput = true
            connection.outputStream.use { it.write(body) }
        }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val bytes = stream?.readBytes() ?: ByteArray(0)
        connection.disconnect()
        respond(
            content = bytes,
            status = HttpStatusCode.fromValue(status),
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }

    /**
     * The same client factory the application uses, so the JSON
     * transformation and the timeouts are the production ones.
     */
    private fun client(): HttpClient = anonymousHttpClient(engine())

    private fun config(): SupabasePublicConfig =
        // The application always talks to an HTTPS project. The transport is
        // what reaches the loopback backend, so the URL stays secure.
        SupabasePublicConfig("https://isolated.test", publicKey!!)

    private class FakePendingDao : PendingOrderDao {
        val rows = linkedMapOf<String, PendingOrderEntity>()
        override suspend fun insert(order: PendingOrderEntity) { rows[order.clientId] = order }
        override fun observeAll(): Flow<List<PendingOrderEntity>> = flowOf(rows.values.toList())
        override suspend fun find(clientId: String): PendingOrderEntity? = rows[clientId]
        override suspend fun update(order: PendingOrderEntity) { if (rows.containsKey(order.clientId)) rows[order.clientId] = order }
        override suspend fun delete(clientId: String) { rows.remove(clientId) }
    }

    // ---------------------------------------------------------- catalogue ---

    @Test
    fun theCustomerReadsTheCatalogueThroughTheRealClient() = runTest {
        val httpClient = client()
        try {
            val api = CatalogApi(httpClient, config())
            val products = api.products()
            // The backend seeds two available products and one hidden product.
            assertTrue("the catalogue must not be empty", products.isNotEmpty())
            assertTrue("a hidden product must never reach the customer", products.none { !it.available })
            assertTrue("the catalogue must carry the categories", products.all { it.category.isNotBlank() })
        } finally {
            httpClient.close()
        }
    }

    @Test
    fun theCustomerReadsTheStoreSettingsThroughTheRealClient() = runTest {
        val httpClient = client()
        try {
            val settings = CatalogApi(httpClient, config()).settings()
            assertEquals("محلات السعيد", settings.storeName)
            assertTrue(settings.whatsapp.isNotBlank())
            assertTrue(settings.currency.isNotBlank())
        } finally {
            httpClient.close()
        }
    }

    // --------------------------------------------------------------- cart ---

    @Test
    fun theCustomerPlacesAnOrderAndTheServerReturnsTheNumber() = runTest {
        val httpClient = client()
        try {
            val api = CatalogApi(httpClient, config())
            val product = api.products().first()
            val order = PendingOrder(
                clientId = UUID.randomUUID().toString(),
                customer = "زبون اختبار",
                phone = "0550123456",
                notes = "ملاحظة",
                customerLocationUrl = "https://maps.example.invalid/?q=1",
                items = listOf(PendingOrderItem(product.id, 2)),
            )
            val number = api.placeOrder(order)
            assertTrue("the server must return a positive order number", number > 0)
        } finally {
            httpClient.close()
        }
    }

    @Test
    fun anInvalidOrderIsRejectedByTheServer() = runTest {
        val httpClient = client()
        try {
            val api = CatalogApi(httpClient, config())
            val product = api.products().first()
            val order = PendingOrder(
                clientId = UUID.randomUUID().toString(),
                customer = "x",
                phone = "0550123456",
                notes = "",
                customerLocationUrl = "",
                items = listOf(PendingOrderItem(product.id, 1)),
            )
            // The client validates first, so the request never leaves the device.
            val failure = runCatching { api.placeOrder(order) }.exceptionOrNull()
            assertNotNull("an invalid order must be refused", failure)
        } finally {
            httpClient.close()
        }
    }

    @Test
    fun anOrderForAnUnavailableProductIsRejectedByTheServer() = runTest {
        val httpClient = client()
        try {
            val api = CatalogApi(httpClient, config())
            // The hidden product exists in the database but is not available.
            val order = PendingOrder(
                clientId = UUID.randomUUID().toString(),
                customer = "زبون اختبار",
                phone = "0550123456",
                notes = "",
                customerLocationUrl = "",
                items = listOf(PendingOrderItem("30000000-0000-4000-8000-000000000003", 1)),
            )
            val failure = runCatching { api.placeOrder(order) }.exceptionOrNull()
            assertNotNull("an unavailable product must be refused", failure)
        } finally {
            httpClient.close()
        }
    }

    // -------------------------------------------------------------- queue ---

    @Test
    fun anOrderThatCannotBeSentIsKeptAndSentOnTheNextAttempt() = runTest {
        val dao = FakePendingDao()
        val httpClient = client()
        try {
            val api = CatalogApi(httpClient, config())
            val product = api.products().first()
            val order = PendingOrder(
                clientId = UUID.randomUUID().toString(),
                customer = "زبون اختبار",
                phone = "0550123456",
                notes = "",
                customerLocationUrl = "",
                items = listOf(PendingOrderItem(product.id, 1)),
            )
            val repository = OrderRepository(api, dao)

            // The device is offline, so the order is queued.
            val queued = repository.submit(order, online = false)
            assertTrue(queued is SubmitOutcome.Queued)
            assertEquals(1, dao.rows.size)

            // The network comes back and the queued order is sent.
            val sent = repository.retry(order.clientId)
            assertTrue("the queued order must be sent: $sent", sent is SubmitOutcome.Sent)
            assertTrue(dao.rows.isEmpty())
        } finally {
            httpClient.close()
        }
    }

    // --------------------------------------------------------------- auth ---

    @Test
    fun theAdminSignsInWithTheRightCredentials() = runTest {
        val httpClient = client()
        try {
            val manager = AuthManager(httpClient, config())
            val result = manager.signIn("owner@example.invalid", "OwnerPass123")
            assertTrue("the sign-in must succeed: $result", result is AuthResult.Success)
            val session = (result as AuthResult.Success).session
            assertEquals("owner@example.invalid", session.email)
            assertTrue(session.accessToken.isNotBlank())
            assertTrue(session.refreshToken.isNotBlank())
            assertTrue(session.expiresAtMillis > 0L)
        } finally {
            httpClient.close()
        }
    }

    @Test
    fun theAdminIsRefusedWithAWrongPassword() = runTest {
        val httpClient = client()
        try {
            val manager = AuthManager(httpClient, config())
            val result = manager.signIn("owner@example.invalid", "WrongPassword123")
            assertTrue(result is AuthResult.Failure)
            assertEquals(AuthError.INVALID_CREDENTIALS, (result as AuthResult.Failure).reason)
        } finally {
            httpClient.close()
        }
    }

    @Test
    fun anEmptyEmailIsRefusedBeforeAnyRequest() = runTest {
        val httpClient = client()
        try {
            val manager = AuthManager(httpClient, config())
            val result = manager.signIn("", "Password123")
            assertTrue(result is AuthResult.Failure)
            assertEquals(AuthError.INVALID_CREDENTIALS, (result as AuthResult.Failure).reason)
        } finally {
            httpClient.close()
        }
    }

    /**
     * The order list is bounded, and the bound reaches the server.
     *
     * Without a bound the management screen asked for the whole table, so the
     * response grew with the age of the shop: 73000 rows after a year at 200
     * orders a day. This drives the real AdminApi and reads the query string
     * that it produced, so a regression that drops the bound is caught.
     */
    @Test
    fun theOrderListAsksTheServerForABoundedPage() = runTest {
        var requested: String? = null
        val engine = MockEngine { request ->
            requested = request.url.toString()
            respond(
                content = "[]",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) {
                json(DefaultJson)
            }
        }
        try {
            val admin = AdminApi(httpClient, config())
            val orders = admin.orders("test-token", limit = 25)
            assertTrue("an empty answer is fine", orders.isEmpty())
            assertTrue("a request must have been made", requested != null)
            val url = requested!!
            assertTrue("the request must carry a limit, was: $url", url.contains("limit=25"))
            assertTrue("the request must not ask for the whole table", !url.contains("limit=1000000"))
        } finally {
            httpClient.close()
        }
    }

    /**
     * A caller cannot raise the bound past the ceiling, which keeps one client
     * from pulling an unbounded page even if a future screen asks for it.
     */
    @Test
    fun theOrderPageCeilingHolds() = runTest {
        var requested: String? = null
        val engine = MockEngine { request ->
            requested = request.url.toString()
            respond(
                content = "[]",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) {
                json(DefaultJson)
            }
        }
        try {
            AdminApi(httpClient, config()).orders("test-token", limit = 1_000_000)
            val url = requested!!
            assertTrue("the limit must be capped, was: $url", !url.contains("1000000"))
            assertTrue("the cap must still be a real page", Regex("limit=(\\d+)").find(url)?.groupValues?.get(1)?.toInt()?.let { it <= 500 } == true)
        } finally {
            httpClient.close()
        }
    }
}
