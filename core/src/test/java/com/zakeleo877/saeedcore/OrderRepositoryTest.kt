package com.zakeleo877.saeedcore

import com.zakeleo877.saeedcore.data.OrderRepository
import com.zakeleo877.saeedcore.data.SubmitOutcome
import com.zakeleo877.saeedcore.data.local.PendingOrderDao
import com.zakeleo877.saeedcore.data.local.PendingOrderEntity
import com.zakeleo877.saeedcore.data.remote.CatalogApi
import com.zakeleo877.saeedcore.data.remote.SupabasePublicConfig
import com.zakeleo877.saeedcore.model.PendingOrder
import com.zakeleo877.saeedcore.model.PendingOrderItem
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderRepositoryTest {
    private val order = PendingOrder(
        clientId = "30000000-0000-4000-8000-000000000001",
        customer = "زبون تجريبي",
        phone = "0555000000",
        notes = "",
        customerLocationUrl = "",
        items = listOf(PendingOrderItem("10000000-0000-4000-8000-000000000001", 2)),
    )

    /** In-memory stand-in for the Room queue. */
    private class FakePendingDao : PendingOrderDao {
        val rows = linkedMapOf<String, PendingOrderEntity>()

        override suspend fun insert(order: PendingOrderEntity) {
            rows[order.clientId] = order
        }

        override fun observeAll(): Flow<List<PendingOrderEntity>> = flowOf(rows.values.toList())

        override suspend fun find(clientId: String): PendingOrderEntity? = rows[clientId]

        override suspend fun delete(clientId: String) {
            rows.remove(clientId)
        }
    }

    private fun apiReturning(status: HttpStatusCode, body: String): CatalogApi {
        val engine = MockEngine {
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return CatalogApi(
            HttpClient(engine) {
                expectSuccess = false
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            },
            SupabasePublicConfig("https://project.example", "sb_publishable_fake_for_test"),
        )
    }

    @Test fun offlineSubmissionQueuesTheOrderWithTheSameClientId() = runTest {
        val dao = FakePendingDao()
        val repository = OrderRepository(apiReturning(HttpStatusCode.OK, "7"), dao)

        val outcome = repository.submit(order, online = false)
        assertTrue(outcome is SubmitOutcome.Queued)
        assertEquals(order.clientId, (outcome as SubmitOutcome.Queued).clientId)
        assertEquals(1, dao.rows.size)
        assertEquals(order.clientId, dao.rows.values.first().clientId)
    }

    @Test fun aSuccessfulSubmissionReturnsTheServerNumberAndClearsTheQueue() = runTest {
        val dao = FakePendingDao()
        val repository = OrderRepository(apiReturning(HttpStatusCode.OK, "42"), dao)

        val outcome = repository.submit(order, online = true)
        assertEquals(SubmitOutcome.Sent(42), outcome)
        assertTrue(dao.rows.isEmpty())
    }

    @Test fun aServerErrorKeepsTheQueuedEntrySoItCanBeRetried() = runTest {
        val dao = FakePendingDao()
        val engine = MockEngine {
            respondError(HttpStatusCode.InternalServerError)
        }
        val repository = OrderRepository(
            CatalogApi(
                HttpClient(engine) {
                    expectSuccess = false
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                },
                SupabasePublicConfig("https://project.example", "sb_publishable_fake_for_test"),
            ),
            dao,
        )

        val outcome = repository.submit(order, online = true)
        assertTrue(outcome is SubmitOutcome.Rejected)
        assertEquals(1, dao.rows.size)
        assertEquals(order.clientId, dao.rows.values.first().clientId)
    }

    @Test fun theQueueEntryKeepsTheItemSnapshotSoARetryIsIdentical() = runTest {
        val dao = FakePendingDao()
        val repository = OrderRepository(apiReturning(HttpStatusCode.OK, "7"), dao)

        repository.queue(order)
        val stored = dao.rows.values.first()
        assertTrue(stored.itemsJson.contains(order.items.first().productId))
        assertTrue(stored.itemsJson.contains("\"quantity\":2"))
    }

    // ---------------------------------------------------------- offline flow ---

    /** An engine that drops the connection the way a real network failure does. */
    private fun apiLosingTheConnection(): CatalogApi {
        val engine = MockEngine {
            throw java.io.IOException("Simulated connection loss")
        }
        return CatalogApi(
            HttpClient(engine) {
                expectSuccess = false
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            },
            SupabasePublicConfig("https://project.example", "sb_publishable_fake_for_test"),
        )
    }

    @Test fun aConnectionLostDuringTheSendKeepsTheOrderInTheQueue() = runTest {
        val dao = FakePendingDao()
        val repository = OrderRepository(apiLosingTheConnection(), dao)

        val outcome = repository.submit(order, online = true)

        assertTrue(outcome is SubmitOutcome.Rejected)
        assertEquals("network_error", (outcome as SubmitOutcome.Rejected).reason)
        // The order must survive the failure, otherwise the customer loses it.
        assertEquals(1, dao.rows.size)
        val stored = dao.rows.values.first()
        assertEquals(order.clientId, stored.clientId)
        assertEquals("FAILED", stored.state)
        assertEquals(1, stored.attemptCount)
        assertEquals("network_error", stored.lastErrorCode)
    }

    @Test fun aRetryAfterTheConnectionReturnsSendsTheOrderAndClearsTheQueue() = runTest {
        val dao = FakePendingDao()
        val failing = OrderRepository(apiLosingTheConnection(), dao)
        failing.submit(order, online = true)
        assertEquals(1, dao.rows.size)

        // The network comes back and the queued order is sent again.
        val working = OrderRepository(apiReturning(HttpStatusCode.OK, "99"), dao)
        val outcome = working.retry(order.clientId)

        assertEquals(SubmitOutcome.Sent(99), outcome)
        assertTrue(dao.rows.isEmpty())
    }

    @Test fun aRetryReusesTheSameClientIdSoTheServerCannotDuplicateTheOrder() = runTest {
        val dao = FakePendingDao()
        val failing = OrderRepository(apiLosingTheConnection(), dao)
        failing.submit(order, online = true)

        val seen = mutableListOf<String>()
        val engine = MockEngine { request ->
            seen += (request.body as io.ktor.http.content.TextContent).text
            respond("99", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val working = OrderRepository(
            CatalogApi(
                HttpClient(engine) {
                    expectSuccess = false
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                },
                SupabasePublicConfig("https://project.example", "sb_publishable_fake_for_test"),
            ),
            dao,
        )
        working.retry(order.clientId)

        // The request body still carries the original client id.
        assertTrue(seen.isNotEmpty())
        assertTrue(seen.any { it.contains(order.clientId) })
    }

    @Test fun aPermanentRejectionRemovesTheOrderInsteadOfLoopingForever() = runTest {
        val dao = FakePendingDao()
        val engine = MockEngine {
            respond(
                """{"code":"P0001","message":"invalid_phone"}""",
                HttpStatusCode.BadRequest,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = OrderRepository(
            CatalogApi(
                HttpClient(engine) {
                    expectSuccess = false
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                },
                SupabasePublicConfig("https://project.example", "sb_publishable_fake_for_test"),
            ),
            dao,
        )

        val outcome = repository.submit(order, online = true)

        assertTrue(outcome is SubmitOutcome.Rejected)
        assertEquals("invalid_order", (outcome as SubmitOutcome.Rejected).reason)
        // A permanently invalid order must not stay in the queue.
        assertTrue(dao.rows.isEmpty())
    }

    @Test fun aRateLimitKeepsTheOrderSoItCanBeSentLater() = runTest {
        val dao = FakePendingDao()
        val engine = MockEngine {
            respond(
                """{"code":"P0001","message":"rate_limited"}""",
                HttpStatusCode.BadRequest,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repository = OrderRepository(
            CatalogApi(
                HttpClient(engine) {
                    expectSuccess = false
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                },
                SupabasePublicConfig("https://project.example", "sb_publishable_fake_for_test"),
            ),
            dao,
        )

        val outcome = repository.submit(order, online = true)

        assertTrue(outcome is SubmitOutcome.Rejected)
        assertEquals(1, dao.rows.size)
    }

    @Test fun anOrderQueuedWhileOfflineIsSentWithTheSameClientId() = runTest {
        val dao = FakePendingDao()
        val offline = OrderRepository(apiReturning(HttpStatusCode.OK, "7"), dao)
        offline.submit(order, online = false)
        assertEquals(1, dao.rows.size)

        val online = OrderRepository(apiReturning(HttpStatusCode.OK, "7"), dao)
        val outcome = online.retry(order.clientId)

        assertEquals(SubmitOutcome.Sent(7), outcome)
        assertTrue(dao.rows.isEmpty())
    }

    // ------------------------------------------------- automatic send on return ---

    @Test fun aWaitingOrderIsSentWhenTheConnectionReturns() = runTest {
        val dao = FakePendingDao()
        val repository = OrderRepository(apiReturning(HttpStatusCode.OK, "77"), dao)
        // The customer tapped send while the phone was offline.
        repository.submit(order, online = false)
        assertEquals("WAITING", dao.rows.values.single().state)

        // The connection comes back; nothing else is tapped.
        val outcomes = repository.drainWaiting()

        assertEquals(listOf(SubmitOutcome.Sent(77)), outcomes)
        assertTrue("a sent order leaves the queue", dao.rows.isEmpty())
    }

    @Test fun aFailedOrderIsNotResentAutomatically() = runTest {
        val dao = FakePendingDao()
        OrderRepository(apiLosingTheConnection(), dao).submit(order, online = true)
        assertEquals("FAILED", dao.rows.values.single().state)

        // A failure may be a rejection the customer has to fix. Sending it again
        // every time the signal flickers would hide the reason behind a retry.
        val outcomes = OrderRepository(apiReturning(HttpStatusCode.OK, "5"), dao).drainWaiting()

        assertTrue("a failed order is the customer's decision to resend", outcomes.isEmpty())
        assertEquals(1, dao.rows.size)
    }

    @Test fun anEmptyQueueSendsNothing() = runTest {
        val dao = FakePendingDao()
        val outcomes = OrderRepository(apiReturning(HttpStatusCode.OK, "1"), dao).drainWaiting()
        assertTrue(outcomes.isEmpty())
    }
}
