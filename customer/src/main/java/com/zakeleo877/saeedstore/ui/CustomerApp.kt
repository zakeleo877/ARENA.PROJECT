package com.zakeleo877.saeedstore.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

    var checkoutOpen by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var successNumber by remember { mutableStateOf<Long?>(null) }
    var queuedMessage by remember { mutableStateOf(false) }
    var lastRemoved by remember { mutableStateOf<CartItem?>(null) }

    val currency = state.settings?.currency ?: "د.ج"

    LaunchedEffect(Unit) {
        viewModel.pendingOrders.collect { orders ->
            // Nothing to do here; the screen observes the same flow.
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = Routes.CATALOG) {
            composable(Routes.CATALOG) {
                CatalogScreen(
                    state = state,
                    cartCount = cartCount,
                    onProductClick = { product ->
                        selectedProduct = product
                        navController.navigate(Routes.DETAIL)
                    },
                    onAddToCart = { product ->
                        viewModel.addToCart(product)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = context.getString(R.string.add_to_cart),
                                actionLabel = null,
                            )
                        }
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
                    ProductDetailScreen(
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    // Undo after removing a cart item.
    val removed = lastRemoved
    if (removed != null) {
        LaunchedEffect(removed) {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.cart_removed),
                actionLabel = context.getString(R.string.action_undo),
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
                            location = form.location,
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
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.generic_error))
                                }
                            }
                        }
                    }
                },
            )
        }
    }

    successNumber?.let { number ->
        SaeedDialog(
            title = context.getString(R.string.order_success_title),
            message = buildString {
                append(context.getString(R.string.order_number))
                append(": ")
                append(Formatters.orderNumber(number))
                val settings = state.settings
                if (settings != null) {
                    append('\n')
                    append(settings.storeName)
                }
            },
            confirmLabel = context.getString(R.string.order_done),
            onConfirm = { successNumber = null },
            onDismiss = { successNumber = null },
        )
    }

    if (queuedMessage) {
        SaeedDialog(
            title = context.getString(R.string.order_queued_title),
            message = context.getString(R.string.order_queued_message),
            confirmLabel = context.getString(R.string.order_done),
            onConfirm = { queuedMessage = false },
            onDismiss = { queuedMessage = false },
        )
    }
}
