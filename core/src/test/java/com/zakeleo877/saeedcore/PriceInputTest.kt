package com.zakeleo877.saeedcore

import com.zakeleo877.saeedcore.util.Formatters
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * What a price field accepts on an Algerian phone.
 *
 * The Arabic keyboard types Arabic-Indic digits and the Persian layout types a
 * different code point with the same shape. Before this, the shop told the
 * manager the price was invalid while four perfectly good digits sat on the
 * screen.
 *
 * The comma is the dangerous part. On a numeric keypad it is a decimal mark, so
 * "12,50" is twelve and a half; on a price with grouping it is a thousands mark,
 * so "1,250" is one thousand two hundred and fifty. Reading either one wrong
 * changes what the customer pays.
 */
class PriceInputTest {

    @Test
    fun `plain digits are read exactly`() {
        assertEquals(BigDecimal("2500"), Formatters.parsePrice("2500"))
        assertEquals(BigDecimal("12.50"), Formatters.parsePrice("12.50"))
        assertEquals(BigDecimal("0.10"), Formatters.parsePrice("0.10"))
    }

    @Test
    fun `arabic-indic digits are read, not rejected`() {
        assertEquals(BigDecimal("2500"), Formatters.parsePrice("٢٥٠٠"))
        assertEquals(BigDecimal("12.50"), Formatters.parsePrice("١٢.٥٠"))
    }

    @Test
    fun `persian digits are read the same way`() {
        assertEquals(BigDecimal("2500"), Formatters.parsePrice("۲۵۰۰"))
    }

    @Test
    fun `the arabic decimal separator is a decimal point`() {
        // U+066B is the Arabic decimal separator, and U+066C is the grouping one.
        assertEquals(BigDecimal("12.50"), Formatters.parsePrice("١٢٫٥٠"))
        assertEquals(BigDecimal("1250"), Formatters.parsePrice("١٬٢٥٠"))
    }

    @Test
    fun `a comma before two digits is a decimal mark`() {
        // The case that overcharges a hundred fold if read as grouping.
        assertEquals(BigDecimal("12.50"), Formatters.parsePrice("12,50"))
        assertEquals(BigDecimal("0.75"), Formatters.parsePrice("0,75"))
    }

    @Test
    fun `a comma inside a grouped number is a thousands mark`() {
        assertEquals(BigDecimal("1250"), Formatters.parsePrice("1,250"))
        assertEquals(BigDecimal("1234567"), Formatters.parsePrice("1,234,567"))
        assertEquals(BigDecimal("1234567.89"), Formatters.parsePrice("1,234,567.89"))
    }

    @Test
    fun `a bare space between groups is tolerated`() {
        assertEquals(BigDecimal("2500"), Formatters.parsePrice("2 500"))
    }

    @Test
    fun `a negative price is refused`() {
        assertNull(Formatters.parsePrice("-100"))
    }

    @Test
    fun `something that is not a number is refused`() {
        assertNull(Formatters.parsePrice("abc"))
        assertNull(Formatters.parsePrice("12.3.4"))
        assertNull(Formatters.parsePrice("1,2,3"))
        assertNull(Formatters.parsePrice(""))
        assertNull(Formatters.parsePrice("   "))
    }

    @Test
    fun `the parser and the formatter agree on what a price looks like`() {
        // What the app writes on a receipt must be readable back as the same
        // number, or an order total would not survive a round trip.
        val amounts = listOf("0.10", "12.50", "1999.99", "12345.50")
        for (amount in amounts) {
            val printed = Formatters.money(BigDecimal(amount), "دج")
            val digitsOnly = printed.substringBefore('\u00A0')
            assertEquals(amount, Formatters.parsePrice(digitsOnly)?.toPlainString())
        }
    }
}
