package com.zakeleo877.saeedstore.ui

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Only the isolated emulator opts into these examples; normal builds seed nothing. */
class DemoCatalogTest {
    @Test
    fun `the optional demo JSON is decoded with its real serializer`() {
        val demo = parseDemoCatalog(
            """[{"id":"example","name":"منتج تجريبي","price":"100","category":"العناية","originalPrice":"150","rating":4.5}]""",
        )
        assertEquals("منتج تجريبي", demo?.single()?.name)
        assertEquals(BigDecimal("100"), demo?.single()?.price)
        assertEquals(BigDecimal("150"), demo?.single()?.originalPrice)
    }

    @Test
    fun `an empty or invalid value never seeds products`() {
        assertNull(parseDemoCatalog(""))
        assertNull(parseDemoCatalog("not valid JSON"))
        assertNull(parseDemoCatalog("[]"))
    }
}
