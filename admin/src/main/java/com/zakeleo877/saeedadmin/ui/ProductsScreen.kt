package com.zakeleo877.saeedadmin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.zakeleo877.saeedcore.designsystem.SaeedBottomSheet
import com.zakeleo877.saeedcore.designsystem.SaeedButton
import com.zakeleo877.saeedcore.designsystem.SaeedCard
import com.zakeleo877.saeedcore.designsystem.SaeedEmptyState
import com.zakeleo877.saeedcore.designsystem.SaeedTextField
import com.zakeleo877.saeedcore.model.Product
import com.zakeleo877.saeedcore.util.CsvExporter
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
    onUploadImage: (productId: String?, fileName: String, bytes: ByteArray, onDone: (String?) -> Unit) -> Unit,
) {
    var editing by remember { mutableStateOf<Product?>(null) }
    var creating by remember { mutableStateOf(false) }
    val context = LocalContext.current

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
            IconButton(
                onClick = { exportProducts(context, products) },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Filled.FileDownload,
                    contentDescription = stringResource(R.string.admin_export_products),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
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
                onUploadImage = { fileName, bytes, onDone ->
                    onUploadImage(null, fileName, bytes, onDone)
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
                onUploadImage = { fileName, bytes, onDone ->
                    onUploadImage(product.id, fileName, bytes, onDone)
                },
                onDismiss = { editing = null },
            )
        }
    }
}

/** Writes the product list as a UTF-8 CSV with a byte order mark for Excel. */
private fun exportProducts(context: android.content.Context, products: List<Product>) {
    val headers = listOf(
        "الاسم",
        "السعر",
        "الفئة",
        "الوصف",
        "رابط الصورة",
        "متاح",
        "الترتيب",
    )
    val rows = products.map { product ->
        listOf(
            product.name,
            product.price.toPlainString(),
            product.category,
            product.description,
            product.imageUrl,
            if (product.available) "نعم" else "لا",
            product.sortOrder.toString(),
        )
    }
    val csv = CsvExporter.build(headers, rows)
    CsvExporter.export(context, "products.csv", csv)
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
            if (product.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.admin_image_preview),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.size(12.dp))
            }
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
                    contentDescription = stringResource(R.string.admin_delete),
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
    onUploadImage: (fileName: String, bytes: ByteArray, onDone: (String?) -> Unit) -> Unit,
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
    var uploading by remember { mutableStateOf(false) }
    var uploadFailed by remember { mutableStateOf(false) }

    // The form is taller than the sheet on a small phone once the image preview
    // is in place, so it scrolls as a whole; otherwise the save button sits
    // below the visible area and can never be pressed.
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
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
        ProductImagePicker(
            imageUrl = imageUrl,
            uploading = uploading,
            uploadError = uploadFailed,
            onLinkChange = { imageUrl = it },
            onUpload = { fileName, bytes ->
                uploading = true
                uploadFailed = false
                onUploadImage(fileName, bytes) { url ->
                    uploading = false
                    if (url != null) {
                        imageUrl = url
                    } else {
                        uploadFailed = true
                    }
                }
            },
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
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.admin_move_up))
            }
            Text(
                text = sortOrder.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(onClick = { sortOrder -= 1 }) {
                Icon(Icons.Filled.Inventory2, contentDescription = stringResource(R.string.admin_move_down))
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
