package com.zakeleo877.saeedadmin.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zakeleo877.saeedadmin.data.AdminAuthorization
import com.zakeleo877.saeedadmin.data.AdminRepository
import com.zakeleo877.saeedcore.data.remote.AdminSession
import com.zakeleo877.saeedcore.data.remote.AuthError
import com.zakeleo877.saeedcore.data.remote.AuthResult
import com.zakeleo877.saeedcore.model.Order
import com.zakeleo877.saeedcore.model.OrderStatus
import com.zakeleo877.saeedcore.model.Product
import java.math.BigDecimal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardStats(
    val newOrders: Int = 0,
    val todayOrders: Int = 0,
    val totalOrders: Int = 0,
    val productCount: Int = 0,
    val handedOverTotal: BigDecimal = BigDecimal.ZERO,
)

data class AdminUiState(
    val orders: List<Order> = emptyList(),
    val products: List<Product> = emptyList(),
    val statusFilter: OrderStatus? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class AdminViewModel(private val repository: AdminRepository) : ViewModel() {
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    private val _products = MutableStateFlow<List<Product>>(emptyList())
    private val _filter = MutableStateFlow<OrderStatus?>(null)
    private val _query = MutableStateFlow("")

    // The holder already exposes read-only state flows.
    val session: StateFlow<AdminSession?> = repository.sessionHolder.session
    val authorization: StateFlow<AdminAuthorization?> = repository.sessionHolder.authorization

    /** The loading flag and the error message travel together. */
    private data class LoadState(val loading: Boolean = false, val error: String? = null)

    private val _load = MutableStateFlow(LoadState())

    val uiState: StateFlow<AdminUiState> = combine(
        _orders, _products, _filter, _query, _load,
    ) { orders, products, filter, query, load ->
        AdminUiState(
            orders = orders.filter { order ->
                val matchesStatus = filter == null || order.status == filter
                val matchesQuery = query.isBlank() ||
                    order.number.toString().contains(query) ||
                    order.customer.contains(query, ignoreCase = true) ||
                    order.phone.contains(query)
                matchesStatus && matchesQuery
            },
            products = products,
            statusFilter = filter,
            searchQuery = query,
            isLoading = load.loading,
            errorMessage = load.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AdminUiState())

    init {
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
            _load.value = _load.value.copy(loading = false)
            if (result is AuthResult.Success) {
                refresh()
            }
            onResult(result)
        }
    }

    fun signOut() {
        repository.sessionHolder.clear()
        _orders.value = emptyList()
        _products.value = emptyList()
    }

    fun refresh() {
        val session = repository.sessionHolder.session.value ?: return
        viewModelScope.launch {
            _load.value = _load.value.copy(loading = true)
            val ordersResult = runCatching { repository.orders(session) }
            val productsResult = runCatching { repository.products(session) }
            _orders.value = ordersResult.getOrDefault(_orders.value)
            _products.value = productsResult.getOrDefault(_products.value)
            _load.value = _load.value.copy(error = if (ordersResult.isFailure && productsResult.isFailure) "load_failed" else null)
            _load.value = _load.value.copy(loading = false)
        }
    }

    fun setStatusFilter(status: OrderStatus?) {
        _filter.value = status
    }

    fun setSearchQuery(query: String) {
        _query.value = query
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus, onDone: (Boolean) -> Unit = {}) {
        val session = repository.sessionHolder.session.value ?: return
        viewModelScope.launch {
            val ok = repository.updateOrderStatus(session, orderId, status)
            if (ok) {
                _orders.value = _orders.value.map { if (it.id == orderId) it.copy(status = status) else it }
            }
            onDone(ok)
        }
    }

    fun saveProduct(
        id: String?,
        name: String,
        price: BigDecimal,
        description: String,
        imageUrl: String,
        category: String,
        available: Boolean,
        sortOrder: Int,
        onDone: (Boolean) -> Unit,
    ) {
        val session = repository.sessionHolder.session.value ?: return
        viewModelScope.launch {
            val ok = repository.upsertProduct(
                session, id, name, price, description, imageUrl, category, available, sortOrder,
            )
            if (ok) refresh()
            onDone(ok)
        }
    }

    fun deleteProduct(id: String, onDone: (Boolean) -> Unit = {}) {
        val session = repository.sessionHolder.session.value ?: return
        viewModelScope.launch {
            val ok = repository.deleteProduct(session, id)
            if (ok) refresh()
            onDone(ok)
        }
    }

    fun uploadImage(fileName: String, bytes: ByteArray, onDone: (String?) -> Unit) {
        val session = repository.sessionHolder.session.value ?: return
        viewModelScope.launch {
            onDone(repository.uploadImage(session, fileName, bytes))
        }
    }

    fun saveSettings(values: Map<String, String>, onDone: (Boolean) -> Unit) {
        val session = repository.sessionHolder.session.value ?: return
        viewModelScope.launch {
            onDone(repository.updateSettings(session, values))
        }
    }

    fun insertManualOrder(
        customer: String,
        phone: String,
        notes: String,
        items: List<Pair<String, Int>>,
        onDone: (Boolean) -> Unit,
    ) {
        val session = repository.sessionHolder.session.value ?: return
        viewModelScope.launch {
            val ok = repository.insertManualOrder(session, customer, phone, notes, items)
            if (ok) refresh()
            onDone(ok)
        }
    }

    fun stats(): DashboardStats {
        val orders = _orders.value
        val today = java.time.LocalDate.now()
        return DashboardStats(
            newOrders = orders.count { it.status == OrderStatus.NEW },
            todayOrders = orders.count {
                it.createdAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate() == today
            },
            totalOrders = orders.size,
            productCount = _products.value.size,
            handedOverTotal = orders
                .filter { it.status == OrderStatus.HANDED_OVER }
                .fold(BigDecimal.ZERO) { acc, order -> acc + order.total },
        )
    }
}
