package com.zakeleo877.saeedcore.data

import com.zakeleo877.saeedcore.model.CartItem
import com.zakeleo877.saeedcore.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory cart for the current session.
 * The customer app persists it in DataStore; this class holds only the live state.
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

    fun clear() {
        _items.value = emptyList()
    }

    fun snapshot(): List<CartItem> = _items.value
}
