package com.zakeleo877.saeedstore.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zakeleo877.saeedcore.designsystem.RemoteImage
import com.zakeleo877.saeedcore.designsystem.SaeedBrandMark
import com.zakeleo877.saeedcore.designsystem.SaeedButton
import com.zakeleo877.saeedcore.designsystem.SaeedCardSurface
import com.zakeleo877.saeedcore.designsystem.SaeedChipSurface
import com.zakeleo877.saeedcore.designsystem.SaeedDiscountRed
import com.zakeleo877.saeedcore.designsystem.SaeedEmptyState
import com.zakeleo877.saeedcore.designsystem.SaeedGold
import com.zakeleo877.saeedcore.designsystem.SaeedGoldSoft
import com.zakeleo877.saeedcore.designsystem.SaeedGreen
import com.zakeleo877.saeedcore.designsystem.SaeedGreenDark
import com.zakeleo877.saeedcore.designsystem.SaeedInk
import com.zakeleo877.saeedcore.designsystem.SaeedInkMuted
import com.zakeleo877.saeedcore.designsystem.SaeedOutline
import com.zakeleo877.saeedcore.designsystem.SaeedSearchField
import com.zakeleo877.saeedcore.designsystem.SaeedSkeleton
import com.zakeleo877.saeedcore.designsystem.SaeedStar
import com.zakeleo877.saeedcore.model.Product
import com.zakeleo877.saeedcore.util.Formatters
import com.zakeleo877.saeedcore.R as CoreR
import com.zakeleo877.saeedstore.R

