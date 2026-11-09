package com.zakeleo877.saeedcore.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zakeleo877.saeedcore.model.CartItem
import com.zakeleo877.saeedcore.model.Product
import java.math.BigDecimal
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Keeps the cart on disk, so closing the app no longer throws the basket away.
 *
 * The in-memory [CartStore] was documented as "the app persists it in
 * DataStore" and nothing did: a customer who filled a basket, took a phone call
 * and came back found it empty. That is the most expensive kind of bug in a
 * shop app - the customer has to pick everything again, and often does not.
 *
 * Two decisions worth knowing:
 *
 * * **A snapshot, not a list of ids.** The price shown in the basket is the
 *   price remembered here. Re-reading the catalogue to rebuild the basket would
 *   silently change what the customer agreed to; the server re-prices the order
 *   at checkout anyway, and the screen says so.
 * * **A bad row is dropped, not fatal.** Invalid rows are decoded independently;
 *   a malformed JSON document as a whole cannot be recovered and is ignored.
 */
class CartPersistence(
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Read the basket without letting a damaged row hide all the other rows. */
    suspend fun load(): List<CartItem> = try {
        val raw = dataStore.data
            .catch { emit(emptyPreferences()) }
            .first()[KEY]
        if (raw == null) {
            emptyList()
        } else {
            val rows = json.parseToJsonElement(raw) as? JsonArray
            rows?.mapNotNull { row ->
                try {
                    json.decodeFromJsonElement<StoredItem>(row).toItem()
                } catch (_: Exception) {
                    null
                }
            } ?: emptyList()
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        emptyList()
    }

    /**
     * Mirrors every change to disk.
     *
     * Called once, after [load] has been applied, so the first write is the
     * restored basket rather than the empty list the process started from.
     */
    fun bind(store: CartStore) {
        scope.launch {
            store.items.collect { items -> save(items) }
        }
    }

    /** A disk failure leaves the live basket usable; cancellation still propagates. */
    private suspend fun save(items: List<CartItem>) {
        try {
            dataStore.edit { prefs ->
                prefs[KEY] = json.encodeToString(items.map(StoredItem::from))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // A full or unavailable disk must not crash the shopping screen.
        }
    }

    @Serializable
    private data class StoredItem(
        val id: String,
        val name: String,
        val price: String,
        val image: String = "",
        val category: String = "",
        val createdAt: Long = 0L,
        val quantity: Int = 1,
    ) {
        /** Null when a stored row cannot be used, so one bad row cannot empty the basket. */
        fun toItem(): CartItem? = runCatching {
            require(id.isNotBlank() && name.isNotBlank())
            val decimal = BigDecimal(price)
            require(decimal.signum() >= 0)
            CartItem(
                product = Product(
                    id = id,
                    name = name,
                    price = decimal,
                    description = "",
                    imageUrl = image,
                    category = category,
                    available = true,
                    sortOrder = 0,
                    createdAt = Instant.ofEpochMilli(createdAt),
                ),
                // The model refuses 0 and anything above 99, and a file is not a
                // trusted source: clamp rather than throw away the row.
                quantity = quantity.coerceIn(1, 99),
            )
        }.getOrNull()

        companion object {
            fun from(item: CartItem) = StoredItem(
                id = item.product.id,
                name = item.product.name,
                price = item.product.price.toPlainString(),
                image = item.product.imageUrl,
                category = item.product.category,
                createdAt = item.product.createdAt.toEpochMilli(),
                quantity = item.quantity,
            )
        }
    }

    companion object {
        /** Named so a future change of shape can read the old key and migrate. */
        internal val KEY = stringPreferencesKey("cart_v1")
    }
}
