package com.zakeleo877.saeedstore.ui

import com.zakeleo877.saeedcore.model.Product
import com.zakeleo877.saeedcore.util.ArabicText

/** The same real catalogue and filters are used on home and products. */
internal fun filterCatalog(
    products: List<Product>,
    query: String,
    category: String?,
    offersOnly: Boolean,
): List<Product> = products.filter { product ->
    val matchesQuery = query.isBlank() ||
        ArabicText.contains(product.name, query.trim()) ||
        ArabicText.contains(product.description, query.trim()) ||
        ArabicText.contains(product.category, query.trim())
    val matchesCategory = category == null || product.category == category
    val matchesOffer = !offersOnly || product.hasDiscount
    matchesQuery && matchesCategory && matchesOffer
}
