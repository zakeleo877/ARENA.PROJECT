package com.zakeleo877.saeedadmin.ui

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zakeleo877.saeedadmin.R

private enum class AdminTab(val labelRes: Int, val icon: ImageVector) {
    DASHBOARD(R.string.admin_tab_dashboard, Icons.Filled.Dashboard),
    ORDERS(R.string.admin_tab_orders, Icons.Filled.ReceiptLong),
    PRODUCTS(R.string.admin_tab_products, Icons.Filled.Inventory2),
    SETTINGS(R.string.admin_tab_settings, Icons.Filled.Settings),
}

/** Root composable of the admin app: login gate, bottom navigation and the four tabs. */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun AdminApp(viewModel: AdminViewModel = hiltViewModel()) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val authorization by viewModel.authorization.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (session == null) {
        LoginScreen(
            loading = state.isLoading,
            errorMessage = authErrorMessage(authorization, state.errorMessage),
            onSignIn = { email, password ->
                viewModel.signIn(email, password) { result ->
                    if (result is com.zakeleo877.saeedcore.data.remote.AuthResult.Failure) {
                        // The message is surfaced through the state below.
                    }
                }
            },
        )
        return
    }

    var tab by remember { mutableStateOf(AdminTab.DASHBOARD) }

    // Wide screens (foldables and tablets) get a navigation rail instead of the
    // bottom bar, so the content area keeps its full height.
    val activity = LocalContext.current.findActivity()
    val widthSizeClass = if (activity != null) {
        calculateWindowSizeClass(activity).widthSizeClass
    } else {
        null
    }
    val useRail = widthSizeClass == WindowWidthSizeClass.Expanded

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!useRail) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    AdminTab.entries.forEach { entry ->
                        NavigationBarItem(
                            selected = tab == entry,
                            onClick = { tab = entry },
                            icon = { Icon(entry.icon, contentDescription = null) },
                            label = { Text(stringResource(entry.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Row(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (useRail) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    AdminTab.entries.forEach { entry ->
                        NavigationRailItem(
                            selected = tab == entry,
                            onClick = { tab = entry },
                            icon = { Icon(entry.icon, contentDescription = null) },
                            label = { Text(stringResource(entry.labelRes)) },
                        )
                    }
                }
            }
            Box(
                Modifier
                    .fillMaxSize(),
            ) {
                when (tab) {
                    AdminTab.DASHBOARD -> DashboardScreen(
                        stats = viewModel.stats(),
                        onRefresh = viewModel::refresh,
                    )
                    AdminTab.ORDERS -> OrdersScreen(
                        state = state,
                        onFilterChange = viewModel::setStatusFilter,
                        onSearchChange = viewModel::setSearchQuery,
                        onStatusUpdate = { id, status -> viewModel.updateOrderStatus(id, status) },
                        onRefresh = viewModel::refresh,
                    )
                    AdminTab.PRODUCTS -> ProductsScreen(
                        products = state.products,
                        onSave = { id, name, price, description, imageUrl, category, available, sortOrder,
                            originalPrice, rating ->
                            viewModel.saveProduct(
                                id, name, price, description, imageUrl, category, available, sortOrder,
                                originalPrice = originalPrice,
                                rating = rating,
                            ) {}
                        },
                        onDelete = { id -> viewModel.deleteProduct(id) },
                        onUploadImage = { productId, fileName, bytes, onDone ->
                            viewModel.attachImage(productId, fileName, bytes, onDone)
                        },
                    )
                    AdminTab.SETTINGS -> SettingsScreen(
                        onSave = { values -> viewModel.saveSettings(values) {} },
                        onSignOut = viewModel::signOut,
                    )
                }
            }
        }
    }
}

/** Unwraps the activity from the context chain that Compose provides. */
private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun authErrorMessage(
    authorization: com.zakeleo877.saeedadmin.data.AdminAuthorization?,
    loadError: String?,
): String? = when {
    authorization == com.zakeleo877.saeedadmin.data.AdminAuthorization.NotAuthorized ->
        stringResource(R.string.admin_not_authorized)
    loadError != null -> stringResource(R.string.admin_load_error)
    else -> null
}
