package com.zakeleo877.saeedstore.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.zakeleo877.saeedcore.designsystem.SaeedCardSurface
import com.zakeleo877.saeedcore.designsystem.SaeedChipSurface
import com.zakeleo877.saeedcore.designsystem.SaeedDiscountRed
import com.zakeleo877.saeedcore.designsystem.SaeedEmptyState
import com.zakeleo877.saeedcore.designsystem.SaeedGreen
import com.zakeleo877.saeedcore.designsystem.SaeedGreenDark
import com.zakeleo877.saeedcore.designsystem.SaeedGreenHeader
import com.zakeleo877.saeedcore.designsystem.SaeedInk
import com.zakeleo877.saeedcore.designsystem.SaeedInkMuted
import com.zakeleo877.saeedcore.designsystem.SaeedSearchField
import com.zakeleo877.saeedcore.designsystem.SaeedSkeleton
import com.zakeleo877.saeedcore.designsystem.SaeedStar
import com.zakeleo877.saeedcore.model.Product
import com.zakeleo877.saeedcore.util.Formatters
import com.zakeleo877.saeedcore.R as CoreR
import com.zakeleo877.saeedstore.R

/**
 * The home screen: a green header, search, category chips and a product grid.
 *
 * [ExperimentalMaterial3Api] is needed because the clickable [Surface] overload
 * is still marked experimental.
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedContentScope,
    state: CustomerUiState,
    cartCount: Int,
    pendingCount: Int,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onCartClick: () -> Unit,
    onPendingClick: () -> Unit,
    onRetry: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var offersOnly by rememberSaveable { mutableStateOf(false) }

    val categories = remember(state.products) { state.products.map { it.category }.distinct().sorted() }
    val visible = remember(state.products, query, selectedCategory, offersOnly) {
        state.products.filter { product ->
            val matchesQuery = query.isBlank() ||
                product.name.contains(query, ignoreCase = true) ||
                product.description.contains(query, ignoreCase = true)
            val matchesCategory = selectedCategory == null || product.category == selectedCategory
            val matchesOffers = !offersOnly || product.hasDiscount
            matchesQuery && matchesCategory && matchesOffers
        }
    }
    val currency = state.settings?.currency ?: "د.ج"

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
            CatalogHeader(
                storeName = state.settings?.storeName ?: stringResource(R.string.app_name),
                cartCount = cartCount,
                pendingCount = pendingCount,
                onCartClick = onCartClick,
                onPendingClick = onPendingClick,
            )

            // The search field overlaps the header, the way the design does it:
            // it is pulled up by half its height so the header reads as one
            // block rather than a stack of bars.
            SearchAndFilters(
                query = query,
                onQueryChange = { query = it },
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                offersOnly = offersOnly,
                onOffersOnly = { offersOnly = !offersOnly },
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    // Lifted onto the header so the two read as one block
                    // instead of a green band followed by a floating field.
                    .offset(y = (-22).dp),
            )
            Spacer(Modifier.height(2.dp))

            if (state.offline) OfflineBanner()

            when {
                state.isRefreshing && state.products.isEmpty() -> CatalogSkeleton()
                state.loadFailed -> SaeedEmptyState(
                    message = stringResource(R.string.catalog_error),
                    icon = Icons.Filled.Inventory2,
                    retry = onRetry,
                )
                visible.isEmpty() -> {
                    // An empty shelf has two very different causes, and the
                    // button must fix the actual one: an empty catalogue needs a
                    // reload, while a filter that matches nothing needs clearing.
                    val hasFilters = query.isNotBlank() || selectedCategory != null || offersOnly
                    val clearFilters: (() -> Unit)? = if (!state.products.isEmpty() || hasFilters) {
                        {
                            query = ""
                            selectedCategory = null
                            offersOnly = false
                        }
                    } else {
                        null
                    }
                    val action: (() -> Unit)? = if (state.products.isEmpty()) onRetry else clearFilters
                    SaeedEmptyState(
                        message = stringResource(R.string.catalog_empty),
                        icon = Icons.Filled.Search,
                        retry = action,
                    )
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        // Room for the floating bar, so the last row is reachable.
                        bottom = 104.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
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
private fun CatalogHeader(
    storeName: String,
    cartCount: Int,
    pendingCount: Int,
    onCartClick: () -> Unit,
    onPendingClick: () -> Unit,
) {
    // The status bar is drawn over this header (the activity is edge to edge),
    // so the content is pushed down by its height and dark icons are used.
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(SaeedGreenHeader, SaeedGreen)))
            .statusBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.catalog_greeting),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Text(
                    text = storeName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            DetailAction(
                icon = Icons.Filled.ReceiptLong,
                description = stringResource(R.string.pending_orders),
                badgeCount = pendingCount,
                onClick = onPendingClick,
            )
            DetailAction(
                icon = Icons.Filled.ShoppingCart,
                description = stringResource(R.string.cart),
                badgeCount = cartCount,
                onClick = onCartClick,
            )
        }
    }
}

/** A round translucent button on the header, with an optional count badge. */
@Composable
private fun DetailAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    badgeCount: Int,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        BadgedBox(
            badge = {
                if (badgeCount > 0) {
                    Badge(containerColor = SaeedDiscountRed, contentColor = Color.White) {
                        Text(badgeCount.toString())
                    }
                }
            },
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = description, tint = Color.White)
            }
        }
    }
}

