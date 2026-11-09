package com.zakeleo877.saeedcore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zakeleo877.saeedcore.data.CartPersistence
import com.zakeleo877.saeedcore.data.CartStore
import com.zakeleo877.saeedcore.model.Product
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The basket has to survive the app being closed.
 *
 * The store was documented as persisted and nothing wrote it, so every restart
 * emptied the basket. These tests use the real file rather than a mock: a bug in
 * the serialisation is exactly the kind a mock hides.
 */
class CartPersistenceTest {

    private val directory: File = Files.createTempDirectory("cart").toFile()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun tearDown() {
        scope.cancel()
        directory.deleteRecursively()
    }

    private fun dataStore(name: String = "cart.preferences_pb") =
        PreferenceDataStoreFactory.create(scope = scope) { File(directory, name) }

    private fun product(id: String, price: String = "1200.50") = Product(
        id = id,
        name = "منتج $id",
        price = BigDecimal(price),
        description = "",
        imageUrl = "https://example.invalid/$id.webp",
        category = "حلويات",
        available = true,
        sortOrder = 1,
        createdAt = Instant.ofEpochSecond(1_700_000_000),
    )

    /** Waits for the mirroring collector to see the change, instead of guessing a yield count. */
    private suspend fun awaitWritten(expected: Int, store: () -> CartPersistence) {
        withTimeout(5_000) {
            while (store().load().size != expected) {
                yield()
            }
        }
    }

    @Test
    fun `a basket written by one process is read by the next`() = runTest {
        val store = CartStore()
        store.add(product("a"), 2)
        store.add(product("b"), 1)
        val persistence = CartPersistence(dataStore(), scope)
        persistence.bind(store)
        awaitWritten(2) { CartPersistence(dataStore(), scope) }

        // A second instance over the same file stands in for the next launch.
        val restarted = CartPersistence(dataStore(), scope).load()
        assertEquals(2, restarted.size)
        assertEquals(2, restarted.first { it.product.id == "a" }.quantity)
        assertEquals(
            "the price the customer saw is the price remembered",
            BigDecimal("1200.50"),
            restarted.first { it.product.id == "a" }.product.price,
        )
    }

    @Test
    fun `an empty file is an empty basket, not a crash`() = runTest {
        assertTrue(CartPersistence(dataStore("fresh.preferences_pb"), scope).load().isEmpty())
    }

    @Test
    fun `a corrupt row is dropped and the rest of the basket survives`() = runTest {
        val ds = dataStore("corrupt.preferences_pb")
        ds.edit { prefs ->
            prefs[stringPreferencesKey("cart_v1")] =
                """[{"id":"a","name":"جيد","price":"100","quantity":3},""" +
                    """{"id":"b","name":"تالف","price":"ليس رقماً","quantity":1}]"""
        }
        val items = CartPersistence(ds, scope).load()
        assertEquals("only the usable row is kept", 1, items.size)
        assertEquals("a", items.single().product.id)
    }

    @Test
    fun `quantities outside the allowed range are clamped, not rejected`() = runTest {
        val ds = dataStore("clamp.preferences_pb")
        ds.edit { prefs ->
            prefs[stringPreferencesKey("cart_v1")] =
                """[{"id":"a","name":"كثير","price":"10","quantity":900}]"""
        }
        assertEquals(99, CartPersistence(ds, scope).load().single().quantity)
    }

    @Test
    fun `the mirror follows later edits`() = runTest {
        val store = CartStore()
        val persistence = CartPersistence(dataStore(), scope)
        persistence.bind(store)
        store.add(product("a"), 1)
        awaitWritten(1) { CartPersistence(dataStore(), scope) }

        store.setQuantity("a", 5)
        withTimeout(5_000) {
            while (CartPersistence(dataStore(), scope).load().firstOrNull()?.quantity != 5) yield()
        }

        store.remove("a")
        awaitWritten(0) { CartPersistence(dataStore(), scope) }
        assertTrue(store.items.first().isEmpty())
    }
}
