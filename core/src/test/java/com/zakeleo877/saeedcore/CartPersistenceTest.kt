package com.zakeleo877.saeedcore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zakeleo877.saeedcore.data.CartPersistence
import com.zakeleo877.saeedcore.data.CartStore
import com.zakeleo877.saeedcore.model.CartItem
import com.zakeleo877.saeedcore.model.Product
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests the actual asynchronous mirror and a real preferences file. */
class CartPersistenceTest {
    private val directory: File = Files.createTempDirectory("cart").toFile()
    private val scopes = mutableListOf<CoroutineScope>()
    private val key = stringPreferencesKey("cart_v1")

    @After
    fun tearDown() = runBlocking {
        scopes.forEach { it.coroutineContext[Job]?.cancelAndJoin() }
        directory.deleteRecursively()
        Unit
    }

    private fun freshStore(name: String = "cart.preferences_pb"): Pair<DataStore<Preferences>, CoroutineScope> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes += scope
        return PreferenceDataStoreFactory.create(scope = scope) { File(directory, name) } to scope
    }

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

    /** Observe the actual DataStore update, not a sleep or its initial empty write. */
    private suspend fun awaitStored(ds: DataStore<Preferences>, matches: (String) -> Boolean): String =
        withTimeout(10_000) {
            ds.data.first { prefs -> prefs[key]?.let(matches) == true }[key]!!
        }

    @Test
    fun `the mirror writes changes and clears the basket`() = runBlocking {
        val (ds, scope) = freshStore()
        val store = CartStore()
        val persistence = CartPersistence(ds, scope)
        persistence.bind(store)

        // A quantity of one is a JSON default and need not be printed explicitly.
        store.add(product("a"), 2)
        val initial = awaitStored(ds) { it.contains("\"quantity\":2") }
        assertTrue(initial.contains("\"id\":\"a\""))
        assertTrue(initial.contains("1200.50"))

        store.setQuantity("a", 5)
        awaitStored(ds) { it.contains("\"quantity\":5") }
        assertEquals(5, persistence.load().single().quantity)

        store.remove("a")
        assertEquals("[]", awaitStored(ds) { it == "[]" })
        assertTrue(persistence.load().isEmpty())
    }

    @Test
    fun `a basket written by one process is read by the next`() = runBlocking {
        val (first, firstScope) = freshStore()
        val store = CartStore()
        store.add(product("a"), 3)
        store.add(product("b"), 1)
        CartPersistence(first, firstScope).bind(store)
        awaitStored(first) { it.contains("\"id\":\"b\"") }

        // DataStore requires one live instance per file. Wait for the first
        // scope and all its children to stop instead of guessing with a sleep.
        firstScope.coroutineContext[Job]!!.cancelAndJoin()
        val (second, secondScope) = freshStore()
        val restarted = CartPersistence(second, secondScope).load()
        assertEquals(2, restarted.size)
        assertEquals(3, restarted.first { it.product.id == "a" }.quantity)
        assertEquals(1, restarted.first { it.product.id == "b" }.quantity)
        assertEquals(BigDecimal("1200.50"), restarted.first { it.product.id == "a" }.product.price)
    }

    @Test
    fun `an empty file is an empty basket`() = runBlocking {
        val (ds, scope) = freshStore()
        assertTrue(CartPersistence(ds, scope).load().isEmpty())
    }

    @Test
    fun `invalid rows are skipped without losing valid rows`() = runBlocking {
        val (ds, scope) = freshStore()
        ds.edit { prefs ->
            prefs[key] = """[{"id":"a","name":"جيد","price":"100","quantity":3},""" +
                """{"id":"b","name":"تالف","price":"ليس رقماً"},""" +
                """{"id":"c","name":"ناقص"},""" +
                """{"id":"d","name":"سعر سالب","price":"-1"}]"""
        }
        val items = CartPersistence(ds, scope).load()
        assertEquals(1, items.size)
        assertEquals("a", items.single().product.id)
    }

    @Test
    fun `quantities outside the allowed range are clamped`() = runBlocking {
        val (ds, scope) = freshStore()
        ds.edit { prefs ->
            prefs[key] = """[{"id":"a","name":"كثير","price":"10","quantity":900}]"""
        }
        assertEquals(99, CartPersistence(ds, scope).load().single().quantity)
    }

    @Test
    fun `edits made during restoration are replayed on the saved basket`() {
        val cart = CartStore(awaitingRestore = true)
        cart.add(product("a"), 3)
        cart.add(product("b"))
        cart.restoreAll(listOf(CartItem(product("a"), 2)))

        assertEquals(5, cart.snapshot().first { it.product.id == "a" }.quantity)
        assertEquals(1, cart.snapshot().first { it.product.id == "b" }.quantity)
    }

    @Test
    fun `clearing during restoration does not resurrect saved items`() {
        val cart = CartStore(awaitingRestore = true)
        cart.clear()
        cart.add(product("b"))
        cart.restoreAll(listOf(CartItem(product("a"), 2)))
        assertEquals(listOf("b"), cart.snapshot().map { it.product.id })
    }

    @Test
    fun `checkout waits for restoration`() = runBlocking {
        val cart = CartStore(awaitingRestore = true)
        val checkout = async(start = CoroutineStart.UNDISPATCHED) {
            cart.awaitRestored()
            cart.snapshot()
        }
        assertFalse(checkout.isCompleted)
        cart.restoreAll(listOf(CartItem(product("a"), 2)))
        assertEquals(2, checkout.await().single().quantity)
    }
}