/** Home and products are distinct views over the same live catalogue and filters. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun CatalogScreen(
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedContentScope,
    state: CustomerUiState,
    isHome: Boolean,
    cartCount: Int,
    query: String,
    onQueryChange: (String) -> Unit,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    offersOnly: Boolean,
    onOffersOnlyChange: (Boolean) -> Unit,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onCartClick: () -> Unit,
    onShowAll: () -> Unit,
    onRetry: () -> Unit,
) {
    val categories = remember(state.products) {
        state.products.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val visible = remember(state.products, query, selectedCategory, offersOnly) {
        filterCatalog(state.products, query, selectedCategory, offersOnly)
    }
    val currency = state.settings?.currency ?: "د.ج"
    val hasFilters = query.isNotBlank() || selectedCategory != null || offersOnly
    val clearFilters = {
        onQueryChange("")
        onCategorySelected(null)
        onOffersOnlyChange(false)
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 136.dp),
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).testTag("catalog-grid"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            CatalogHeader(
                storeName = state.settings?.storeName ?: stringResource(R.string.app_name),
                isHome = isHome,
                cartCount = cartCount,
                onCartClick = onCartClick,
            )
        }
        if (state.offline) {
            item(span = { GridItemSpan(maxLineSpan) }) { OfflineBanner() }
        }
        if (isHome) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(stringResource(R.string.catalog_welcome), style = MaterialTheme.typography.bodyMedium, color = SaeedInkMuted)
                    Text(stringResource(R.string.catalog_welcome_title), style = MaterialTheme.typography.headlineSmall, color = SaeedInk)
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SaeedSearchField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.search_products),
            )
        }
        if (isHome) {
            item(span = { GridItemSpan(maxLineSpan) }) { CatalogFeature(onShowAll) }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(stringResource(R.string.catalog_categories), style = MaterialTheme.typography.titleMedium, color = SaeedInk)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            CategoryFilters(
                categories = categories,
                selectedCategory = selectedCategory,
                offersOnly = offersOnly,
                onCategorySelected = onCategorySelected,
                onOffersOnlyChange = onOffersOnlyChange,
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(if (isHome) R.string.catalog_choices else R.string.catalog_all_products),
                    style = MaterialTheme.typography.titleMedium,
                    color = SaeedInk,
                    modifier = Modifier.weight(1f),
                )
                if (isHome) {
                    TextButton(onClick = onShowAll) { Text(stringResource(R.string.catalog_view_all)) }
                } else if (!state.loadFailed && !state.isRefreshing) {
                    Text(
                        stringResource(R.string.catalog_results_count, visible.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = SaeedInkMuted,
                    )
                }
            }
        }
        when {
            state.isRefreshing && state.products.isEmpty() -> {
                items(6) { CatalogSkeletonCard() }
            }
            state.loadFailed && state.products.isEmpty() -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SaeedEmptyState(stringResource(R.string.catalog_error), icon = Icons.Filled.Inventory2, retry = onRetry)
                }
            }
            visible.isEmpty() -> {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        SaeedEmptyState(
                            message = stringResource(if (hasFilters) R.string.catalog_empty else R.string.catalog_no_products),
                            icon = if (hasFilters) Icons.Filled.Search else Icons.Filled.Inventory2,
                            retry = if (hasFilters) null else onRetry,
                        )
                        if (hasFilters) {
                            SaeedButton(stringResource(R.string.catalog_clear_filters), onClick = clearFilters)
                        }
                    }
                }
            }
            else -> {
                items(visible, key = { it.id }) { product ->
                    with(sharedScope) {
                        ProductCard(
                            animatedScope = animatedScope,
                            product = product,
                            currency = currency,
                            onClick = { onProductClick(product) },
                            onAdd = { onAddToCart(product) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogHeader(storeName: String, isHome: Boolean, cartCount: Int, onCartClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (isHome) {
            SaeedBrandMark()
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = if (isHome) storeName else stringResource(R.string.catalog_products_tab),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SaeedInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.catalog_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = SaeedInkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val cartDescription = stringResource(R.string.catalog_cart_description, cartCount)
        IconButton(onClick = onCartClick, modifier = Modifier.size(48.dp)) {
            BadgedBox(badge = {
                if (cartCount > 0) {
                    Badge(containerColor = SaeedGold, contentColor = SaeedInk) { Text(cartCount.toString()) }
                }
            }) {
                Icon(Icons.Filled.ShoppingCart, contentDescription = cartDescription, tint = SaeedGreen)
            }
        }
    }
}

@Composable
private fun CatalogFeature(onShowAll: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 190.dp).clip(RoundedCornerShape(20.dp)).background(SaeedGreen),
    ) {
        // A neutral botanical background, never a photograph of a fictitious product.
        Image(
            painter = painterResource(R.drawable.catalog_editorial),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        Column(
            modifier = Modifier.fillMaxWidth(0.8f).align(Alignment.CenterStart).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.catalog_feature_label), style = MaterialTheme.typography.labelMedium, color = SaeedGold)
            Text(stringResource(R.string.catalog_feature_title), style = MaterialTheme.typography.titleLarge, color = Color.White)
            Button(
                onClick = onShowAll,
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = SaeedGreen),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            ) { Text(stringResource(R.string.catalog_discover), style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun CategoryFilters(
    categories: List<String>,
    selectedCategory: String?,
    offersOnly: Boolean,
    onCategorySelected: (String?) -> Unit,
    onOffersOnlyChange: (Boolean) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            CategoryChip(stringResource(R.string.catalog_all), selectedCategory == null && !offersOnly) {
                onCategorySelected(null)
                onOffersOnlyChange(false)
            }
        }
        item {
            CategoryChip(stringResource(R.string.catalog_offers), offersOnly) { onOffersOnlyChange(!offersOnly) }
        }
        items(categories, key = { it }) { category ->
            CategoryChip(category, selectedCategory == category) {
                onCategorySelected(if (selectedCategory == category) null else category)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isSelected) SaeedGreen else SaeedChipSurface,
        contentColor = if (isSelected) Color.White else SaeedInk,
        modifier = Modifier.defaultMinSize(minHeight = 48.dp).semantics { role = Role.Button; selected = isSelected },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun OfflineBanner() {
    Surface(color = SaeedGoldSoft, contentColor = SaeedInk, shape = MaterialTheme.shapes.medium) {
        Text(
            text = stringResource(R.string.offline_banner),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SharedTransitionScope.ProductCard(
    animatedScope: AnimatedContentScope,
    product: Product,
    currency: String,
    onClick: () -> Unit,
    onAdd: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = SaeedCardSurface,
        border = BorderStroke(1.dp, SaeedOutline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(1.12f).background(SaeedChipSurface)) {
                RemoteImage(
                    url = product.imageUrl,
                    contentDescription = product.name,
                    size = 180.dp,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().sharedElement(
                        sharedContentState = rememberSharedContentState(key = "product-image-${product.id}"),
                        animatedVisibilityScope = animatedScope,
                    ),
                )
                product.discountPercent?.let { percent ->
                    Surface(
                        color = SaeedDiscountRed,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                    ) {
                        Text(
                            stringResource(R.string.catalog_discount_badge, percent),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                if (!product.available) {
                    Surface(color = SaeedInk.copy(alpha = 0.8f), contentColor = Color.White, modifier = Modifier.fillMaxSize()) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(stringResource(CoreR.string.product_unavailable), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(10.dp)) {
                if (product.category.isNotBlank()) {
                    Text(product.category, style = MaterialTheme.typography.labelSmall, color = SaeedInkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(3.dp))
                }
                Text(product.name, style = MaterialTheme.typography.titleSmall, color = SaeedInk, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                product.rating?.let { stars ->
                    val rating = java.math.BigDecimal.valueOf(stars).setScale(1, java.math.RoundingMode.HALF_UP).toPlainString()
                    val ratingDescription = stringResource(R.string.catalog_rating_description, rating)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics { contentDescription = ratingDescription }) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = SaeedStar, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(rating, style = MaterialTheme.typography.labelSmall, color = SaeedInkMuted)
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(Formatters.money(product.price, currency), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = SaeedGreenDark)
                        val was = product.originalPrice
                        if (product.hasDiscount && was != null) {
                            Text(Formatters.money(was, currency), style = MaterialTheme.typography.labelSmall, color = SaeedInkMuted, textDecoration = TextDecoration.LineThrough)
                        }
                    }
                    Surface(
                        onClick = onAdd,
                        enabled = product.available,
                        shape = RoundedCornerShape(12.dp),
                        color = if (product.available) SaeedGreen else SaeedChipSurface,
                        contentColor = if (product.available) Color.White else SaeedInkMuted,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = stringResource(R.string.catalog_add_named, product.name), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogSkeletonCard() {
    Column {
        SaeedSkeleton(Modifier.fillMaxWidth().aspectRatio(1.12f))
        Spacer(Modifier.height(8.dp))
        SaeedSkeleton(Modifier.fillMaxWidth().height(16.dp))
    }
}
