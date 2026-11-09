package com.zakeleo877.saeedadmin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zakeleo877.saeedadmin.R
import com.zakeleo877.saeedcore.designsystem.SaeedBottomSheet
import com.zakeleo877.saeedcore.designsystem.SaeedBrandMark
import com.zakeleo877.saeedcore.designsystem.SaeedCard
import com.zakeleo877.saeedcore.designsystem.SaeedChipSurface
import com.zakeleo877.saeedcore.designsystem.SaeedGold
import com.zakeleo877.saeedcore.designsystem.SaeedGreen
import com.zakeleo877.saeedcore.designsystem.SaeedGreenDark
import com.zakeleo877.saeedcore.designsystem.SaeedInk
import com.zakeleo877.saeedcore.designsystem.SaeedInkMuted
import com.zakeleo877.saeedcore.model.OrderStatus
import com.zakeleo877.saeedcore.util.Formatters

/** Overview of the unfiltered, locally loaded page; it makes no global-total claim. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: AdminUiState,
    storeName: String,
    currency: String,
    onRefresh: () -> Unit,
    onOrdersClick: () -> Unit,
    onProductsClick: () -> Unit,
    onStatusUpdate: (String, OrderStatus, (Boolean) -> Unit) -> Unit,
) {
    val stats = state.dashboardStats
    val ordersAvailable = state.ordersLoaded
    val productsAvailable = state.productsLoaded
    var selectedOrderId by remember { mutableStateOf<String?>(null) }
    var statusUpdating by remember { mutableStateOf(false) }
    var statusFailed by remember { mutableStateOf(false) }
    LaunchedEffect(selectedOrderId) { statusFailed = false }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("admin-dashboard"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SaeedBrandMark()
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(storeName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SaeedInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.admin_title), style = MaterialTheme.typography.bodySmall, color = SaeedInkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onRefresh, enabled = !state.isLoading, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.admin_refresh), tint = SaeedGreen)
                }
            }
        }
        item {
            Column {
                Text(stringResource(R.string.admin_dashboard_welcome), style = MaterialTheme.typography.bodyMedium, color = SaeedInkMuted)
                Text(stringResource(R.string.admin_dashboard_title), style = MaterialTheme.typography.headlineSmall, color = SaeedInk)
            }
        }
        if (state.ordersLoadFailed) {
            item {
                Text(stringResource(R.string.admin_orders_stale), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        }
        if (state.productsLoadFailed) {
            item {
                Text(stringResource(R.string.admin_products_stale), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
        }
        item {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(SaeedGreen, SaeedGreenDark)))
                    .padding(20.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = SaeedGold, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.admin_stat_new), style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                    Text(if (ordersAvailable) stats.newOrders.toString() else "—", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                    Text(stringResource(R.string.admin_dashboard_new_hint), style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
            }
        }
        item {
            Text(stringResource(R.string.admin_dashboard_scope), style = MaterialTheme.typography.bodySmall, color = SaeedInkMuted)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardStat(
                    label = stringResource(R.string.admin_stat_total),
                    value = if (ordersAvailable) stats.totalOrders.toString() else "—",
                    modifier = Modifier.weight(1f),
                )
                DashboardStat(
                    label = stringResource(R.string.admin_stat_today),
                    value = if (ordersAvailable) stats.todayOrders.toString() else "—",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardStat(
                    label = stringResource(R.string.admin_stat_products),
                    value = if (productsAvailable) stats.productCount.toString() else "—",
                    modifier = Modifier.weight(1f),
                )
                DashboardStat(
                    label = stringResource(R.string.admin_stat_sales),
                    value = if (ordersAvailable) Formatters.money(stats.handedOverTotal, currency) else "—",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.admin_dashboard_recent), style = MaterialTheme.typography.titleMedium, color = SaeedInk, modifier = Modifier.weight(1f))
                TextButton(onClick = onOrdersClick) { Text(stringResource(R.string.admin_dashboard_view_all)) }
            }
        }
        if (state.allOrders.isEmpty()) {
            item {
                Text(
                    text = stringResource(
                        when {
                            state.ordersLoadFailed && !ordersAvailable -> R.string.admin_orders_load_failed
                            !ordersAvailable -> R.string.admin_orders_loading
                            else -> R.string.admin_no_orders
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SaeedInkMuted,
                )
            }
        } else {
            items(state.allOrders.take(3), key = { it.id }) { order ->
                OrderRow(order = order, onClick = { selectedOrderId = order.id }, currency = currency)
            }
        }
        item {
            Surface(
                onClick = onProductsClick,
                color = SaeedChipSurface,
                contentColor = SaeedInk,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Inventory2, contentDescription = null, tint = SaeedGreen, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.admin_dashboard_products), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.admin_dashboard_products_hint), style = MaterialTheme.typography.bodySmall, color = SaeedInkMuted)
                    }
                }
            }
        }
    }

    val selectedOrder = state.allOrders.firstOrNull { it.id == selectedOrderId }
    if (selectedOrder != null) {
        SaeedBottomSheet(onDismiss = { selectedOrderId = null }) {
            if (statusFailed) {
                Text(
                    stringResource(R.string.admin_status_update_failed),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            OrderDetailSheet(
                order = selectedOrder,
                currency = currency,
                statusUpdating = statusUpdating,
                onStatusChange = { next ->
                    if (!statusUpdating) {
                        statusUpdating = true
                        statusFailed = false
                        val orderId = selectedOrder.id
                        onStatusUpdate(orderId, next) { succeeded ->
                            statusUpdating = false
                            if (selectedOrderId == orderId) statusFailed = !succeeded
                        }
                    }
                },
                onClose = { selectedOrderId = null },
            )
        }
    }
}

@Composable
private fun DashboardStat(label: String, value: String, modifier: Modifier = Modifier) {
    SaeedCard(modifier = modifier) {
        Column(
            Modifier.fillMaxWidth().defaultMinSize(minHeight = 104.dp).padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = SaeedInkMuted)
            Spacer(Modifier.height(10.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, color = SaeedInk, fontWeight = FontWeight.Bold)
        }
    }
}
