package com.zakeleo877.saeedadmin.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.zakeleo877.saeedadmin.data.AdminAuthorization
import com.zakeleo877.saeedadmin.data.AdminRepository
import com.zakeleo877.saeedcore.data.remote.AdminSession
import com.zakeleo877.saeedcore.data.remote.AuthError
import com.zakeleo877.saeedcore.data.remote.AuthResult
import com.zakeleo877.saeedcore.model.Order
import com.zakeleo877.saeedcore.util.ArabicText
import com.zakeleo877.saeedcore.model.OrderStatus
import com.zakeleo877.saeedcore.model.Product
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class DashboardStats(
    val newOrders: Int = 0,
    val todayOrders: Int = 0,
    val totalOrders: Int = 0,
    val productCount: Int = 0,
    val handedOverTotal: BigDecimal = BigDecimal.ZERO,
)

data class AdminUiState(
    // The list tab is filtered; the dashboard must always use the unfiltered snapshot.
    val orders: List<Order> = emptyList(),
    val allOrders: List<Order> = emptyList(),
    val dashboardStats: DashboardStats = DashboardStats(),
    val products: List<Product> = emptyList(),
    val statusFilter: OrderStatus? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val ordersLoaded: Boolean = false,
    val productsLoaded: Boolean = false,
    val ordersLoadFailed: Boolean = false,
    val productsLoadFailed: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class AdminViewModel @Inject constructor(private val repository: AdminRepository) : ViewModel() {
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    private val _filter = MutableStateFlow<OrderStatus?>(null)
    private val _query = MutableStateFlow("")
    private val _settings = MutableStateFlow<Map<String, String>?>(null)
    val settings: StateFlow<Map<String, String>?> = _settings

    // The holder already exposes read-only state flows.
    val session: StateFlow<AdminSession?> = repository.sessionHolder.session
    val authorization: StateFlow<AdminAuthorization?> = repository.sessionHolder.authorization

    /** The loading flag and the error message travel together. */
    private data class LoadState(
        val loading: Boolean = false,
        val error: String? = null,
        val ordersLoaded: Boolean = false,
        val productsLoaded: Boolean = false,
        val ordersFailed: Boolean = false,
        val productsFailed: Boolean = false,
    )

    private val _load = MutableStateFlow(LoadState())

    val uiState: StateFlow<AdminUiState> = combine(
        _orders, _products, _filter, _query, _load,
    ) { orders, products, filter, query, load ->
        AdminUiState(
            orders = orders.filter { order ->
                val matchesStatus = filter == null || order.status == filter
                // The same folding the catalogue uses: a customer name written
                // «أحمد» must be found by a search for «احمد», which is what a
                // phone keyboard produces. The order number and the phone are
                // digits, so folding them is harmless.
                val matchesQuery = query.isBlank() ||
                    ArabicText.contains(order.number.toString(), query) ||
                    ArabicText.contains(order.customer, query) ||
                    ArabicText.contains(order.phone, query)
                matchesStatus && matchesQuery
            },
            allOrders = orders,
            dashboardStats = calculateDashboardStats(orders, products),
            products = if (query.isBlank()) {
                products
            } else {
                products.filter {
                    ArabicText.contains(it.name, query) ||
                        ArabicText.contains(it.category, query) ||
                        ArabicText.contains(it.description, query)
                }
            },
            statusFilter = filter,
            searchQuery = query,
            isLoading = load.loading,
            ordersLoaded = load.ordersLoaded,
            productsLoaded = load.productsLoaded,
            ordersLoadFailed = load.ordersFailed,
            productsLoadFailed = load.productsFailed,
            errorMessage = load.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminUiState())

    init {
        viewModelScope.launch {
            // A temporary outage must not strand the encrypted session behind
            // the login screen. Retry while this screen remains open; sign-out
            // after a successful restore does not start this loop again.
            while (repository.sessionHolder.session.value == null) {
                if (repository.restoreSession()) break
                delay(30_000)
            }
        }
        // Load the catalog as soon as an admin session becomes available.
        viewModelScope.launch {
            repository.sessionHolder.session.collect { session ->
                if (session != null) refresh()
            }
        }
    }



    fun signIn(email: String, password: String, onResult: (AuthResult) -> Unit) {
        viewModelScope.launch {
            _load.value = _load.value.copy(loading = true)
            val result = repository.signIn(email, password)
            _load.value = _load.value.copy(
                loading = false,
                error = when (result) {
                    is AuthResult.Success -> null
                    is AuthResult.Failure -> when (result.reason) {
                        AuthError.INVALID_CREDENTIALS -> "invalid_credentials"
                        AuthError.RATE_LIMITED -> "rate_limited"
                        else -> "load_failed"
                    }
                },
            )
            if (result is AuthResult.Success) refresh()
            onResult(result)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _orders.value = emptyList()
            _products.value = emptyList()
            _settings.value = null
            _load.value = LoadState()
        }
    }

    fun refresh() {
        val session = repository.sessionHolder.session.value ?: return
        if (_load.value.loading) return
        _load.value = _load.value.copy(loading = true)
        viewModelScope.launch {
            try {
                val ordersResult = runCatching { repository.orders(session) }
                val productsResult = runCatching { repository.products(session) }
                val settingsResult = runCatching { repository.settings(session) }
                // A response from a session that signed out must not populate
                // the next user's screen.
                if (repository.sessionHolder.session.value?.userId != session.userId) return@launch
                // A failed read keeps the last list, but is explicitly labelled
                // as stale rather than pretending that the shop has no orders.
                _orders.value = ordersResult.getOrDefault(_orders.value)
                _products.value = productsResult.getOrDefault(_products.value)
                settingsResult.onSuccess { values ->
                    if (values.keys.containsAll(listOf("storeName", "whatsapp", "currency", "footerNote"))) {
                        _settings.value = values
                    }
                }
                _load.value = _load.value.copy(
                    ordersLoaded = _load.value.ordersLoaded || ordersResult.isSuccess,
                    productsLoaded = _load.value.productsLoaded || productsResult.isSuccess,
                    ordersFailed = ordersResult.isFailure,
                    productsFailed = productsResult.isFailure,
                    error = when {
                        productsResult.isFailure -> repository.lastFailure ?: "products_failed"
                        ordersResult.isFailure -> "load_failed"
                        else -> null
                    },
                )
            } finally {
                _load.value = _load.value.copy(loading = false)
            }
        }
    }

    private val _diagnostics =
        MutableStateFlow<List<com.zakeleo877.saeedcore.data.remote.DiagnosticCheck>?>(null)

    /** The latest connection check. Null before the first run. */
    val diagnostics: StateFlow<List<com.zakeleo877.saeedcore.data.remote.DiagnosticCheck>?> =
        _diagnostics

    private val _diagnosticsRunning = MutableStateFlow(false)

    /** True while the check is in flight, so the button can say so. */
    val diagnosticsRunning: StateFlow<Boolean> = _diagnosticsRunning

    fun runDiagnostics() {
        val session = repository.sessionHolder.session.value ?: return
        if (_diagnosticsRunning.value) return
        _diagnosticsRunning.value = true
        viewModelScope.launch {
            _diagnostics.value = runCatching { repository.diagnose(session) }
                .getOrElse {
                    listOf(
                        com.zakeleo877.saeedcore.data.remote.DiagnosticCheck(
                            "إجراء الفحص",
                            false,
                            "تعذّر إكمال الفحص — تحقّق من الاتصال",
                        ),
                    )
                }
            _diagnosticsRunning.value = false
        }
    }

    fun setStatusFilter(status: OrderStatus?) {
        _filter.value = status
    }

    fun setSearchQuery(query: String) {
        _query.value = query
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus, onDone: (Boolean) -> Unit = {}) {
        val session = repository.sessionHolder.session.value ?: run {
            onDone(false)
            return
        }
        viewModelScope.launch {
            val ok = try {
                repository.updateOrderStatus(session, orderId, status)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
            // A response from an old session must not change the current view.
            val sameSession = repository.sessionHolder.session.value?.userId == session.userId
            if (ok && sameSession) {
                _orders.value = _orders.value.map { if (it.id == orderId) it.copy(status = status) else it }
            }
            onDone(ok && sameSession)
        }
    }

    /** Set when a write is refused, so the form can show what the server said. */
    val saveFailure = MutableStateFlow<String?>(null)

    fun saveProduct(
        id: String?,
        name: String,
        price: BigDecimal,
        description: String,
        imageUrl: String,
        category: String,
        available: Boolean,
        sortOrder: Int,
        originalPrice: BigDecimal? = null,
        rating: Double? = null,
        onDone: (Boolean, String?) -> Unit,
    ) {
        val session = repository.sessionHolder.session.value ?: run {
            onDone(false, "no_session")
            return
        }
        viewModelScope.launch {
            val ok = repository.upsertProduct(
                session, id, name, price, description, imageUrl, category, available, sortOrder,
                originalPrice = originalPrice,
                rating = rating,
            )
            // The list is re-read either way: a write that landed but was
            // answered oddly must show up rather than look lost.
            refresh()
            val reason = if (ok) null else repository.lastFailure
            saveFailure.value = reason
            onDone(ok, reason)
        }
    }

    fun deleteProduct(id: String, onDone: (String?) -> Unit = {}) {
        val session = repository.sessionHolder.session.value ?: run {
            onDone("no_session")
            return
        }
        viewModelScope.launch {
            val ok = repository.deleteProduct(session, id)
            // The list is re-read either way: a write that landed but was
            // answered oddly must show up rather than look lost.
            refresh()
            val reason = if (ok) null else repository.lastFailure
            saveFailure.value = reason
            onDone(reason)
        }
    }

    fun uploadImage(fileName: String, bytes: ByteArray, onDone: (String?) -> Unit) {
        val session = repository.sessionHolder.session.value ?: return
        viewModelScope.launch {
            onDone(repository.uploadImage(session, fileName, bytes))
        }
    }

    /**
     * Uploads a picked photo and stores its URL on the product, in one action.
     *
     * The form used to need two separate steps (copy the URL, paste it into a
     * field), and the upload it depends on was never called from the screen. A
     * single call removes both failure points: either the link lands on the
     * product row, or [onDone] receives null and the caller shows an error.
     *
     * An existing product is patched immediately and the list is refreshed. A
     * product that is still being created has no id yet, so the URL is only
     * returned to the form, which saves it together with the rest of the fields.
     */
    fun attachImage(
        productId: String?,
        fileName: String,
        bytes: ByteArray,
        onDone: (String?) -> Unit,
    ) {
        val session = repository.sessionHolder.session.value ?: return
        viewModelScope.launch {
            val url = repository.uploadImage(session, fileName, bytes)
            if (url != null && productId != null) {
                val product = _products.value.firstOrNull { it.id == productId }
                val saved = product != null && repository.upsertProduct(
                    session = session,
                    id = product.id,
                    name = product.name,
                    price = product.price,
                    description = product.description,
                    imageUrl = url,
                    category = product.category,
                    available = product.available,
                    sortOrder = product.sortOrder,
                )
                if (saved) refresh()
                onDone(if (saved) url else null)
                return@launch
            }
            onDone(url)
        }
    }

    fun saveSettings(values: Map<String, String>, onDone: (Boolean) -> Unit) {
        val session = repository.sessionHolder.session.value ?: run {
            onDone(false)
            return
        }
        viewModelScope.launch {
            val saved = runCatching { repository.updateSettings(session, values) }.getOrDefault(false)
            if (saved) _settings.value = (_settings.value.orEmpty() + values)
            onDone(saved)
        }
    }

    fun insertManualOrder(
        customer: String,
        phone: String,
        notes: String,
        items: List<Pair<String, Int>>,
        onDone: (Boolean, String?) -> Unit,
    ) {
        val session = repository.sessionHolder.session.value ?: run {
            onDone(false, "no_session")
            return
        }
        viewModelScope.launch {
            val ok = repository.insertManualOrder(session, customer, phone, notes, items)
            // The list is re-read either way: a write that landed but was
            // answered oddly must show up rather than look lost.
            refresh()
            val reason = if (ok) null else repository.lastFailure
            saveFailure.value = reason
            onDone(ok, reason)
        }
    }

}
