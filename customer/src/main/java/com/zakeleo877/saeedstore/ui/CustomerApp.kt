package com.zakeleo877.saeedstore.ui

import android.content.Intent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.zakeleo877.saeedcore.data.SubmitOutcome
import com.zakeleo877.saeedcore.designsystem.SaeedBottomSheet
import com.zakeleo877.saeedcore.designsystem.SaeedDialog
import com.zakeleo877.saeedcore.model.CartItem
import com.zakeleo877.saeedcore.model.Product
import com.zakeleo877.saeedcore.util.Formatters
import com.zakeleo877.saeedcore.util.Links
import com.zakeleo877.saeedstore.R
import kotlinx.coroutines.launch

private object Routes {
    const val CATALOG = "catalog"
    const val CART = "cart"
    const val PENDING = "pending"
    const val DETAIL = "product"
}

/** Root composable of the customer app: navigation, checkout sheet and order feedback. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CustomerApp(viewModel: CustomerViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val cartCount by viewModel.cartCount.collectAsStateWithLifecycle()
    val pendingOrders by viewModel.pendingOrders.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // The strings are read through the composition so they follow the
    // configuration changes.
    val addedToCartText = stringResource(R.string.add_to_cart)
    val cartRemovedText = stringResource(R.string.cart_removed)
    val undoText = stringResource(R.string.action_undo)
    val genericErrorText = stringResource(R.string.generic_error)
    val orderSuccessTitle = stringResource(R.string.order_success_title)
    val orderNumberLabel = stringResource(R.string.order_number)
    val orderDoneText = stringResource(R.string.order_done)
    val orderQueuedTitle = stringResource(R.string.order_queued_title)
    val orderQueuedMessage = stringResource(R.string.order_queued_message)
    val cartTotalLabel = stringResource(R.string.cart_total)

    var checkoutOpen by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var successNumber by remember { mutableStateOf<Long?>(null) }
    var queuedMessage by remember { mutableStateOf(false) }
    var lastRemoved by remember { mutableStateOf<CartItem?>(null) }

    val currency = state.settings?.currency ?: "د.ج"

    // The bar belongs to the shell, not to one screen. It used to be drawn by
    // the catalogue alone, so the cart and the pending list had no bar and no
    // way back to the shop.
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val showBar = currentRoute != null && !currentRoute.startsWith(Routes.DETAIL)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBar) {
                SaeedBottomBar(
                    cartCount = cartCount,
                    pendingCount = pendingOrders.size,
                    current = when {
                        currentRoute.orEmpty().startsWith(Routes.CART) -> SaeedTab.CART
                        currentRoute.orEmpty().startsWith(Routes.PENDING) -> SaeedTab.PENDING
                        else -> SaeedTab.CATALOG
                    },
                    onCatalogClick = {
                        navController.navigate(Routes.CATALOG) {
                            popUpTo(Routes.CATALOG) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onCartClick = { navController.navigate(Routes.CART) { launchSingleTop = true } },
                    onPendingClick = { navController.navigate(Routes.PENDING) { launchSingleTop = true } },
                )
            }
        },
    ) { innerPadding ->
    Box(Modifier.fillMaxSize().padding(innerPadding)) {
      SharedTransitionLayout {
        NavHost(navController = navController, startDestination = Routes.CATALOG) {
            composable(Routes.CATALOG) {
                CatalogScreen(
                    sharedScope = this@SharedTransitionLayout,
                    animatedScope = this,
                    state = state,
                    cartCount = cartCount,
                    pendingCount = pendingOrders.size,
                    onProductClick = { product ->
                        selectedProduct = product
                        navController.navigate(Routes.DETAIL)
                    },
                    onAddToCart = { product ->
                        viewModel.addToCart(product)
                        scope.launch { snackbarHostState.showSnackbar(addedToCartText) }
                    },
                    onCartClick = { navController.navigate(Routes.CART) },
                    onPendingClick = { navController.navigate(Routes.PENDING) },
                    onRetry = viewModel::refresh,
                )
            }

            composable(Routes.CART) {
                CartScreen(
                    items = cartItems,
                    currency = currency,
                    onQuantityChange = viewModel::setQuantity,
                    onRemove = { productId ->
                        lastRemoved = viewModel.removeFromCart(productId)
                    },
                    onCheckout = { checkoutOpen = true },
                )
            }

            composable(Routes.PENDING) {
                PendingOrdersScreen(
                    orders = pendingOrders,
                    onRetry = { clientId ->
                        scope.launch {
                            when (viewModel.retry(clientId)) {
                                is SubmitOutcome.Sent -> navController.popBackStack()
                                else -> Unit
                            }
                        }
                    },
                    onDelete = { clientId ->
                        scope.launch { viewModel.deletePending(clientId) }
                    },
                )
            }

            composable(
                route = "${Routes.DETAIL}?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType; defaultValue = "" }),
            ) {
                val product = state.products.firstOrNull { it.id == selectedProduct?.id } ?: selectedProduct
                if (product == null) {
                    navController.popBackStack()
                } else {
                    val animatedScope = this
                    with(this@SharedTransitionLayout) {
                        ProductDetailScreen(
                            animatedScope = animatedScope,
                            product = product,
                            currency = currency,
                            onBack = { navController.popBackStack() },
                            onAddToCart = { quantity ->
                                viewModel.addToCart(product, quantity)
                                navController.popBackStack()
                            },
                        )
                    }
                }
            }
        }
      }
    }

        SnackbarHost(
            hostState = snackbarHostState,
            // Inside a Scaffold there is no BoxScope, so `align` does not exist.
            // The Scaffold places the host; this centres it across the width.
            modifier = Modifier.fillMaxWidth(),
        )
    }

    // Undo after removing a cart item.
    val removed = lastRemoved
    if (removed != null) {
        LaunchedEffect(removed) {
            val result = snackbarHostState.showSnackbar(
                message = cartRemovedText,
                actionLabel = undoText,
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreCartItem(removed)
            }
            lastRemoved = null
        }
    }

    if (checkoutOpen) {
        SaeedBottomSheet(onDismiss = { if (!submitting) checkoutOpen = false }) {
            CheckoutSheet(
                submitting = submitting,
                onSubmit = { form ->
                    submitting = true
                    scope.launch {
                        val order = viewModel.buildPendingOrder(
                            customer = form.customer,
                            phone = form.phone,
                            notes = form.notes,
                            customerLocationUrl = form.customerLocationUrl,
                        )
                        when (val outcome = viewModel.submit(order)) {
                            is SubmitOutcome.Sent -> {
                                submitting = false
                                checkoutOpen = false
                                successNumber = outcome.orderNumber
                                viewModel.clearCart()
                            }
                            is SubmitOutcome.Queued -> {
                                submitting = false
                                checkoutOpen = false
                                queuedMessage = true
                                viewModel.clearCart()
                            }
                            is SubmitOutcome.Rejected -> {
                                submitting = false
                                scope.launch { snackbarHostState.showSnackbar(genericErrorText) }
                            }
                        }
                    }
                },
            )
        }
    }

    successNumber?.let { number ->
        val settings = state.settings
        val total = cartItems.sumOf { it.estimatedTotal }
        val summary = buildString {
            append(orderNumberLabel)
            append(": ")
            append(Formatters.orderNumber(number))
            if (settings != null) {
                append('\n')
                append(settings.storeName)
                append(" · ")
                append(cartTotalLabel)
                append(": ")
                append(Formatters.money(total, settings.currency))
            }
        }

        SaeedDialog(
            title = orderSuccessTitle,
            message = summary,
            confirmLabel = orderDoneText,
            onConfirm = { successNumber = null },
            onDismiss = { successNumber = null },
        )

        // The WhatsApp message carries the invoice number, the items and the total.
        val whatsappLink = settings?.let { current ->
            val message = buildString {
                append(current.storeName)
                append(" — ")
                append(orderNumberLabel)
                append(": ")
                append(Formatters.orderNumber(number))
                append('\n')
                append(cartTotalLabel)
                append(": ")
                append(Formatters.money(total, current.currency))
            }
            Links.whatsapp(current.whatsapp, message)
        }
        if (whatsappLink != null) {
            LaunchedEffect(whatsappLink) {
                val intent = Intent(Intent.ACTION_VIEW, whatsappLink.toUri())
                runCatching { context.startActivity(intent) }
            }
        }
    }

    if (queuedMessage) {
        SaeedDialog(
            title = orderQueuedTitle,
            message = orderQueuedMessage,
            confirmLabel = orderDoneText,
            onConfirm = { queuedMessage = false },
            onDismiss = { queuedMessage = false },
        )
    }
}
