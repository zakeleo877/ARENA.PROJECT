package com.zakeleo877.saeedstore.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zakeleo877.saeedcore.data.Cached
import com.zakeleo877.saeedcore.data.CartStore
import com.zakeleo877.saeedcore.data.OrderRepository
import com.zakeleo877.saeedcore.data.PendingOrderRow
import com.zakeleo877.saeedcore.data.SubmitOutcome
import com.zakeleo877.saeedcore.data.local.CatalogDao
import com.zakeleo877.saeedcore.data.local.ProductEntity
import com.zakeleo877.saeedcore.data.local.SettingEntity
import com.zakeleo877.saeedcore.data.remote.CatalogApi
import com.zakeleo877.saeedcore.model.CartItem
import com.zakeleo877.saeedcore.model.PendingOrder
import com.zakeleo877.saeedcore.model.PendingOrderItem
import com.zakeleo877.saeedcore.model.Product
import com.zakeleo877.saeedcore.model.StoreSettings
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the customer screens need, in one observable state. */
data class CustomerUiState(
    val products: List<Product> = emptyList(),
    val settings: StoreSettings? = null,
    val isRefreshing: Boolean = false,
    val loadFailed: Boolean = false,
    val offline: Boolean = false,
    val everLoaded: Boolean = false,
)

class CustomerViewModel(
    private val dao: CatalogDao,
    private val api: CatalogApi,
    private val orderRepository: OrderRepository,
    val cart: CartStore,
    private val isOnline: () -> Boolean,
) : ViewModel() {
    private val _refreshing = MutableStateFlow(false)
    private val _failed = MutableStateFlow(false)
    private val _offline = MutableStateFlow(!isOnline())

    val cartItems: StateFlow<List<CartItem>> = cart.items.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )
    val cartCount: StateFlow<Int> = cart.count.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), 0,
    )
    val pendingOrders: StateFlow<List<PendingOrderRow>> = orderRepository.observePending().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )

    val uiState: StateFlow<CustomerUiState> = combine(
        dao.observeProducts(),
        dao.observeSettings(),
        dao.observeMetadata("products"),
        _refreshing,
        _failed,
        _offline,
    ) { products, settings, metadata, refreshing, failed, offline ->
        CustomerUiState(
            products = products.map(ProductEntity::model),
            settings = settings.toStoreSettings(),
            isRefreshing = refreshing,
            loadFailed = failed,
            offline = offline,
            everLoaded = metadata != null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CustomerUiState())

    init {
        refresh()
        viewModelScope.launch {
            // Keeps the offline banner and the send button state in sync.
            while (true) {
                _offline.value = !isOnline()
                delay(5_000)
            }
        }
    }

    fun refresh() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            val productsResult = runCatching { api.products() }
            productsResult.onSuccess { snapshot ->
                runCatching {
                    dao.replaceProducts(snapshot.map(ProductEntity::from), System.currentTimeMillis())
                }
            }
            val settingsResult = runCatching { api.settings() }
            settingsResult.onSuccess { settings ->
                runCatching {
                    dao.replaceSettings(
                        listOf(
                            SettingEntity("storeName", settings.storeName),
                            SettingEntity("whatsapp", settings.whatsapp),
                            SettingEntity("currency", settings.currency),
                            SettingEntity("footerNote", settings.footerNote),
                        ),
                        System.currentTimeMillis(),
                    )
                }
            }
            // The cache stays usable when the network fails, so only a cold start fails.
            _failed.value = productsResult.isFailure && !dao.hasProducts()
            _refreshing.value = false
        }
    }

    fun addToCart(product: Product, quantity: Int = 1) = cart.add(product, quantity)

    fun setQuantity(productId: String, quantity: Int) = cart.setQuantity(productId, quantity)

    fun removeFromCart(productId: String): CartItem? = cart.removeAndReturn(productId)

    fun restoreCartItem(item: CartItem) = cart.restore(item)

    fun clearCart() = cart.clear()

    /** Builds the immutable snapshot that the retry path reuses verbatim. */
    fun buildPendingOrder(customer: String, phone: String, notes: String, location: String): PendingOrder =
        PendingOrder(
            clientId = UUID.randomUUID().toString(),
            customer = customer.trim(),
            phone = phone.trim(),
            notes = notes.trim(),
            location = location.trim(),
            items = cart.snapshot().map { PendingOrderItem(it.product.id, it.quantity) },
        )

    suspend fun submit(order: PendingOrder): SubmitOutcome = orderRepository.submit(order, isOnline())

    suspend fun retry(clientId: String): SubmitOutcome = orderRepository.retry(clientId)

    suspend fun deletePending(clientId: String) = orderRepository.delete(clientId)

    private fun List<SettingEntity>.toStoreSettings(): StoreSettings? {
        val map = associate { it.key to it.value }
        if (!map.keys.containsAll(listOf("storeName", "whatsapp", "currency", "footerNote"))) return null
        return StoreSettings(
            storeName = map.getValue("storeName"),
            whatsapp = map.getValue("whatsapp"),
            currency = map.getValue("currency"),
            footerNote = map.getValue("footerNote"),
        )
    }
}
