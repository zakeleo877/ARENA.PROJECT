package com.zakeleo877.saeedcore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The basket has to survive the app being closed.
 *
 * The store was documented as persisted and nothing wrote it, so every restart
 * emptied the basket. These tests use a real file rather than a mock: the
 * serialisation is exactly the part a mock would hide.
 *
 * One rule governs the whole file: **only one DataStore may be alive for a given
 * file at a time**. DataStore enforces this, and two live instances deadlock
 * rather than fail loudly, which is what made the first version of these tests
 * time out. A "restart" is therefore modelled by stopping the first instance
 * before the second is created.
 */
class CartPersistenceTest {

    private val directory: File = Files.createTempDirectory("cart").toFile()
    private val scopes = mutableListOf<CoroutineScope>()

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
        directory.deleteRecursively()
    }

    /** A DataStore with its own scope, so it can be stopped before the next one starts. */
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

    /**
     * Reads the raw bytes that were written.
     *
     * Asserting on the stored JSON, rather than on a second in-memory object,
     * is what makes this a test of persistence and not of caching.
     */
    /**
     * Waits until the stored JSON satisfies [condition], and returns it.
     *
     * Waiting for "a value exists" is not enough: the collector's first write is
     * the empty basket, so the check would return before the change under test
     * had been written - which is exactly how this test failed once.
     */
    private fun awaitStored(
        ds: DataStore<Preferences>,
        condition: (String) -> Boolean = { true },
    ): String = runBlocking(Dispatchers.IO) {
        withTimeout(10_000) {
            var raw: String
            do {
                raw = ds.data.first()[stringPreferencesKey("cart_v1")].orEmpty()
                if (!condition(raw)) delay(20)
            } while (!condition(raw))
            raw
        }
    }

    @Test
    fun `the basket reaches the disk and can be read back`() {
        val (ds, _) = freshStore()
        val store = CartStore()
        store.add(product("a"), 2)
        store.add(product("b"), 1)
        CartPersistence(ds, scopes.last()).bind(store)

        val written = awaitStored(ds) { it.contains("\"id\":\"b\"") }
        assertTrue("the file must carry both lines", written.contains("\"id\":\"a\""))
        assertTrue(written.contains("\"id\":\"b\""))
        assertTrue("the quantity is stored, not just the id", written.contains("\"quantity\":2"))
        assertTrue(
            "the price the customer saw is remembered, not looked up again",
            written.contains("1200.50"),
        )
    }

    @Test
    fun `a basket written by one process is read by the next`() {
        val (first, firstScope) = freshStore()
        val store = CartStore()
        store.add(product("a"), 3)
        CartPersistence(first, firstScope).bind(store)
        awaitStored(first) { it.contains("\"id\":\"a\"") }
        // The process ends: its scope, and with it the DataStore, is stopped
        // before the next launch opens the same file.
        firstScope.cancel()
        Thread.sleep(100)

        val (second, secondScope) = freshStore()
        val restarted = runBlocking(Dispatchers.IO) { CartPersistence(second, secondScope).load() }
        assertEquals("the basket survived the restart", 1, restarted.size)
        assertEquals(3, restarted.single().quantity)
        assertEquals(
            BigDecimal("1200.50"),
            restarted.single().product.price,
        )
    }

    @Test
    fun `an empty file is an empty basket, not a crash`() {
        val (ds, scope) = freshStore("fresh.preferences_pb")
        assertTrue(runBlocking(Dispatchers.IO) { CartPersistence(ds, scope).load() }.isEmpty())
    }

    @Test
    fun `a corrupt row is dropped and the rest of the basket survives`() {
        val (ds, scope) = freshStore("corrupt.preferences_pb")
        runBlocking(Dispatchers.IO) {
            ds.edit { prefs ->
                prefs[stringPreferencesKey("cart_v1")] =
                    """[{"id":"a","name":"جيد","price":"100","quantity":3},""" +
                        """{"id":"b","name":"تالف","price":"ليس رقماً","quantity":1}]"""
            }
        }
        val items = runBlocking(Dispatchers.IO) { CartPersistence(ds, scope).load() }
        assertEquals("only the usable row is kept", 1, items.size)
        assertEquals("a", items.single().product.id)
    }

    @Test
    fun `quantities outside the allowed range are clamped, not rejected`() {
        val (ds, scope) = freshStore("clamp.preferences_pb")
        runBlocking(Dispatchers.IO) {
            ds.edit { prefs ->
                prefs[stringPreferencesKey("cart_v1")] =
                    """[{"id":"a","name":"كثير","price":"10","quantity":900}]"""
            }
        }
        val items = runBlocking(Dispatchers.IO) { CartPersistence(ds, scope).load() }
        assertEquals(99, items.single().quantity)
    }

    @Test
    fun `the mirror follows later edits`() {
        val (ds, scope) = freshStore()
        val store = CartStore()
        CartPersistence(ds, scope).bind(store)

        store.add(product("a"), 1)
        assertTrue(awaitStored(ds) { it.contains("\"quantity\":1") }.contains("\"quantity\":1"))

        store.setQuantity("a", 5)
        assertTrue(awaitStored(ds) { it.contains("\"quantity\":5") }.contains("\"quantity\":5"))

        store.remove("a")
        assertEquals("an emptied basket is written as an empty list", "[]", awaitStored(ds) { it == "[]" })
    }
}
