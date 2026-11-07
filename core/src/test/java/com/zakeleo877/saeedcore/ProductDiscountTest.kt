package com.zakeleo877.saeedcore

import com.zakeleo877.saeedcore.model.Product
import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The discount shown on a card is derived, never stored, so these rules are the
 * only thing standing between a customer and a misleading price.
 */
class ProductDiscountTest {

    private fun product(
        price: String,
        original: String? = null,
        rating: Double? = null,
    ) = Product(
        id = "p1",
        name = "منتج",
        price = BigDecimal(price),
        description = "",
        imageUrl = "",
        category = "عام",
        available = true,
        sortOrder = 0,
        createdAt = Instant.EPOCH,
        originalPrice = original?.let(::BigDecimal),
        rating = rating,
    )

    @Test
    fun `no original price means no discount`() {
        val p = product("1999.00")
        assertFalse(p.hasDiscount)
        assertNull(p.discountPercent)
    }

    @Test
    fun `an original price equal to the price is not a discount`() {
        val p = product("1999.00", original = "1999.00")
        assertFalse(p.hasDiscount)
        assertNull(p.discountPercent)
    }

    @Test
    fun `an original price below the price is not a discount`() {
        val p = product("1999.00", original = "1500.00")
        assertFalse(p.hasDiscount)
        assertNull(p.discountPercent)
    }

    @Test
    fun `the percentage is rounded down so the shop never overstates it`() {
        // 200.00 of 1999.00 is 10.005%, which must read as 10% and not 11%.
        val p = product("1799.00", original = "1999.00")
        assertTrue(p.hasDiscount)
        assertEquals(10, p.discountPercent?.toInt())
    }

    @Test
    fun `a clean percentage is exact`() {
        val p = product("800.00", original = "1000.00")
        assertEquals(20, p.discountPercent?.toInt())
    }

    @Test
    fun `a rating is carried through unchanged and may be absent`() {
        assertEquals(4.5, product("100.00", rating = 4.5).rating!!, 0.0001)
        assertNull(product("100.00").rating)
    }
}
