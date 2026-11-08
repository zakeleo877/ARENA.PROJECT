package com.zakeleo877.saeedcore

import com.zakeleo877.saeedcore.model.Order
import com.zakeleo877.saeedcore.util.CsvExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    @Test fun theDocumentStartsWithTheUtf8ByteOrderMark() {
        val csv = CsvExporter.build(listOf("الاسم"), listOf(listOf("منتج")))
        assertTrue("Excel needs the UTF-8 BOM", csv.startsWith("\uFEFF"))
        assertEquals("\uFEFFالاسم\r\nمنتج\r\n", csv)
    }

    @Test fun plainFieldsAreNotQuoted() {
        assertEquals("منتج", CsvExporter.field("منتج"))
        assertEquals("100", CsvExporter.field("100"))
    }

    @Test fun fieldsWithASeparatorAreQuoted() {
        assertEquals("\"سميت, كيتشي\"", CsvExporter.field("سميت, كيتشي"))
    }

    @Test fun embeddedQuotesAreDoubled() {
        assertEquals("\"قال \"\"مرحبا\"\"\"", CsvExporter.field("قال \"مرحبا\""))
    }

    @Test fun lineBreaksForceQuoting() {
        assertEquals("\"سطر\nثاني\"", CsvExporter.field("سطر\nثاني"))
    }

    @Test fun aMissingValueBecomesAnEmptyField() {
        assertEquals("", CsvExporter.field(null))
    }

    @Test fun theOrderModelExposesTheMapLinkAsCustomerLocationUrl() {
        // The name is part of the published contract: the map link is a
        // contact-only detail for the shop to reach the customer.
        val property = Order::class.java.declaredFields.firstOrNull { it.name == "customerLocationUrl" }
        assertTrue("Order must expose customerLocationUrl", property != null)
    }

    @Test fun everyRowEndsWithALineBreak() {
        val csv = CsvExporter.build(
            headers = listOf("الاسم", "السعر"),
            rows = listOf(listOf("منتج", "100"), listOf("آخر", "250")),
        )
        assertEquals(
            "\uFEFFالاسم,السعر\r\nمنتج,100\r\nآخر,250\r\n",
            csv,
        )
    }
}
