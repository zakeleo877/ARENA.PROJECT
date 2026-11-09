package com.zakeleo877.saeedstore.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerNavigationTest {
    @Test
    fun `all four real top level screens keep the bottom bar`() {
        assertTrue(customerShowsBottomBar(CustomerRoutes.HOME))
        assertTrue(customerShowsBottomBar(CustomerRoutes.PRODUCTS))
        assertTrue(customerShowsBottomBar(CustomerRoutes.CART))
        assertTrue(customerShowsBottomBar(CustomerRoutes.PENDING))
        assertEquals(SaeedTab.PRODUCTS, customerTabForRoute(CustomerRoutes.PRODUCTS))
        assertEquals(SaeedTab.CART, customerTabForRoute(CustomerRoutes.CART))
        assertEquals(SaeedTab.PENDING, customerTabForRoute(CustomerRoutes.PENDING))
    }

    @Test
    fun `product detail hides the bar without hiding products`() {
        assertFalse(customerShowsBottomBar(null))
        assertFalse(customerShowsBottomBar(CustomerRoutes.DETAIL))
        assertFalse(customerShowsBottomBar(CustomerRoutes.DETAIL_PATTERN))
        assertFalse(customerShowsBottomBar("product?id=42"))
        assertTrue(customerShowsBottomBar("products"))
    }
}
