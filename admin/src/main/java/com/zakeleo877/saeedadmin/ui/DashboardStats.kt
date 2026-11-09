package com.zakeleo877.saeedadmin.ui

import com.zakeleo877.saeedcore.model.Order
import com.zakeleo877.saeedcore.model.OrderStatus
import com.zakeleo877.saeedcore.model.Product
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

/**
 * These describe the loaded, unfiltered snapshot, not a server-wide aggregate.
 * The orders endpoint deliberately returns only the latest page.
 */
internal fun calculateDashboardStats(
    orders: List<Order>,
    products: List<Product>,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): DashboardStats = DashboardStats(
    newOrders = orders.count { it.status == OrderStatus.NEW },
    todayOrders = orders.count { it.createdAt.atZone(zone).toLocalDate() == today },
    totalOrders = orders.size,
    productCount = products.size,
    handedOverTotal = orders
        .filter { it.status == OrderStatus.HANDED_OVER }
        .fold(BigDecimal.ZERO) { acc, order -> acc + order.total },
)
