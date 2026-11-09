package com.zakeleo877.saeedcore.data

import com.zakeleo877.saeedcore.model.CartItem
import com.zakeleo877.saeedcore.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * The live basket the screens read. [CartPersistence] mirrors it to disk.
 *
 * When restoration is asynchronous, changes made before it finishes are
 * replayed on the restored basket in order. This prevents a slow disk read
 * from silently replacing a product the customer just added.
 */
class CartStore(
    initial: List<CartItem> = emptyList(),
    awaitingRestore: Boolean = false,
) {
    private val lock = Any()
    private val _items = MutableStateFlow(initial)
    val items: Flow<List<CartItem>> = _items.asStateFlow()
    val count: Flow<Int> = items.map { list -> list.sumOf { it.quantity } }

    private val restored = MutableStateFlow(!awaitingRestore)
    private var restorationPending = awaitingRestore
    private val pendingChanges = mutableListOf<(List<CartItem>) -> List<CartItem>>()

    /** Apply the same changes to the visible basket and, if needed, the saved one. */
    private fun change(transform: (List<CartItem>) -> List<CartItem>) {
        synchronized(lock) {
            _items.value = transform(_items.value)
            if (restorationPending) pendingChanges.add(transform)
        }
    }

    fun add(product: Product, quantity: Int = 1) {
        require(quantity in 1..99)
        change { current ->
            val existing = current.firstOrNull { it.product.id == product.id }
            if (existing == null) {
                current + CartItem(product, quantity)
            } else {
                current.map {
                    if (it.product.id == product.id) {
                        it.copy(quantity = (it.quantity + quantity).coerceAtMost(99))
                    } else {
                        it
                    }
                }
            }
        }
    }

    fun setQuantity(productId: String, quantity: Int) {
        change { current ->
            when {
                quantity <= 0 -> current.filterNot { it.product.id == productId }
                else -> current.map {
                    if (it.product.id == productId) it.copy(quantity = quantity.coerceAtMost(99)) else it
                }
            }
        }
    }

    fun remove(productId: String) {
        change { current -> current.filterNot { it.product.id == productId } }
    }

    /** Returns the removed item so the UI can offer an undo action. */
    fun removeAndReturn(productId: String): CartItem? = synchronized(lock) {
        val removed = _items.value.firstOrNull { it.product.id == productId }
        remove(productId)
        removed
    }

    fun restore(item: CartItem) = add(item.product, item.quantity)

    /** Applies the saved basket once, including edits made while it was loading. */
    fun restoreAll(items: List<CartItem>) {
        synchronized(lock) {
            if (restorationPending) {
                _items.value = pendingChanges.fold(items) { basket, change -> change(basket) }
                pendingChanges.clear()
                restorationPending = false
                restored.value = true
            }
        }
    }

    /** Checkout must not take a partial snapshot while restoration is in progress. */
    suspend fun awaitRestored() {
        restored.first { it }
    }

    fun clear() {
        change { emptyList() }
    }

    fun snapshot(): List<CartItem> = _items.value
}
