package com.zakeleo877.saeedstore.ui

/** Keep destination names distinct: "products" is not a product detail route. */
internal object CustomerRoutes {
    const val HOME = "catalog"
    const val PRODUCTS = "products"
    const val CART = "cart"
    const val PENDING = "pending"
    const val DETAIL = "product"
    const val DETAIL_PATTERN = "$DETAIL?id={id}"
}

internal enum class SaeedTab { HOME, PRODUCTS, CART, PENDING }

internal fun customerTabForRoute(route: String?): SaeedTab = when (route) {
    CustomerRoutes.PRODUCTS -> SaeedTab.PRODUCTS
    CustomerRoutes.CART -> SaeedTab.CART
    CustomerRoutes.PENDING -> SaeedTab.PENDING
    else -> SaeedTab.HOME
}

internal fun customerShowsBottomBar(route: String?): Boolean =
    route != null && route != CustomerRoutes.DETAIL && !route.startsWith("${CustomerRoutes.DETAIL}?")
