package com.zakeleo877.saeedadmin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zakeleo877.saeedcore.designsystem.SaeedBottomSheet
import com.zakeleo877.saeedcore.designsystem.SaeedCard
import com.zakeleo877.saeedcore.designsystem.SaeedEmptyState
import com.zakeleo877.saeedcore.designsystem.SaeedSearchField
import com.zakeleo877.saeedcore.designsystem.SaeedStatusChip
import com.zakeleo877.saeedcore.model.Order
import com.zakeleo877.saeedcore.model.OrderStatus
import com.zakeleo877.saeedcore.util.CsvExporter
import com.zakeleo877.saeedcore.util.Formatters
import com.zakeleo877.saeedadmin.R

/** Order list with status filters, search and the order detail sheet. */
@Composable
fun OrdersScreen(
    state: AdminUiState,
    onFilterChange: (OrderStatus?) -> Unit,
    onSearchChange: (String) -> Unit,
    onStatusUpdate: (String, OrderStatus) -> Unit,
    onRefresh: () -> Unit,
) {
    var selected by remember { mutableStateOf<Order?>(null) }
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.admin_tab_orders),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { exportOrders(context, state.orders) },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Filled.FileDownload,
                    contentDescription = stringResource(R.string.admin_export_orders),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        SaeedSearchField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            placeholder = stringResource(R.string.admin_search_orders),
        )
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = state.statusFilter == null,
                    onClick = { onFilterChange(null) },
                    label = { Text(stringResource(R.string.admin_filter_all)) },
                )
            }
            items(OrderStatus.entries.toList()) { status ->
                FilterChip(
                    selected = state.statusFilter == status,
                    onClick = { onFilterChange(if (state.statusFilter == status) null else status) },
                    label = { Text(status.databaseValue) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        if (state.orders.isEmpty()) {
            SaeedEmptyState(
                message = stringResource(R.string.admin_no_orders),
                icon = Icons.Filled.ReceiptLong,
                retry = onRefresh,
            )
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.orders, key = { it.id }) { order ->
                OrderRow(order = order, onClick = { selected = order })
            }
        }
    }

    selected?.let { order ->
        SaeedBottomSheet(onDismiss = { selected = null }) {
            OrderDetailSheet(
                order = order,
                onStatusChange = { status ->
                    onStatusUpdate(order.id, status)
                    selected = selected?.copy(status = status)
                },
                onClose = { selected = null },
            )
        }
    }
}

/** Writes the order list as a UTF-8 CSV with a byte order mark for Excel. */
private fun exportOrders(context: android.content.Context, orders: List<Order>) {
    val headers = listOf(
        "رقم الفاتورة",
        "التاريخ",
        "الزبون",
        "الهاتف",
        "الحالة",
        "القناة",
        "المجموع",
        "المنتجات",
        "ملاحظات",
    )
    val rows = orders.map { order ->
        listOf(
            order.number.toString(),
            order.createdAt.toString(),
            order.customer,
            order.phone,
            order.status.databaseValue,
            order.channel.databaseValue,
            order.total.toPlainString(),
            order.items.joinToString(" | ") { item ->
                "${item.name} x${item.quantity}"
            },
            order.notes,
        )
    }
    val csv = CsvExporter.build(headers, rows)
    CsvExporter.export(context, "orders.csv", csv)
}

@Composable
private fun OrderRow(order: Order, onClick: () -> Unit) {
    SaeedCard(onClick = onClick) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Formatters.orderNumber(order.number),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.weight(1f))
                SaeedStatusChip(status = order.status)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = order.customer.ifBlank { stringResource(R.string.admin_no_name) },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = order.phone,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = Formatters.money(order.total, "د.ج"),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = Formatters.dateTime(order.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
