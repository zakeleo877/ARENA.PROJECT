package com.zakeleo877.saeedstore.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zakeleo877.saeedcore.data.PendingOrderRow
import com.zakeleo877.saeedcore.data.PendingState
import com.zakeleo877.saeedcore.designsystem.SaeedButton
import com.zakeleo877.saeedcore.designsystem.SaeedCard
import com.zakeleo877.saeedcore.designsystem.SaeedEmptyState
import com.zakeleo877.saeedcore.designsystem.SaeedGoldInk
import com.zakeleo877.saeedcore.designsystem.SaeedGoldSoft
import com.zakeleo877.saeedstore.R

/** Lists queued and failed orders so the customer can retry, edit or delete them. */
@Composable
fun PendingOrdersScreen(
    orders: List<PendingOrderRow>,
    onRetry: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.pending_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.pending_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        if (orders.isEmpty()) {
            SaeedEmptyState(
                message = stringResource(R.string.pending_empty),
                icon = Icons.Filled.HourglassEmpty,
            )
            return@Column
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(orders, key = { it.clientId }) { order ->
                PendingOrderCard(order = order, onRetry = { onRetry(order.clientId) }, onDelete = { onDelete(order.clientId) })
            }
        }
    }
}

@Composable
private fun PendingOrderCard(
    order: PendingOrderRow,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
) {
    SaeedCard {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = order.customer,
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
                }
                PendingStateChip(order.state)
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = order.items.joinToString("، ") { "${it.quantity}×" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val lastError = order.lastError
            if (lastError != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = errorMessage(lastError),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.pending_delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                SaeedButton(text = stringResource(R.string.pending_retry_now), onClick = onRetry, icon = Icons.Filled.Refresh)
            }
        }
    }
}

@Composable
private fun PendingStateChip(state: PendingState) {
    val label = when (state) {
        PendingState.WAITING -> stringResource(R.string.pending_waiting)
        PendingState.SENDING -> stringResource(R.string.pending_sending)
        PendingState.FAILED -> stringResource(R.string.pending_failed)
    }
    val failed = state == PendingState.FAILED
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (failed) MaterialTheme.colorScheme.errorContainer else SaeedGoldSoft,
        contentColor = if (failed) MaterialTheme.colorScheme.onErrorContainer else SaeedGoldInk,
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (failed) Icons.Filled.ErrorOutline else Icons.Filled.HourglassEmpty, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun errorMessage(code: String): String = when (code) {
    "network_error" -> stringResource(R.string.pending_error_network)
    "server_error", "request_failed" -> stringResource(R.string.pending_error_server)
    "invalid_order", "invalid_request" -> stringResource(R.string.pending_error_invalid)
    "rate_limited" -> stringResource(R.string.pending_error_rate)
    else -> stringResource(R.string.generic_error)
}
