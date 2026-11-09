package com.zakeleo877.saeedadmin.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderNotificationCursorTest {
    @Test
    fun `blocked notification leaves every unseen number for later`() {
        val recorded = mutableListOf<Long>()
        val allPosted = postAndRecordNumbers(listOf(2, 1), 0, { false }, { recorded += it })
        assertFalse(allPosted)
        assertTrue(recorded.isEmpty())
    }

    @Test
    fun `a partial failure records only numbers actually posted`() {
        val recorded = mutableListOf<Long>()
        val posted = mutableListOf<Long>()
        val allPosted = postAndRecordNumbers(listOf(3, 1, 2), 0, {
            posted += it
            it != 2L
        }, { recorded += it })
        assertFalse(allPosted)
        assertEquals(listOf(1L, 2L), posted)
        assertEquals(listOf(1L), recorded)
    }

    @Test
    fun `old and repeated numbers are not posted twice`() {
        val recorded = mutableListOf<Long>()
        val posted = mutableListOf<Long>()
        val allPosted = postAndRecordNumbers(listOf(5, 4, 5, 3), 3, {
            posted += it
            true
        }, { recorded += it })
        assertTrue(allPosted)
        assertEquals(listOf(4L, 5L), posted)
        assertEquals(posted, recorded)
    }
}
