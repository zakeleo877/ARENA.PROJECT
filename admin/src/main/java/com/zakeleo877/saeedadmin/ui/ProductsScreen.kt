package com.zakeleo877.saeedadmin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zakeleo877.saeedcore.designsystem.SaeedBottomSheet
import com.zakeleo877.saeedcore.designsystem.SaeedButton
import com.zakeleo877.saeedcore.designsystem.SaeedCard
import com.zakeleo877.saeedcore.designsystem.SaeedEmptyState
import com.zakeleo877.saeedcore.designsystem.SaeedTextField
import com.zakeleo877.saeedcore.model.Product
import com.zakeleo877.saeedcore.util.Formatters
import com.zakeleo877.saeedadmin.R
import java.math.BigDecimal

/** Product management: list, add and edit products including the availability switch. */
@Composable
fun ProductsScreen(
    products: List<Product>,
    onSave: (
        id: String?,
        name: String,
        price: BigDecimal,
        description: String,
        imageUrl: String,
        category: String,
        available: Boolean,
        sortOrder: Int,
    ) -> Unit,
    onDelete: (String) -> Unit,
    onUploadImage: (String, ByteArray) -> Unit,
) {
    var editing by remember { mutableStateOf<Product?>(null) }
    var creating by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.admin_tab_products),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            SaeedButton(
                text = stringResource(R.string.admin_add_product),
                onClick = { creating = true },
                icon = Icons.Filled.Add,
            )
        }
        Spacer(Modifier.height(16.dp))

        if (products.isEmpty()) {
            SaeedEmptyState(
                message = stringResource(R.string.admin_no_products),
                icon = Icons.Filled.Inventory2,
            )
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(products, key = { it.id }) { product ->
                ProductRow(
                    product = product,
                    onEdit = { editing = product },
                    onDelete = { onDelete(product.id) },
                )
            }
        }
    }

    if (creating) {
        SaeedBottomSheet(onDismiss = { creating = false }) {
            ProductFormSheet(
                product = null,
                categories = products.map { it.category }.distinct().sorted(),
                onSave = { name, price, description, imageUrl, category, available, sortOrder ->
                    onSave(null, name, price, description, imageUrl, category, available, sortOrder)
                    creating = false
                },
                onDismiss = { creating = false },
            )
        }
    }

    editing?.let { product ->
        SaeedBottomSheet(onDismiss = { editing = null }) {
            ProductFormSheet(
                product = product,
                categories = products.map { it.category }.distinct().sorted(),
                onSave = { name, price, description, imageUrl, category, available, sortOrder ->
                    onSave(product.id, name, price, description, imageUrl, category, available, sortOrder)
                    editing = null
                },
                onDismiss = { editing = null },
            )
        }
    }
}

@Composable
private fun ProductRow(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    SaeedCard {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${product.category} · ${Formatters.money(product.price, "د.ج")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (product.available) stringResource(R.string.admin_available)
                    else stringResource(R.string.admin_hidden),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (product.available) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.height(48.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.admin_edit))
            }
            IconButton(onClick = onDelete, modifier = Modifier.height(48.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** Add or edit a product. */
@Composable
private fun ProductFormSheet(
    product: Product?,
    categories: List<String>,
    onSave: (
        name: String,
        price: BigDecimal,
        description: String,
        imageUrl: String,
        category: String,
        available: Boolean,
        sortOrder: Int,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(product?.name.orEmpty()) }
    var price by remember { mutableStateOf(product?.price?.toPlainString().orEmpty()) }
    var description by remember { mutableStateOf(product?.description.orEmpty()) }
    var imageUrl by remember { mutableStateOf(product?.imageUrl.orEmpty()) }
    var category by remember { mutableStateOf(product?.category ?: categories.firstOrNull().orEmpty()) }
    var available by remember { mutableStateOf(product?.available ?: true) }
    var sortOrder by remember { mutableStateOf(product?.sortOrder ?: 0) }
    var priceError by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.admin_product_form_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        SaeedTextField(
            value = name,
            onValueChange = { name = it },
            label = stringResource(R.string.admin_product_name),
        )
        SaeedTextField(
            value = price,
            onValueChange = { price = it; priceError = false },
            label = stringResource(R.string.admin_product_price),
            isError = priceError,
            supportingText = if (priceError) stringResource(R.string.admin_price_error) else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        SaeedTextField(
            value = description,
            onValueChange = { description = it },
            label = stringResource(R.string.admin_product_description),
            singleLine = false,
        )
        SaeedTextField(
            value = category,
            onValueChange = { category = it },
            label = stringResource(R.string.admin_product_category),
        )
        SaeedTextField(
            value = imageUrl,
            onValueChange = { imageUrl = it },
            label = stringResource(R.string.admin_product_image),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.admin_product_available),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = available, onCheckedChange = { available = it })
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.admin_product_order),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { sortOrder += 1 }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_increase))
            }
            Text(
                text = sortOrder.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(onClick = { sortOrder -= 1 }) {
                Icon(Icons.Filled.Inventory2, contentDescription = stringResource(R.string.action_decrease))
            }
        }

        SaeedButton(
            text = stringResource(R.string.admin_save),
            onClick = {
                val parsed = price.toBigDecimalOrNull()
                if (parsed == null || parsed.signum() < 0) {
                    priceError = true
                } else {
                    onSave(name.trim(), parsed, description.trim(), imageUrl.trim(), category.trim(), available, sortOrder)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
