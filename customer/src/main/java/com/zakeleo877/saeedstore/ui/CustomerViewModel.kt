package com.zakeleo877.saeedstore.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
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
import com.zakeleo877.saeedcore.network.NetworkMonitor
import com.zakeleo877.saeedstore.BuildConfig
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the customer screens need, in one observable state. */
/**
 * The shape of one throwaway product. It mirrors the wire format so the demo
 * value stays readable in the resource file, and it deliberately cannot carry
 * an id that a real product could collide with.
 */
@Serializable
private data class DemoProduct(
    val id: String,
    val name: String,
    val price: String,
    val category: String,
    val imageUrl: String = "",
    @SerialName("originalPrice") val originalPrice: String? = null,
    val rating: Double? = null,
    val available: Boolean = true,
) {
    fun toProduct(): Product = Product(
        id = id,
        name = name,
        price = PriceReader.parse(price),
        description = "",
        imageUrl = imageUrl,
        category = category,
        available = available,
        sortOrder = 0,
        createdAt = java.time.Instant.EPOCH,
        originalPrice = originalPrice?.let(PriceReader::parse),
        rating = rating,
    )
}

/** Reads the demo prices the way the rest of the app does, or fails loudly. */
private object PriceReader {
    fun parse(text: String): java.math.BigDecimal =
        text.toBigDecimalOrNull() ?: throw IllegalArgumentException("bad demo price: $text")
}

/** Shared JSON reader for the optional throwaway catalogue. */
private val demoJson = Json { ignoreUnknownKeys = true }

data class CustomerUiState(
    val products: List<Product> = emptyList(),
    val settings: StoreSettings? = null,
    val isRefreshing: Boolean = false,
    val loadFailed: Boolean = false,
    val offline: Boolean = false,
    val everLoaded: Boolean = false,
)

@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val dao: CatalogDao,
    private val api: CatalogApi,
    private val orderRepository: OrderRepository,
    val cart: CartStore,
    private val monitor: NetworkMonitor,
    // Only used to read the optional throwaway catalogue from the resources.
    @ApplicationContext private val context: Context,
) : ViewModel() {

    /** The state of the network is read on demand, so a queued order is the answer when the device is offline. */
    private val isOnline: () -> Boolean = { monitor.isOnline() }

    val cartItems: StateFlow<List<CartItem>> = cart.items.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )
    val cartCount: StateFlow<Int> = cart.count.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), 0,
    )
    val pendingOrders: StateFlow<List<PendingOrderRow>> = orderRepository.observePending().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )

    /** The refresh flags travel together so the combine stays within its arity. */
    private data class RefreshState(
        val refreshing: Boolean = false,
        val failed: Boolean = false,
        val offline: Boolean = false,
    )

    private val _refresh = MutableStateFlow(RefreshState())

    /**
     * The throwaway catalogue, or null in a normal build.
     *
     * `demo_products` is an empty string unless the build explicitly asks for
     * demo data, so parsing yields nothing and no shop ever sees these products.
     * A malformed value is ignored rather than crashing the screen.
     */
    private val demoProducts: List<Product>? by lazy {
        val raw = BuildConfig.DEMO_PRODUCTS
        if (raw.isBlank()) return@lazy null
        runCatching { demoJson.decodeFromString<List<DemoProduct>>(raw).map { it.toProduct() } }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
    }

    val uiState: StateFlow<CustomerUiState> = combine(
        dao.observeProducts(),
        dao.observeSettings(),
        dao.observeMetadata("products"),
        _refresh,
    ) { products, settings, metadata, refresh ->
        CustomerUiState(
            products = products.map { it.model() },
            settings = settings.toStoreSettings(),
            isRefreshing = refresh.refreshing,
            loadFailed = refresh.failed,
            offline = refresh.offline,
            everLoaded = metadata != null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CustomerUiState())

    init {
        viewModelScope.launch { seedDemoCatalog() }
        refresh()
        viewModelScope.launch {
            // Keeps the offline banner and the send button state in sync.
            while (true) {
                _refresh.value = _refresh.value.copy(offline = !isOnline())
                delay(5_000)
            }
        }
        viewModelScope.launch {
            // The offline banner promises the order is sent when the connection
            // returns, and nothing did it: the customer had to open the list and
            // tap retry themselves. Watching the network is what keeps that
            // promise. Only the transition from offline to online sends, so a
            // device that is online all day does not retry anything.
            var wasOnline = isOnline()
            monitor.observe().collect { online ->
                if (online && !wasOnline) {
                    runCatching { orderRepository.drainWaiting() }
                }
                wasOnline = online
            }
        }
    }

    fun refresh() {
        if (_refresh.value.refreshing) return
        viewModelScope.launch {
            _refresh.value = _refresh.value.copy(refreshing = true)
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
            // The demo data also counts as content: an emulator run has no route
            // to a project, and the screen must still be inspectable.
            val hasContent = dao.hasProducts() || demoProducts != null
            _refresh.value = _refresh.value.copy(failed = productsResult.isFailure && !hasContent)
            _refresh.value = _refresh.value.copy(refreshing = false)
        }
    }

    /**
     * Fills an empty cache with the throwaway catalogue, when one is compiled in.
     *
     * This exists so the emulator run, which deliberately has no route to a
     * project, can show a real product card instead of only the offline state.
     * It is empty in a normal build, so a real shop never sees these names.
     */
    private suspend fun seedDemoCatalog() {
        val demo = demoProducts ?: return
        if (dao.hasProducts()) return
        runCatching {
            dao.replaceProducts(demo.map(ProductEntity::from), System.currentTimeMillis())
        }
    }

    fun addToCart(product: Product, quantity: Int = 1) = cart.add(product, quantity)

    fun setQuantity(productId: String, quantity: Int) = cart.setQuantity(productId, quantity)

    fun removeFromCart(productId: String): CartItem? = cart.removeAndReturn(productId)

    fun restoreCartItem(item: CartItem) = cart.restore(item)

    fun clearCart() = cart.clear()

    /** Builds the immutable snapshot that the retry path reuses verbatim. */
    suspend fun buildPendingOrder(customer: String, phone: String, notes: String, customerLocationUrl: String): PendingOrder {
        // Do not place an order from the temporary, partially restored basket.
        cart.awaitRestored()
        return PendingOrder(
            clientId = UUID.randomUUID().toString(),
            customer = customer.trim(),
            phone = phone.trim(),
            notes = notes.trim(),
            customerLocationUrl = customerLocationUrl.trim(),
            items = cart.snapshot().map { PendingOrderItem(it.product.id, it.quantity) },
        )
    }

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
