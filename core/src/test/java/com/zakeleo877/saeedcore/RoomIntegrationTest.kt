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
