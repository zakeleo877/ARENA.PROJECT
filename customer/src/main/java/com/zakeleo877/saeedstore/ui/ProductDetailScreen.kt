package com.zakeleo877.saeedstore.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.zakeleo877.saeedcore.designsystem.SaeedButton
import com.zakeleo877.saeedcore.model.Product
import com.zakeleo877.saeedcore.util.Formatters
import com.zakeleo877.saeedstore.R

/** Product details with a quantity stepper and an add-to-cart action. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.ProductDetailScreen(
    animatedScope: AnimatedContentScope,
    product: Product,
    currency: String,
    onBack: () -> Unit,
    onAddToCart: (Int) -> Unit,
) {
    var quantity by remember(product.id) { mutableIntStateOf(1) }
    var zoomOpen by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.product_details))
            }
            Text(
                text = stringResource(R.string.product_details),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
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
                        .fillMaxWidth()
                        .height(280.dp)
                        .sharedElement(
                            sharedContentState = rememberSharedContentState(key = "product-image-${product.id}"),
                            animatedVisibilityScope = animatedScope,
                        )
                        .clickable { zoomOpen = true },
                )
            }

            Column(Modifier.padding(16.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = product.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = Formatters.money(product.price, currency),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.product_description),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = product.description.ifBlank { stringResource(R.string.product_no_description) },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 10,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { if (quantity > 1) quantity-- },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.decrease))
            }
            Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(
                onClick = { if (quantity < 99) quantity++ },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.increase))
            }
            Spacer(Modifier.size(12.dp))
            SaeedButton(
                text = stringResource(R.string.add_to_cart),
                onClick = { onAddToCart(quantity) },
                icon = Icons.Filled.Add,
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (zoomOpen && product.imageUrl.isNotBlank()) {
        ProductImageDialog(
            imageUrl = product.imageUrl,
            contentDescription = stringResource(R.string.product_image_viewer),
            onDismiss = { zoomOpen = false },
        )
    }
}
