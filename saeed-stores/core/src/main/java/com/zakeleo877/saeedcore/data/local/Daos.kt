package com.zakeleo877.saeedcore.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Query("SELECT * FROM products ORDER BY sort_order ASC, created_at DESC")
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM settings")
    fun observeSettings(): Flow<List<SettingEntity>>

    @Query("SELECT * FROM cache_metadata WHERE `key` = :key LIMIT 1")
    fun observeMetadata(key: String): Flow<CacheMetadataEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Query("DELETE FROM products")
    suspend fun clearProducts()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: List<SettingEntity>)

    @Query("DELETE FROM settings")
    suspend fun clearSettings()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateMetadata(metadata: CacheMetadataEntity)

    // Delete + insert + freshness timestamp must be all-or-nothing.
    @Transaction
    suspend fun replaceProducts(products: List<ProductEntity>, atMillis: Long) {
        clearProducts()
        insertProducts(products)
        updateMetadata(CacheMetadataEntity("products", atMillis))
    }

    @Transaction
    suspend fun replaceSettings(settings: List<SettingEntity>, atMillis: Long) {
        clearSettings()
        insertSettings(settings)
        updateMetadata(CacheMetadataEntity("settings", atMillis))
    }
}

@Dao
interface PendingOrderDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(order: PendingOrderEntity)

    @Query("SELECT * FROM pending_orders ORDER BY created_at ASC")
    fun observeAll(): Flow<List<PendingOrderEntity>>

    @Query("SELECT * FROM pending_orders WHERE client_id = :clientId LIMIT 1")
    suspend fun find(clientId: String): PendingOrderEntity?

    @Query("DELETE FROM pending_orders WHERE client_id = :clientId")
    suspend fun delete(clientId: String)
}
