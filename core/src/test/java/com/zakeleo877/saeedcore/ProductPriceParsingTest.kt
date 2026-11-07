package com.zakeleo877.saeedcore

import com.zakeleo877.saeedcore.data.remote.CatalogApi
import com.zakeleo877.saeedcore.data.remote.SupabasePublicConfig
import com.zakeleo877.saeedcore.data.remote.anonymousHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The prices a shop can actually type, read through the same code the app uses.
 *
 * PostgREST returns a numeric column as a JSON string, so the app receives text
 * and turns it into a decimal. That step is where a price can silently change:
 * a Double would lose digits on a large amount, and an empty string read as zero
 * would make a product free. The customer pays whatever comes out of here, so
 * every case below is a payment.
 */
class ProductPriceParsingTest {

    private fun catalogue(productJson: String): CatalogApi {
        val client = anonymousHttpClient(
            MockEngine {
                respond(
                    "[$productJson]",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        )
        return CatalogApi(client, SupabasePublicConfig("https://project.example", "sb_publishable_test"))
    }

    private fun wire(price: String, extra: String = "") = """
        {"id":"p1","name":"منتج","price":$price,"description":"","image_url":"",
         "category":"عام","available":true,"sort_order":0,
         "created_at":"2026-01-01T00:00:00+00:00"$extra}
    """.trimIndent()

    private suspend fun readPrice(price: String) =
        catalogue(wire(price)).products().single().price

    @Test
    fun `a price is read exactly, without a floating point detour`() = runTest {
        assertEquals(BigDecimal("0.10"), readPrice("\"0.10\""))
        assertEquals(BigDecimal("2500.00"), readPrice("\"2500.00\""))
        assertEquals(BigDecimal("1999.99"), readPrice("\"1999.99\""))
    }

    @Test
    fun `a large price keeps every digit`() = runTest {
        // The column allows ten integer digits. A Double loses the last ones,
        // which would show on a receipt and never be noticed in testing.
        assertEquals(BigDecimal("9999999999.99"), readPrice("\"9999999999.99\""))
    }

    @Test
    fun `a negative price is refused`() = runTest {
        val thrown = runCatching { readPrice("\"-1.00\"") }.exceptionOrNull()
        assertTrue("a negative price must be refused", thrown is IllegalArgumentException)
        assertEquals("Invalid catalog price.", thrown!!.message)
    }

    @Test
    fun `an empty price is refused rather than read as zero`() = runTest {
        // Zero is a free product. Refusing is the only safe reading of "".
        val thrown = runCatching { readPrice("\"\"") }.exceptionOrNull()
        assertTrue("an empty price must be refused", thrown is IllegalArgumentException)
    }

    @Test
    fun `a price that is not a number is refused`() = runTest {
        val thrown = runCatching { readPrice("\"abc\"") }.exceptionOrNull()
        assertTrue("a non-numeric price must be refused", thrown is IllegalArgumentException)
    }

    @Test
    fun `an old price below the price is dropped, not shown as a discount`() = runTest {
        val product = catalogue(wire("\"2000.00\"", ",\"original_price\":\"1500.00\""))
            .products().single()
        assertNull("a lower old price is not a discount", product.originalPrice)
        assertFalse(product.hasDiscount)
    }

    @Test
    fun `a rating above five hides the stars without hiding the product`() = runTest {
        val product = catalogue(wire("\"100.00\"", ",\"rating\":\"7.5\""))
            .products().single()
        assertNull("a rating above five is not a rating", product.rating)
        assertEquals(BigDecimal("100.00"), product.price)
    }

    @Test
    fun `a rating the shop really uses survives`() = runTest {
        val product = catalogue(wire("\"100.00\"", ",\"rating\":\"4.5\""))
            .products().single()
        assertEquals(4.5, product.rating!!, 0.0001)
    }
}
