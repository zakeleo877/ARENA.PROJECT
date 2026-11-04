package com.zakeleo877.saeedcore.data.local

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.zakeleo877.saeedcore.model.Product
import java.math.BigDecimal
import java.time.Instant

@Entity(tableName = "products", indices = [Index(value = ["sort_order", "created_at"])])
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** Decimal string avoids binary floating-point rounding. */
    val price: String,
    val description: String,
    @ColumnInfo(name = "image_url") val imageUrl: String,
    val category: String,
    val available: Boolean,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "created_at") val createdAtMillis: Long,
) {
    fun model() = Product(
        id, name, BigDecimal(price), description, imageUrl, category, available, sortOrder,
        Instant.ofEpochMilli(createdAtMillis),
    )

    companion object {
        fun from(model: Product) = ProductEntity(
            model.id, model.name, model.price.toPlainString(), model.description,
            model.imageUrl, model.category, model.available, model.sortOrder, model.createdAt.toEpochMilli(),
        )
    }
}

@Entity(tableName = "settings")
data class SettingEntity(@PrimaryKey val key: String, val value: String)

@Entity(tableName = "cache_metadata")
data class CacheMetadataEntity(@PrimaryKey val key: String, val updatedAtMillis: Long)

/** Local queue storage only. Scheduling/retry/UI are implemented in the customer phase. */
@Entity(tableName = "pending_orders", indices = [Index(value = ["created_at"])])
data class PendingOrderEntity(
    @PrimaryKey @ColumnInfo(name = "client_id") val clientId: String,
    val customer: String,
    val phone: String,
    val notes: String,
    /** Contact-only map link for shop-to-customer contact. */
    @ColumnInfo(name = "location") val location: String,
    @ColumnInfo(name = "items_json") val itemsJson: String,
    val state: String,
    @ColumnInfo(name = "attempt_count") val attemptCount: Int,
    @ColumnInfo(name = "created_at") val createdAtMillis: Long,
    @ColumnInfo(name = "last_error_code") val lastErrorCode: String?,
)
