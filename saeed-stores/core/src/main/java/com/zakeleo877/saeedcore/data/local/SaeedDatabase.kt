package com.zakeleo877.saeedcore.data.local

import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver

@Database(
    entities = [ProductEntity::class, SettingEntity::class, CacheMetadataEntity::class, PendingOrderEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class SaeedDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
    abstract fun pendingOrderDao(): PendingOrderDao

    companion object {
        fun create(context: Context): SaeedDatabase =
            Room.databaseBuilder<SaeedDatabase>(context.applicationContext, "saeed-stores.db")
                // Android's SQLite driver avoids adding native SQLite binaries to both APKs.
                .setDriver(AndroidSQLiteDriver())
                .build()
    }
}
