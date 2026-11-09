package com.zakeleo877.saeedstore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zakeleo877.saeedcore.designsystem.RemoteImage
import com.zakeleo877.saeedcore.designsystem.SaeedChipSurface
import com.zakeleo877.saeedcore.designsystem.SaeedButton
import com.zakeleo877.saeedcore.designsystem.SaeedCard
import com.zakeleo877.saeedcore.designsystem.SaeedEmptyState
import com.zakeleo877.saeedcore.model.CartItem
import com.zakeleo877.saeedcore.util.Formatters
import com.zakeleo877.saeedstore.R
import java.math.BigDecimal

/** The cart screen: quantity editing, swipe-to-remove and the running total. */
@Composable
fun CartScreen(
    items: List<CartItem>,
    currency: String,
    onQuantityChange: (String, Int) -> Unit,
    onRemove: (String) -> Unit,
    onCheckout: () -> Unit,
) {
    val total: BigDecimal = items.fold(BigDecimal.ZERO) { acc, item -> acc + item.estimatedTotal }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.cart_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.cart_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        if (items.isEmpty()) {
            SaeedEmptyState(
                message = stringResource(R.string.cart_empty),
                icon = Icons.Filled.ShoppingCart,
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.product.id }) { item ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value != SwipeToDismissBoxValue.Settled) {
                            onRemove(item.product.id)
                        }
                        // The item leaves the list, so the swipe is allowed to
                        // finish instead of animating back.
                        true
                    },
                )
                SwipeToDismissBox(
                    state = dismissState,
                    // Both directions are allowed so the gesture works the same
                    // way in the Arabic right-to-left layout.
                    enableDismissFromStartToEnd = true,
                    enableDismissFromEndToStart = true,
                    backgroundContent = { CartDismissBackground() },
                ) {
                    CartRow(
                        item = item,
                        currency = currency,
                        onQuantityChange = { onQuantityChange(item.product.id, it) },
                        onRemove = { onRemove(item.product.id) },
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Surface(
            color = SaeedChipSurface,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.cart_total),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = Formatters.money(total, currency),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        SaeedButton(
            text = stringResource(R.string.cart_checkout),
            onClick = onCheckout,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CartDismissBackground() {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            Icons.Filled.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun CartRow(
    item: CartItem,
    currency: String,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    SaeedCard {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RemoteImage(
                    url = item.product.imageUrl,
                    contentDescription = item.product.name,
                    size = 72.dp,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(SaeedChipSurface),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = item.product.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = Formatters.money(item.product.price, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.pending_delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onQuantityChange(item.quantity - 1) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.decrease))
                }
                Text(item.quantity.toString(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                IconButton(onClick = { onQuantityChange(item.quantity + 1) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.increase))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = Formatters.money(item.estimatedTotal, currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
