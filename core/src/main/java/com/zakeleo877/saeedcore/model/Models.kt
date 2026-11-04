package com.zakeleo877.saeedcore.model

import java.math.BigDecimal
import java.time.Instant

/** Prices shown by the client are indicative; place_order is authoritative. */
data class Product(
    val id: String,
    val name: String,
    val price: BigDecimal,
    val description: String,
    val imageUrl: String,
    val category: String,
    val available: Boolean,
    val sortOrder: Int,
    val createdAt: Instant,
)

data class StoreSettings(
    val storeName: String,
    val whatsapp: String,
    val currency: String,
    val footerNote: String,
)

data class CartItem(val product: Product, val quantity: Int) {
    init { require(quantity in 1..99) }
    val estimatedTotal: BigDecimal get() = product.price.multiply(quantity.toBigDecimal())
}

data class OrderItem(
    val productId: String,
    val name: String,
    val price: BigDecimal,
    val quantity: Int,
)

enum class OrderStatus(val databaseValue: String) {
    NEW("جديد"),
    PREPARING("قيد التحضير"),
    READY("جاهز"),
    HANDED_OVER("مُسلَّم"),
    CANCELLED("ملغي");

    companion object {
        fun fromDatabase(value: String): OrderStatus =
            entries.firstOrNull { it.databaseValue == value }
                ?: throw IllegalArgumentException("Unknown order status")
    }
}

enum class OrderChannel(val databaseValue: String) {
    APP("التطبيق"), MANUAL("يدوي")
}

data class Order(
    val id: String,
    val number: Long,
    val customer: String,
    val phone: String,
    val notes: String,
    val location: String,
    val channel: OrderChannel,
    val status: OrderStatus,
    val items: List<OrderItem>,
    val total: BigDecimal,
    val clientId: String?,
    val createdAt: Instant,
)

/** Immutable snapshot. A retry must use the SAME clientId and item snapshot. */
data class PendingOrder(
    val clientId: String,
    val customer: String,
    val phone: String,
    val notes: String,
    val location: String,
    val items: List<PendingOrderItem>,
)

data class PendingOrderItem(val productId: String, val quantity: Int)
