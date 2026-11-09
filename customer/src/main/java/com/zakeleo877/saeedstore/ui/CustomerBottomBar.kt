package com.zakeleo877.saeedstore.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zakeleo877.saeedcore.designsystem.SaeedGold
import com.zakeleo877.saeedcore.designsystem.SaeedGreen
import com.zakeleo877.saeedcore.designsystem.SaeedGreenSoft
import com.zakeleo877.saeedcore.designsystem.SaeedInk
import com.zakeleo877.saeedcore.designsystem.SaeedInkMuted
import com.zakeleo877.saeedstore.R

/** Four real destinations; the pending tab is the local send queue, not order history. */
@Composable
internal fun SaeedBottomBar(
    cartCount: Int,
    pendingCount: Int,
    onHomeClick: () -> Unit,
    onProductsClick: () -> Unit,
    onCartClick: () -> Unit,
    onPendingClick: () -> Unit,
    current: SaeedTab,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        @Composable
        fun Item(
            tab: SaeedTab,
            icon: ImageVector,
            label: String,
            description: String,
            count: Int,
            action: () -> Unit,
        ) {
            val badgeDescription = stringResource(R.string.catalog_tab_badge, count)
            NavigationBarItem(
                selected = current == tab,
                onClick = action,
                modifier = Modifier.semantics {
                    contentDescription = if (count > 0) "$description، $badgeDescription" else description
                },
                icon = {
                    BadgedBox(
                        badge = {
                            if (count > 0) {
                                Badge(containerColor = SaeedGold, contentColor = SaeedInk) {
                                    Text(count.toString())
                                }
                            }
                        },
                    ) { Icon(icon, contentDescription = null) }
                },
                label = {
                    Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SaeedGreen,
                    selectedTextColor = SaeedGreen,
                    indicatorColor = SaeedGreenSoft,
                    unselectedIconColor = SaeedInkMuted,
                    unselectedTextColor = SaeedInkMuted,
                ),
            )
        }
        val home = stringResource(R.string.catalog_home_tab)
        val products = stringResource(R.string.catalog_products_tab)
        val cart = stringResource(R.string.cart)
        val pending = stringResource(R.string.pending_orders)
        Item(SaeedTab.HOME, Icons.Filled.Home, home, home, 0, onHomeClick)
        Item(SaeedTab.PRODUCTS, Icons.Filled.Inventory2, products, products, 0, onProductsClick)
        Item(SaeedTab.CART, Icons.Filled.ShoppingCart, cart, cart, cartCount, onCartClick)
        Item(SaeedTab.PENDING, Icons.Filled.ReceiptLong, stringResource(R.string.catalog_pending_tab), pending, pendingCount, onPendingClick)
    }
}
