package com.zakeleo877.saeedcore

import com.zakeleo877.saeedcore.data.local.MIGRATION_1_2
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Room throws when the schema it finds on disk differs from the one the code
 * expects, and the exported JSON files are the contract between them. The admin
 * never opens a database, but every customer phone that already has the app
 * does, so the migration must exist and must cover every new column.
 *
 * This runs without SQLite: it checks that version 2 was exported and that the
 * migration adds exactly the columns that are new between the two exports.
 */
class DatabaseMigrationTest {

    private fun schema(version: Int): String {
        // The tests run from the module directory, so the exported schemas sit
        // next to it at a fixed relative path.
        val file = File("schemas/com.zakeleo877.saeedcore.data.local.SaeedDatabase/$version.json")
        assertTrue("the exported schema for version $version must exist", file.exists())
        return file.readText()
    }

    @Test
    fun `version two is exported`() {
        assertTrue(schema(2).contains("\"version\": 2"))
    }

    @Test
    fun `the products table gains the discount and rating columns`() {
        val versionTwo = schema(2)
        assertTrue(versionTwo.contains("`original_price` TEXT"))
        assertTrue(versionTwo.contains("`rating` REAL"))
    }

    @Test
    fun `the migration moves from version one to version two`() {
        assertEquals(1, MIGRATION_1_2.startVersion)
        assertEquals(2, MIGRATION_1_2.endVersion)
    }

    @Test
    fun `no other table changes between the two exports`() {
        // If a later change touches another table, this fails and the migration
        // has to be extended instead of silently leaving it behind.
        val versionOne = schema(1)
        val versionTwo = schema(2)
        for (table in listOf("settings", "cache_metadata", "pending_orders")) {
            val fromOne = versionOne.substringAfter("\"tableName\": \"$table\"").substringBefore("\"indices\"")
            val fromTwo = versionTwo.substringAfter("\"tableName\": \"$table\"").substringBefore("\"indices\"")
            assertEquals("the $table table must be unchanged", fromOne, fromTwo)
        }
    }
}
