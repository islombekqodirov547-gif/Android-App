package com.example.storemobile.ui.seller

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.storemobile.data.ApiResult
import com.example.storemobile.data.SessionManager
import com.example.storemobile.data.StoreRepository
import com.example.storemobile.data.model.CartLine
import com.example.storemobile.data.model.Client
import com.example.storemobile.data.model.Order
import com.example.storemobile.data.model.OrderItemRequest
import com.example.storemobile.data.model.OrderRequest
import com.example.storemobile.data.model.Product
import com.example.storemobile.data.model.SaleMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SellerUiState(
    val products: List<Product> = emptyList(),
    val productsLoading: Boolean = false,
    val productsError: String? = null,
    val refreshing: Boolean = false,
    val search: String = "",

    val cart: List<CartLine> = emptyList(),

    val clients: List<Client> = emptyList(),

    val history: List<Order> = emptyList(),
    val historyLoading: Boolean = false,
    val historyError: String? = null,

    val sending: Boolean = false,
    val toast: String? = null
) {
    val filteredProducts: List<Product>
        get() = if (search.isBlank()) products
        else products.filter { it.name.contains(search.trim(), ignoreCase = true) }

    val cartTotal: Double get() = cart.sumOf { it.lineTotal }
    val cartCount: Int get() = cart.size
    val cartPieces: Int get() = cart.sumOf { it.pieces }
}

class SellerViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = StoreRepository()
    private val session = SessionManager(app)

    private val _ui = MutableStateFlow(SellerUiState())
    val ui: StateFlow<SellerUiState> = _ui.asStateFlow()

    /** App-wide theme preference ("system" | "light" | "dark"). */
    val themeMode: StateFlow<String> = session.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SessionManager.THEME_SYSTEM
    )

    fun setThemeMode(mode: String) {
        viewModelScope.launch { session.saveThemeMode(mode) }
    }

    var userId: Int = -1
    var userName: String = "Sotuvchi"
    private var started = false

    fun start(userId: Int, userName: String) {
        if (started && this.userId == userId) return
        started = true
        this.userId = userId
        this.userName = userName
        // Fresh state for a new seller (also clears any leftover cart).
        _ui.value = SellerUiState()
        loadProducts()
        loadClients()
        loadHistory()
    }

    /* ───────── Products ───────── */

    fun loadProducts() {
        _ui.value = _ui.value.copy(productsLoading = true, productsError = null)
        viewModelScope.launch {
            when (val r = repo.getProducts()) {
                is ApiResult.Success -> _ui.value = _ui.value.copy(
                    productsLoading = false,
                    products = r.data.sortedBy { it.name }
                )
                is ApiResult.Error -> _ui.value = _ui.value.copy(
                    productsLoading = false,
                    productsError = r.message
                )
            }
        }
    }

    fun setSearch(value: String) {
        _ui.value = _ui.value.copy(search = value)
    }

    /**
     * Manual "pull to refresh" via the refresh button. Keeps the current grid
     * visible (uses [refreshing] instead of the full-screen loader) and quietly
     * re-fetches products + clients so newly added products / stock appear.
     */
    fun refreshProducts() {
        if (_ui.value.refreshing) return
        _ui.value = _ui.value.copy(refreshing = true)
        viewModelScope.launch {
            when (val r = repo.getProducts()) {
                is ApiResult.Success -> _ui.value = _ui.value.copy(
                    products = r.data.sortedBy { it.name },
                    productsError = null
                )
                is ApiResult.Error ->
                    if (_ui.value.products.isEmpty())
                        _ui.value = _ui.value.copy(productsError = r.message)
            }
            _ui.value = _ui.value.copy(refreshing = false)
            loadClients()
        }
    }

    /**
     * Silent background refresh used by the lightweight auto-poll while the
     * Products tab is open. No spinners, no error toasts — it just updates the
     * list in place when the admin adds a product / makes a stock entry. Only
     * the small products endpoint is hit, so it is gentle on the server.
     */
    fun autoRefreshProducts() {
        if (_ui.value.refreshing || _ui.value.productsLoading) return
        viewModelScope.launch {
            when (val r = repo.getProducts()) {
                is ApiResult.Success -> _ui.value = _ui.value.copy(
                    products = r.data.sortedBy { it.name },
                    productsError = null
                )
                is ApiResult.Error -> { /* stay quiet on transient failures */ }
            }
        }
    }

    private fun loadClients() {
        viewModelScope.launch {
            when (val r = repo.getClients()) {
                is ApiResult.Success -> _ui.value = _ui.value.copy(clients = r.data)
                is ApiResult.Error -> {}
            }
        }
    }

    fun addClient(name: String, phone: String?, onResult: (Client?) -> Unit) {
        viewModelScope.launch {
            when (val r = repo.createClient(name, phone)) {
                is ApiResult.Success -> {
                    _ui.value = _ui.value.copy(clients = _ui.value.clients + r.data)
                    onResult(r.data)
                }
                is ApiResult.Error -> {
                    _ui.value = _ui.value.copy(toast = r.message)
                    onResult(null)
                }
            }
        }
    }

    /* ───────── Cart ───────── */

    /** Adds (or increases) a cart line. Quantity is capped by available stock. */
    fun addToCart(product: Product, mode: SaleMode, count: Int) {
        if (count <= 0) return
        val list = _ui.value.cart.toMutableList()
        val key = "${product.id}-${mode.name}"
        val idx = list.indexOfFirst { it.key == key }
        val maxCount = if (mode == SaleMode.BLOCK && product.quantityInBlock > 0)
            product.totalPieces / product.quantityInBlock else product.totalPieces

        if (idx >= 0) {
            val current = list[idx]
            val newCount = (current.count + count).coerceAtMost(maxCount.coerceAtLeast(1))
            list[idx] = current.copy(count = newCount)
        } else {
            list.add(CartLine(product, mode, count.coerceAtMost(maxCount.coerceAtLeast(1))))
        }
        _ui.value = _ui.value.copy(cart = list, toast = "${product.name} savatga qo'shildi")
    }

    fun changeCartCount(line: CartLine, delta: Int) {
        val list = _ui.value.cart.toMutableList()
        val idx = list.indexOfFirst { it.key == line.key }
        if (idx < 0) return
        val current = list[idx]
        val maxCount = if (current.mode == SaleMode.BLOCK && current.product.quantityInBlock > 0)
            current.product.totalPieces / current.product.quantityInBlock else current.product.totalPieces
        val newCount = current.count + delta
        when {
            newCount <= 0 -> list.removeAt(idx)
            newCount > maxCount -> {
                list[idx] = current.copy(count = maxCount.coerceAtLeast(1))
                _ui.value = _ui.value.copy(toast = "Omborda yetarli emas")
            }
            else -> list[idx] = current.copy(count = newCount)
        }
        _ui.value = _ui.value.copy(cart = list)
    }

    fun removeCartLine(line: CartLine) {
        _ui.value = _ui.value.copy(cart = _ui.value.cart.filterNot { it.key == line.key })
    }

    fun clearCart() {
        _ui.value = _ui.value.copy(cart = emptyList())
    }

    /* ───────── Send order ───────── */

    fun sendOrder(client: Client?, onDone: (Boolean) -> Unit) {
        val cart = _ui.value.cart
        if (cart.isEmpty()) return
        _ui.value = _ui.value.copy(sending = true)
        val request = OrderRequest(
            clientId = client?.id,
            userId = if (userId > 0) userId else null,
            totalSum = cart.sumOf { it.lineTotal },
            items = cart.map {
                OrderItemRequest(
                    productId = it.product.id,
                    quantity = it.pieces,
                    price = it.pricePerPiece
                )
            }
        )
        viewModelScope.launch {
            when (val r = repo.createOrder(request)) {
                is ApiResult.Success -> {
                    _ui.value = _ui.value.copy(
                        sending = false,
                        cart = emptyList(),
                        toast = "Buyurtma kassirga yuborildi"
                    )
                    loadProducts()
                    onDone(true)
                }
                is ApiResult.Error -> {
                    _ui.value = _ui.value.copy(sending = false, toast = r.message)
                    onDone(false)
                }
            }
        }
    }

    /* ───────── History (my sales) ───────── */

    fun loadHistory() {
        _ui.value = _ui.value.copy(historyLoading = true, historyError = null)
        viewModelScope.launch {
            val mine = mutableListOf<Order>()
            when (val h = repo.getHistory()) {
                is ApiResult.Success -> mine += h.data.filter { it.user?.id == userId }
                is ApiResult.Error -> {
                    _ui.value = _ui.value.copy(historyLoading = false, historyError = h.message)
                    return@launch
                }
            }
            when (val p = repo.getPendingOrders()) {
                is ApiResult.Success -> mine += p.data.filter { it.user?.id == userId }
                is ApiResult.Error -> {}
            }
            _ui.value = _ui.value.copy(
                historyLoading = false,
                history = mine.sortedByDescending { it.createdAt }
            )
        }
    }

    fun consumeToast() {
        _ui.value = _ui.value.copy(toast = null)
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            session.clearSession()
            onDone()
        }
    }
}
