package com.zakeleo877.saeedadmin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zakeleo877.saeedcore.designsystem.SaeedButton
import com.zakeleo877.saeedcore.designsystem.SaeedCard
import com.zakeleo877.saeedcore.util.Formatters
import com.zakeleo877.saeedadmin.R

/** Live overview of the store: order counters and handed-over sales. */
@Composable
fun DashboardScreen(
    stats: DashboardStats,
    onRefresh: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.admin_tab_dashboard),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            SaeedButton(
                text = stringResource(R.string.action_retry),
                onClick = onRefresh,
                icon = Icons.Filled.Refresh,
            )
        }
        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                StatCard(
                    label = stringResource(R.string.admin_stat_new),
                    value = stats.newOrders.toString(),
                    icon = Icons.Filled.ReceiptLong,
                )
            }
            item {
                StatCard(
                    label = stringResource(R.string.admin_stat_today),
                    value = stats.todayOrders.toString(),
                    icon = Icons.Filled.ReceiptLong,
                )
            }
            item {
                StatCard(
                    label = stringResource(R.string.admin_stat_total),
                    value = stats.totalOrders.toString(),
                    icon = Icons.Filled.ReceiptLong,
                )
            }
            item {
                StatCard(
                    label = stringResource(R.string.admin_stat_products),
                    value = stats.productCount.toString(),
                    icon = Icons.Filled.Inventory2,
                )
            }
            item {
                StatCard(
                    label = stringResource(R.string.admin_stat_sales),
                    value = Formatters.money(stats.handedOverTotal, "د.ج"),
                    icon = Icons.Filled.ReceiptLong,
                )
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector) {
    SaeedCard {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(24.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
