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
        location = "",
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
}
