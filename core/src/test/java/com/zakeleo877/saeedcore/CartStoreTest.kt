package com.zakeleo877.saeedcore

import com.zakeleo877.saeedcore.data.CartStore
import com.zakeleo877.saeedcore.model.Product
import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CartStoreTest {
    private fun product(id: String, price: String = "100.00") = Product(
        id = id,
        name = "منتج $id",
        price = BigDecimal(price),
        description = "",
        imageUrl = "",
        category = "عام",
        available = true,
        sortOrder = 0,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    @Test fun addingTheSameProductTwiceIncreasesTheQuantity() {
        val cart = CartStore()
        cart.add(product("a"))
        cart.add(product("a"))
        assertEquals(1, cart.snapshot().size)
        assertEquals(2, cart.snapshot().first().quantity)
    }

    @Test fun quantityIsCappedAtNinetyNine() {
        val cart = CartStore()
        cart.add(product("a"), 99)
        cart.add(product("a"), 99)
        assertEquals(99, cart.snapshot().first().quantity)
    }

    @Test fun settingQuantityToZeroRemovesTheLine() {
        val cart = CartStore()
        cart.add(product("a"))
        cart.setQuantity("a", 0)
        assertTrue(cart.snapshot().isEmpty())
    }

    @Test fun removingReturnsTheLineSoItCanBeRestored() {
        val cart = CartStore()
        cart.add(product("a"), 3)
        val removed = cart.removeAndReturn("a")
        assertEquals(3, removed?.quantity)
        assertTrue(cart.snapshot().isEmpty())
        removed?.let { cart.restore(it) }
        assertEquals(1, cart.snapshot().size)
    }

    @Test fun removingAnAbsentProductReturnsNull() {
        assertNull(CartStore().removeAndReturn("missing"))
    }

    @Test fun clearingEmptiesTheCart() {
        val cart = CartStore()
        cart.add(product("a"))
        cart.clear()
        assertTrue(cart.snapshot().isEmpty())
    }
}
