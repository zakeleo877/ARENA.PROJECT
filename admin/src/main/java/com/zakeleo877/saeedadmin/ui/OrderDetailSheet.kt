package com.zakeleo877.saeedadmin.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zakeleo877.saeedcore.designsystem.SaeedButton
import com.zakeleo877.saeedcore.model.Order
import com.zakeleo877.saeedcore.model.OrderStatus
import com.zakeleo877.saeedcore.util.Formatters
import com.zakeleo877.saeedadmin.R

/** Order details inside the bottom sheet, including the status flow. */
@Composable
fun OrderDetailSheet(
    order: Order,
    onStatusChange: (OrderStatus) -> Unit,
    onClose: () -> Unit,
    statusUpdating: Boolean = false,
    currency: String = "د.ج",
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.admin_order_number, Formatters.orderNumber(order.number)),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.admin_order_date, Formatters.dateTime(order.createdAt)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))
        DetailRow(stringResource(R.string.admin_customer), order.customer)
        DetailRow(stringResource(R.string.admin_phone), order.phone)
        if (order.notes.isNotBlank()) {
            DetailRow(stringResource(R.string.admin_notes), order.notes)
        }
        if (order.customerLocationUrl.isNotBlank()) {
            DetailRow(stringResource(R.string.admin_location), order.customerLocationUrl)
        }
        DetailRow(stringResource(R.string.admin_channel), order.channel.databaseValue)

        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.admin_order_items),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        order.items.forEach { item ->
            Row(Modifier.fillMaxWidth()) {
                Text(
                    text = "${item.name} ×${item.quantity}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = Formatters.money(item.price * item.quantity.toBigDecimal(), currency),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.admin_total),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = Formatters.money(order.total, currency),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.admin_change_status),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val nextStatuses = nextStatuses(order.status)
        nextStatuses.forEach { status ->
            SaeedButton(
                text = status.databaseValue,
                onClick = { onStatusChange(status) },
                enabled = !statusUpdating,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** The allowed transitions: new → preparing → ready → handed over, cancel from any open state. */
fun nextStatuses(current: OrderStatus): List<OrderStatus> = when (current) {
    OrderStatus.NEW -> listOf(OrderStatus.PREPARING, OrderStatus.CANCELLED)
    OrderStatus.PREPARING -> listOf(OrderStatus.READY, OrderStatus.CANCELLED)
    OrderStatus.READY -> listOf(OrderStatus.HANDED_OVER, OrderStatus.CANCELLED)
    OrderStatus.HANDED_OVER, OrderStatus.CANCELLED -> emptyList()
}
