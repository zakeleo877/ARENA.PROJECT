package com.zakeleo877.saeedstore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zakeleo877.saeedcore.designsystem.SaeedGreen
import com.zakeleo877.saeedcore.designsystem.SaeedGreenDark
import com.zakeleo877.saeedstore.R

/** The three screens the customer app really has. */
internal enum class SaeedTab { CATALOG, CART, PENDING }

/**
 * The floating bar: three destinations, because the app has three screens.
 *
 * The design showed five, including favourites and an account page that do not
 * exist here; a tab that leads nowhere is worse than no tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SaeedBottomBar(
    cartCount: Int,
    pendingCount: Int,
    onCatalogClick: () -> Unit,
    onCartClick: () -> Unit,
    onPendingClick: () -> Unit,
    current: SaeedTab,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SaeedGreen,
        contentColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 12.dp,
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 32.dp, vertical = 12.dp)
            .fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The catalogue is the screen being shown, so this tab is not
            // clickable: tapping it can do nothing, and pretending otherwise
            // would be a lie to the user and to a screen reader.
            BarItem(
                icon = Icons.Filled.Dashboard,
                label = stringResource(R.string.catalog_all),
                selected = current == SaeedTab.CATALOG,
                badgeCount = 0,
                onClick = if (current == SaeedTab.CATALOG) null else onCatalogClick,
            )
            BarItem(
                icon = Icons.Filled.ShoppingCart,
                label = stringResource(R.string.cart),
                selected = current == SaeedTab.CART,
                badgeCount = cartCount,
                onClick = onCartClick,
            )
            BarItem(
                icon = Icons.Filled.ReceiptLong,
                label = stringResource(R.string.catalog_pending_tab),
                selected = current == SaeedTab.PENDING,
                badgeCount = pendingCount,
                onClick = onPendingClick,
            )
        }
    }
}

@Composable
private fun BarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    badgeCount: Int,
    onClick: (() -> Unit)?,
) {
    val alpha = if (selected) 1f else 0.72f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClickLabel = label, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        BadgedBox(
            badge = {
                if (badgeCount > 0) {
                    Badge(containerColor = Color.White, contentColor = SaeedGreenDark) {
                        Text(badgeCount.toString())
                    }
                }
            },
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = Color.White.copy(alpha = alpha),
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = alpha),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

