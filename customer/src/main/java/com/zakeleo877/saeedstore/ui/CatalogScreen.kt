package com.zakeleo877.saeedstore.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.zakeleo877.saeedcore.designsystem.SaeedCard
import com.zakeleo877.saeedcore.designsystem.SaeedEmptyState
import com.zakeleo877.saeedcore.designsystem.SaeedSearchField
import com.zakeleo877.saeedcore.designsystem.SaeedSkeleton
import com.zakeleo877.saeedcore.model.Product
import com.zakeleo877.saeedcore.util.Formatters
import com.zakeleo877.saeedstore.R

/** The home screen: search, category chips and an adaptive product grid. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CatalogScreen(
    sharedScope: SharedTransitionScope,
    animatedScope: AnimatedContentScope,
    state: CustomerUiState,
    cartCount: Int,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit,
    onCartClick: () -> Unit,
    onPendingClick: () -> Unit,
    onRetry: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<String?>(null) }

    val categories = remember(state.products) { state.products.map { it.category }.distinct().sorted() }
    val visible = remember(state.products, query, selectedCategory) {
        state.products.filter { product ->
            val matchesQuery = query.isBlank() ||
                product.name.contains(query, ignoreCase = true) ||
                product.description.contains(query, ignoreCase = true)
            val matchesCategory = selectedCategory == null || product.category == selectedCategory
            matchesQuery && matchesCategory
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        CatalogTopBar(
            storeName = state.settings?.storeName ?: stringResource(R.string.app_name),
            cartCount = cartCount,
            pendingCount = 0,
            onCartClick = onCartClick,
            onPendingClick = onPendingClick,
        )

        if (state.offline) {
            OfflineBanner()
        }

        SaeedSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.search_products),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        if (categories.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = if (selectedCategory == category) null else category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }
        }

        when {
            state.isRefreshing && state.products.isEmpty() -> CatalogSkeleton()
            state.loadFailed -> SaeedEmptyState(
                message = stringResource(R.string.catalog_error),
                icon = Icons.Filled.Inventory2,
                retry = onRetry,
            )
            visible.isEmpty() -> SaeedEmptyState(
                message = stringResource(R.string.catalog_empty),
                icon = Icons.Filled.Search,
                retry = if (state.products.isEmpty()) onRetry else null,
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(visible, key = { it.id }) { product ->
                    with(sharedScope) {
                        ProductCard(
                            animatedScope = animatedScope,
                            product = product,
                            currency = state.settings?.currency ?: "د.ج",
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
private fun CatalogTopBar(
    storeName: String,
    cartCount: Int,
    pendingCount: Int,
    onCartClick: () -> Unit,
    onPendingClick: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = storeName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.catalog_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (pendingCount > 0) {
                IconButton(onClick = onPendingClick) {
                    Badge { Text(pendingCount.toString()) }
                    Icon(Icons.Filled.Inventory2, contentDescription = stringResource(R.string.pending_orders))
                }
            }
            IconButton(onClick = onCartClick) {
                BadgedBox(
                    badge = {
                        if (cartCount > 0) Badge { Text(cartCount.toString()) }
                    },
                ) {
                    Icon(Icons.Filled.ShoppingCart, contentDescription = stringResource(R.string.cart))
                }
            }
        }
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.ProductCard(
    animatedScope: AnimatedContentScope,
    product: Product,
    currency: String,
    onClick: () -> Unit,
    onAdd: () -> Unit,
) {
    SaeedCard(onClick = onClick) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
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
                            state = rememberSharedContentState(key = "product-image-${product.id}"),
                            animatedVisibilityScope = animatedScope,
                        ),
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surfaceContainerHighest,
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }

        Column(Modifier.padding(12.dp)) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = product.category,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Formatters.money(product.price, currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onAdd, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add_to_cart),
                        tint = MaterialTheme.colorScheme.primary,
                    )
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
                        .aspectRatio(1.4f),
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
