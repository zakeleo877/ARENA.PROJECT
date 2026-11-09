package com.zakeleo877.saeedcore.data

import com.zakeleo877.saeedcore.model.CartItem
import com.zakeleo877.saeedcore.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * The live basket the screens read.
 *
 * It is mirrored to disk by [CartPersistence], which the customer app wires at
 * startup; this class stays free of I/O and is therefore trivial to test.
 */
class CartStore(initial: List<CartItem> = emptyList()) {
    private val _items = MutableStateFlow(initial)
    val items: Flow<List<CartItem>> = _items.asStateFlow()

    val count: Flow<Int> = items.map { list -> list.sumOf { it.quantity } }

    fun add(product: Product, quantity: Int = 1) {
        require(quantity in 1..99)
        _items.update { current ->
            val existing = current.firstOrNull { it.product.id == product.id }
            if (existing == null) {
                current + CartItem(product, quantity.coerceAtMost(99))
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
        _items.update { current ->
            when {
                quantity <= 0 -> current.filterNot { it.product.id == productId }
                else -> current.map {
                    if (it.product.id == productId) it.copy(quantity = quantity.coerceAtMost(99)) else it
                }
            }
        }
    }

    fun remove(productId: String) {
        _items.update { current -> current.filterNot { it.product.id == productId } }
    }

    /** Returns the removed item so the UI can offer an undo action. */
    fun removeAndReturn(productId: String): CartItem? {
        val removed = _items.value.firstOrNull { it.product.id == productId }
        remove(productId)
        return removed
    }

    fun restore(item: CartItem) = add(item.product, item.quantity)

    /** Replaces the whole basket, used once when the stored one is read back. */
    fun restoreAll(items: List<CartItem>) {
        _items.value = items
    }

    fun clear() {
        _items.value = emptyList()
    }

    fun snapshot(): List<CartItem> = _items.value
}
