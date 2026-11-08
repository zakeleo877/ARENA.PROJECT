package com.zakeleo877.saeedcore

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.zakeleo877.saeedcore.data.local.PendingOrderEntity
import com.zakeleo877.saeedcore.data.local.ProductEntity
import com.zakeleo877.saeedcore.data.local.SaeedDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.zakeleo877.saeedcore.data.local.MIGRATION_1_2
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 28])
class RoomIntegrationTest {
    private lateinit var database: SaeedDatabase

    @Before fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder<SaeedDatabase>(context)
            .setDriver(AndroidSQLiteDriver())
            .build()
    }

    @After fun closeDatabase() { database.close() }

    /**
     * Runs the upgrade on a real SQLite file that has the version 1 shape.
     *
     * Room itself cannot be asked for a version 1 database once the entity is at
     * version 2, because opening the file would create it at the newest version.
     * So the old table is created by hand, exactly as version 1 exported it, and
     * the migration is applied to it directly. A phone that already had the app
     * takes the same path.
     */
    @Test fun theUpgradeKeepsCachedProductsAndAddsTheNewColumns(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = java.io.File(context.cacheDir, "legacy-${System.nanoTime()}.db")
        val driver = AndroidSQLiteDriver()
        val connection: SQLiteConnection = driver.open(file.absolutePath)

        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `products` (" +
                "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `price` TEXT NOT NULL, " +
                "`description` TEXT NOT NULL, `image_url` TEXT NOT NULL, `category` TEXT NOT NULL, " +
                "`available` INTEGER NOT NULL, `sort_order` INTEGER NOT NULL, " +
                "`created_at` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        connection.execSQL(
            "INSERT INTO `products` VALUES " +
                "('p1', 'منتج قديم', '12.30', '', '', 'عام', 1, 0, 1000)",
        )

        MIGRATION_1_2.migrate(connection)

        connection.prepare("SELECT id, original_price, rating FROM products").use { statement ->
            assertTrue("the old row must survive the upgrade", statement.step())
            assertEquals("p1", statement.getText(0))
            assertNull("an old row has no discount", statement.getText(1))
            assertNull("an old row has no rating", statement.getText(2))
        }
        // The columns must accept values once they exist.
        connection.execSQL("UPDATE products SET original_price = '20.00', rating = 4.5 WHERE id = 'p1'")
        connection.prepare("SELECT original_price, rating FROM products").use { statement ->
            assertTrue(statement.step())
            assertEquals("20.00", statement.getText(0))
            assertEquals(4.5, statement.getDouble(1), 0.0001)
        }

        connection.close()
        file.delete()
    }

    @Test fun catalogReplacementPersistsItemsAndTimestamp() = runTest {
        val dao = database.catalogDao()
        val product = ProductEntity("p1", "منتج", "12.30", "", "", "عام", false, 0, 1000)
        dao.replaceProducts(listOf(product), 1234)
        assertEquals(listOf(product), dao.observeProducts().first())
        assertEquals(1234L, dao.observeMetadata("products").first()?.updatedAtMillis)
        dao.replaceProducts(emptyList(), 5678)
        assertTrue(dao.observeProducts().first().isEmpty())
        assertEquals(5678L, dao.observeMetadata("products").first()?.updatedAtMillis)
    }

    @Test fun queueRetainsOneSnapshotPerClientId() = runTest {
        val dao = database.pendingOrderDao()
        val item = PendingOrderEntity("id-1", "اسم", "0555555511", "", "", "[]", "pending", 0, 1234, null)
        dao.insert(item)
        assertEquals(item, dao.find("id-1"))
        try {
            dao.insert(item.copy(customer = "اسم آخر"))
            fail("Duplicate client IDs must not replace the original snapshot")
        } catch (_: android.database.sqlite.SQLiteConstraintException) { }
        assertEquals(item, dao.find("id-1"))
        dao.delete("id-1")
        assertNull(dao.find("id-1"))
    }
}
