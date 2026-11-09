package com.zakeleo877.saeedadmin.ui

import com.zakeleo877.saeedcore.model.Order
import com.zakeleo877.saeedcore.model.OrderChannel
import com.zakeleo877.saeedcore.model.OrderStatus
import com.zakeleo877.saeedcore.model.Product
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardStatsTest {
    private fun order(id: String, status: OrderStatus, time: String, total: String) = Order(
        id = id,
        number = id.toLong(),
        customer = "",
        phone = "",
        notes = "",
        customerLocationUrl = "",
        channel = OrderChannel.APP,
        status = status,
        items = emptyList(),
        total = BigDecimal(total),
        clientId = null,
        createdAt = Instant.parse(time),
    )

    private fun product(id: String) = Product(
        id = id, name = id, price = BigDecimal.ONE, description = "", imageUrl = "",
        category = "", available = true, sortOrder = 0, createdAt = Instant.EPOCH,
    )

    @Test
    fun `counts use the unfiltered loaded snapshot and sales only include handed over orders`() {
        val orders = listOf(
            order("1", OrderStatus.NEW, "2026-10-31T23:30:00Z", "100"),
            order("2", OrderStatus.HANDED_OVER, "2026-11-01T08:00:00Z", "250.50"),
            order("3", OrderStatus.CANCELLED, "2026-11-01T23:30:00Z", "999"),
        )
        val stats = calculateDashboardStats(
            orders, listOf(product("a"), product("b")),
            today = LocalDate.parse("2026-11-01"), zone = ZoneId.of("Africa/Algiers"),
        )
        assertEquals(1, stats.newOrders)
        assertEquals(2, stats.todayOrders)
        assertEquals(3, stats.totalOrders)
        assertEquals(2, stats.productCount)
        assertEquals(BigDecimal("250.50"), stats.handedOverTotal)
    }

    @Test
    fun `an empty loaded page has zero counts`() {
        assertEquals(DashboardStats(), calculateDashboardStats(emptyList(), emptyList()))
    }
}
