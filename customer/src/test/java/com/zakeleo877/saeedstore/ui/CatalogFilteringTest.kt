package com.zakeleo877.saeedstore.ui

import com.zakeleo877.saeedcore.model.Product
import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogFilteringTest {
    private fun product(
        id: String,
        name: String,
        category: String,
        description: String = "",
        available: Boolean = true,
        originalPrice: String? = null,
    ) = Product(
        id = id,
        name = name,
        price = BigDecimal("100"),
        description = description,
        imageUrl = "",
        category = category,
        available = available,
        sortOrder = 0,
        createdAt = Instant.EPOCH,
        originalPrice = originalPrice?.let(::BigDecimal),
    )

    private val products = listOf(
        product("a", "أداة تصفيف", "العناية", "للشعر", originalPrice = "150"),
        product("b", "مشط", "العناية", "خشبي", available = false),
        product("c", "كريم", "التجميل", "للوجه", originalPrice = "100"),
    )

    @Test
    fun `search folds Arabic and checks real names descriptions and categories`() {
        assertEquals(listOf("a"), filterCatalog(products, "  اداة  ", null, false).map { it.id })
        assertEquals(listOf("a"), filterCatalog(products, "للشعر", null, false).map { it.id })
        assertEquals(listOf("c"), filterCatalog(products, "التجميل", null, false).map { it.id })
    }

    @Test
    fun `category and real discount filters combine without hiding unavailable products`() {
        assertEquals(listOf("a", "b"), filterCatalog(products, "", "العناية", false).map { it.id })
        assertEquals(listOf("a"), filterCatalog(products, "", "العناية", true).map { it.id })
        assertTrue(filterCatalog(products, "", "التجميل", true).isEmpty())
        assertEquals(products, filterCatalog(products, "", null, false))
    }
}
