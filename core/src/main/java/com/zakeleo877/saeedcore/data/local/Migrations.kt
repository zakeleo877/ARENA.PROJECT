package com.zakeleo877.saeedcore.data.local

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Adds the optional discount and rating columns to the cached catalog.
 *
 * Both columns are nullable and carry no default, so every existing row reads
 * back as "no discount" and "not rated" instead of a misleading zero. SQLite
 * supports `ADD COLUMN` without a table rewrite, so the cached catalog survives
 * the upgrade and the customer keeps seeing products offline.
 *
 * Room validates the resulting schema against the exported version 2 before the
 * database is opened; a mismatch here fails loudly instead of showing a table
 * that is subtly wrong.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE `products` ADD COLUMN `original_price` TEXT")
        connection.execSQL("ALTER TABLE `products` ADD COLUMN `rating` REAL")
    }
}
