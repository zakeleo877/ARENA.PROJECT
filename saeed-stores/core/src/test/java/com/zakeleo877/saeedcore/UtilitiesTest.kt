package com.zakeleo877.saeedcore

import com.zakeleo877.saeedcore.util.Formatters
import com.zakeleo877.saeedcore.util.Links
import com.zakeleo877.saeedcore.util.Validation
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class UtilitiesTest {
    @Test fun moneyUsesWesternDigitsAndDecimalPrecision() {
        assertEquals("12,345.50\u00A0دج", Formatters.money(BigDecimal("12345.50"), "دج"))
        assertEquals("0.10\u00A0دج", Formatters.money(BigDecimal("0.10"), "دج"))
    }

    @Test fun orderNumbersAreAtLeastThreeDigits() {
        assertEquals("#007", Formatters.orderNumber(7))
        assertEquals("#1234", Formatters.orderNumber(1234))
    }

    @Test fun dateUsesExplicitZoneAndWesternDigits() {
        assertEquals("2026/01/01 01:00", Formatters.dateTime(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("Africa/Algiers")))
    }

    @Test fun phoneAndLimitsMatchTheDatabase() {
        assertTrue(Validation.phone("0555 555 511"))
        assertFalse(Validation.phone("++++++++"))
        assertFalse(Validation.phone("0555-555-511"))
        assertTrue(Validation.quantity(99))
        assertFalse(Validation.quantity(100))
        assertTrue(Validation.notes("x".repeat(500)))
        assertFalse(Validation.notes("x".repeat(501)))
    }

    @Test fun linksRejectUnsafeInputAndEncodeMessage() {
        assertEquals("https://wa.me/213776151686?text=%D9%85%D8%B1%D8%AD%D8%A8%D8%A7%20%D8%A8%D9%83", Links.whatsapp("213776151686", "مرحبا بك"))
        assertNull(Links.whatsapp("+213776151686"))
        assertFalse(Links.safeHttpsUrl("http://example.com/image.webp"))
        assertFalse(Links.safeHttpsUrl("https://user@example.com/a"))
        assertFalse(Links.safeHttpsUrl("not a url"))
        assertTrue(Links.safeHttpsUrl("https://example.com/image.webp"))
    }
}
