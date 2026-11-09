package com.zakeleo877.saeedadmin.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.content.ContextCompat
import com.zakeleo877.saeedadmin.R
import com.zakeleo877.saeedadmin.notifications.OrderNotificationScheduler
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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
    val diagnostics by viewModel.diagnostics.collectAsStateWithLifecycle()
    val diagnosticsRunning by viewModel.diagnosticsRunning.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) OrderNotificationScheduler.checkNow(context)
    }

    LaunchedEffect(session?.userId) {
        if (session != null && OrderNotificationScheduler.isEnabled(context)) {
            OrderNotificationScheduler.checkNow(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !OrderNotificationScheduler.permissionWasPrompted(context) &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                OrderNotificationScheduler.markPermissionPrompted(context)
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    // Keep an open admin screen reasonably fresh, and refresh on every resume.
    // This is separate from Android's delayed background notification checks.
    LaunchedEffect(session?.userId, lifecycleOwner) {
        if (session != null) lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (currentCoroutineContext().isActive) {
                viewModel.refresh()
                delay(30_000)
            }
        }
    }

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

    // rememberSaveable keeps the tab across rotation and process death. With
    // plain remember the owner was thrown back to the dashboard every time the
    // phone was turned.
    var tab by rememberSaveable { mutableStateOf(AdminTab.DASHBOARD) }

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
                        ordersLoadFailed = state.ordersLoadFailed,
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
                        searchQuery = state.searchQuery,
                        onSearchChange = viewModel::setSearchQuery,
                        // A failed read used to look exactly like an empty
                        // catalogue: the owner sees nothing and blames the save.
                        loadError = state.errorMessage,
                        onRetry = { viewModel.refresh() },
                        onSave = { id, name, price, description, imageUrl, category, available,
                            sortOrder, originalPrice, rating, onDone ->
                            viewModel.saveProduct(
                                id, name, price, description, imageUrl, category, available, sortOrder,
                                originalPrice = originalPrice,
                                rating = rating,
                                onDone = onDone,
                            )
                        },
                        onDelete = { id -> viewModel.deleteProduct(id) },
                        onUploadImage = { productId, fileName, bytes, onDone ->
                            viewModel.attachImage(productId, fileName, bytes, onDone)
                        },
                    )
                    AdminTab.SETTINGS -> SettingsScreen(
                        onSave = viewModel::saveSettings,
                        initialSettings = settings,
                        onRefresh = viewModel::refresh,
                        checks = diagnostics,
                        checking = diagnosticsRunning,
                        onCheckConnection = viewModel::runDiagnostics,
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
    loadError == "invalid_credentials" -> stringResource(R.string.admin_invalid_credentials)
    loadError == "rate_limited" -> stringResource(R.string.admin_rate_limited)
    loadError != null -> stringResource(R.string.admin_load_error)
    else -> null
}