@Composable
private fun SearchAndFilters(
    query: String,
    onQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    offersOnly: Boolean,
    onOffersOnly: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        SaeedSearchField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = stringResource(R.string.search_products),
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                CategoryChip(
                    label = stringResource(R.string.catalog_all),
                    isSelected = selectedCategory == null && !offersOnly,
                    onClick = {
                        onCategorySelected(null)
                        if (offersOnly) onOffersOnly()
                    },
                )
            }
            item {
                CategoryChip(
                    label = stringResource(R.string.catalog_offers),
                    isSelected = offersOnly,
                    onClick = onOffersOnly,
                )
            }
            items(categories) { category ->
                CategoryChip(
                    label = category,
                    isSelected = selectedCategory == category,
                    onClick = {
                        onCategorySelected(if (selectedCategory == category) null else category)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    val background = if (isSelected) SaeedGreen else SaeedChipSurface
    val content = if (isSelected) Color.White else SaeedInk
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = background,
        contentColor = content,
        // The chip behaves as a toggle, so it is announced as one instead of
        // repeating its own label as a separate description.
        modifier = Modifier.semantics {
            role = Role.Button
            selected = isSelected
        },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun OfflineBanner() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Text(
            text = stringResource(R.string.offline_banner),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
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
        shape = RoundedCornerShape(18.dp),
        color = SaeedCardSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                        .background(SaeedChipSurface),
                    contentAlignment = Alignment.Center,
                ) {
                    if (product.imageUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(product.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(key = "product-image-${product.id}"),
                                    animatedVisibilityScope = animatedScope,
                                ),
                        )
                    } else {
                        Icon(
                            Icons.Filled.Inventory2,
                            contentDescription = null,
                            tint = SaeedInkMuted,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }

                // The badge only appears when a discount really exists, and it
                // carries the same number the card shows, never a rounded guess.
                product.discountPercent?.let { percent ->
                    Surface(
                        color = SaeedDiscountRed,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.catalog_discount_badge, percent),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }

                if (!product.available) {
                    Surface(
                        color = SaeedInk.copy(alpha = 0.72f),
                        contentColor = Color.White,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(CoreR.string.product_unavailable),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }

            Column(Modifier.padding(10.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = SaeedInk,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))

                product.rating?.let { stars ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = SaeedStar,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = stringResource(
                                R.string.catalog_rating,
                                java.math.BigDecimal.valueOf(stars)
                                    .setScale(1, java.math.RoundingMode.HALF_UP)
                                    .toPlainString(),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = SaeedInkMuted,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = Formatters.money(product.price, currency),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = SaeedGreenDark,
                        )
                        // Read once into a local: hasDiscount and originalPrice
                        // are separate reads of the same value, and only the
                        // local is certainly non-null here.
                        val was = product.originalPrice
                        if (product.hasDiscount && was != null) {
                            Text(
                                text = Formatters.money(was, currency),
                                style = MaterialTheme.typography.labelSmall,
                                color = SaeedInkMuted,
                                textDecoration = TextDecoration.LineThrough,
                            )
                        }
                    }
                    // A cart, not a plus: a plus is ambiguous between "add" and
                    // "expand", and the design asks for an explicit cart.
                    Surface(
                        onClick = onAdd,
                        enabled = product.available,
                        shape = CircleShape,
                        color = if (product.available) SaeedGreen else SaeedChipSurface,
                        contentColor = if (product.available) Color.White else SaeedInkMuted,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.ShoppingCart,
                                contentDescription = stringResource(R.string.add_to_cart),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogSkeleton() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false,
    ) {
        items(6) {
            Column {
                SaeedSkeleton(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.2f),
                )
                Spacer(Modifier.height(8.dp))
                SaeedSkeleton(
                    Modifier
                        .fillMaxWidth()
                        .height(16.dp),
                )
            }
        }
    }
}
