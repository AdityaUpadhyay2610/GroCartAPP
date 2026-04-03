package com.grocart.first.ui

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.grocart.first.data.InternetItem
import com.grocart.first.data.Order
import com.grocart.first.data.SessionManager
import com.grocart.first.network.FirstApi
import com.grocart.first.network.UserResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.*
import com.grocart.first.data.PaymentMethod
import com.grocart.first.data.CouponOffer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.ui.graphics.vector.ImageVector

class GroViewModel(private val sessionManager: SessionManager) : ViewModel() {

    // ════════════════════════════════════════════════════════════════════════════
    //  CORE / SHARED — Firebase instance, JSON parser, UI state classes
    //  Used across multiple screens
    // ════════════════════════════════════════════════════════════════════════════

    private val auth = FirebaseAuth.getInstance()
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    sealed interface ItemUiState {
        data class Success(val items: List<InternetItem>) : ItemUiState
        object Error : ItemUiState
        object Loading : ItemUiState
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  STATE — All StateFlows grouped by their primary screen usage
    // ════════════════════════════════════════════════════════════════════════════

    // --- LoginUi / Auth state ---
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _user = MutableStateFlow<UserResponse?>(null)
    val user: StateFlow<UserResponse?> = _user.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _isGuestSession = MutableStateFlow(false)
    val isGuestSession: StateFlow<Boolean> = _isGuestSession.asStateFlow()

    // --- ItemScreen / StartScreen state ---
    private val _allItems = MutableStateFlow<List<InternetItem>>(emptyList())
    val allItems: StateFlow<List<InternetItem>> = _allItems.asStateFlow()

    var itemUiState: ItemUiState by mutableStateOf(ItemUiState.Loading)
        private set

    private val _animatingItem = MutableStateFlow<InternetItem?>(null)
    val animatingItem = _animatingItem.asStateFlow()

    private val _uiState = MutableStateFlow(GroUiState())
    val uiState: StateFlow<GroUiState> = _uiState.asStateFlow()

    // --- CartScreen state ---
    private val _cartItems = MutableStateFlow<List<com.grocart.first.data.CartItemResponse>>(emptyList())
    val cartItems: StateFlow<List<com.grocart.first.data.CartItemResponse>> = _cartItems.asStateFlow()

    private val _showPaymentScreen = MutableStateFlow(false)
    val showPaymentScreen: StateFlow<Boolean> = _showPaymentScreen.asStateFlow()

    private val _paymentCountdown = MutableStateFlow(10)
    val paymentCountdown: StateFlow<Int> = _paymentCountdown.asStateFlow()

    // --- MyOrdersScreen state ---
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    // --- FirstApp (Navigation / Logout dialog) state ---
    private val _logoutClicked = MutableStateFlow(false)
    val logoutClicked: StateFlow<Boolean> = _logoutClicked.asStateFlow()

    // --- Payment / Checkout state ---
    private val _selectedPaymentMethod = MutableStateFlow<PaymentMethod?>(null)
    val selectedPaymentMethod: StateFlow<PaymentMethod?> = _selectedPaymentMethod.asStateFlow()

    private val _appliedCoupon = MutableStateFlow<CouponOffer?>(null)
    val appliedCoupon: StateFlow<CouponOffer?> = _appliedCoupon.asStateFlow()

    val couponOffers = listOf(
        CouponOffer("SAVE10", "10% off on orders above ₹200", 10, 200),
        CouponOffer("FRESH20", "20% off on orders above ₹500", 20, 500),
        CouponOffer("GROCART5", "Flat 5% off on all orders", 5, 0),
        CouponOffer("FIRST50", "50% off — Max ₹100 (New User)", 50, 100),
        CouponOffer("FESTIVE15", "Festive 15% off above ₹300", 15, 300)
    )

    // ════════════════════════════════════════════════════════════════════════════
    //  INIT — App startup: checks session, fetches items & user data
    // ════════════════════════════════════════════════════════════════════════════

    init {
        checkExistingSession()
        viewModelScope.launch {
            // Load items and sync user data concurrently
            launch { getFirstItem() }
            if (_user.value != null) {
                launch { loadUserCart() }
                launch { loadOrders() }
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  INTERNAL HELPERS — Used internally by other methods
    // ════════════════════════════════════════════════════════════════════════════

    /** Gets Firebase ID token for authenticated API calls */
    private suspend fun getIdToken(): String? {
        return try {
            auth.currentUser?.getIdToken(false)?.await()?.token
        } catch (e: Exception) {
            Log.e("AUTH_TOKEN", "Failed to get token: ${e.message}")
            null
        }
    }

    /** Checks if user is already logged in via Firebase on app startup */
    private fun checkExistingSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _user.value = UserResponse(id = currentUser.uid, username = currentUser.displayName ?: "User", email = currentUser.email ?: "")
            _isGuestSession.value = false
        }
    }

    /** Parses Firebase JSON response (handles both Object and Array formats) */
    private fun parseItems(element: JsonElement?): List<JsonElement> {
        if (element == null || element is JsonNull) return emptyList()
        return when (element) {
            is JsonObject -> element.values.toList()
            is JsonArray -> element.filter { it !is JsonNull }
            else -> emptyList()
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  LOGIN SCREEN (LoginUi.kt)
    //  Methods: login(), register(), clearAuthError(), startGuestSession()
    // ════════════════════════════════════════════════════════════════════════════

    /** Clears authentication error message from UI */
    fun clearAuthError() { _authError.value = null }

    /** Logs in user via Firebase Auth with email/password */
    fun login(e: String, p: String) {
        _loading.value = true
        _authError.value = null
        Log.d("LOGIN", "Attempting login with email: $e")
        auth.signInWithEmailAndPassword(e, p).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                if (user != null) {
                    Log.d("LOGIN", "Login successful! UID: ${user.uid}")
                    _user.value = UserResponse(id = user.uid, username = user.displayName ?: "User", email = user.email ?: "")
                    loadUserCart()
                    loadOrders()
                    getFirstItem()
                }
            } else {
                val errorMsg = task.exception?.message ?: "Login failed"
                Log.e("LOGIN", "Login failed: $errorMsg")
                _authError.value = errorMsg
            }
            _loading.value = false
        }
    }

    /** Registers a new user via Firebase Auth and sets display name */
    fun register(u: String, e: String, p: String) {
        _loading.value = true
        _authError.value = null
        Log.d("REGISTER", "Attempting registration for: $e")
        auth.createUserWithEmailAndPassword(e, p).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                Log.d("REGISTER", "Registration successful! UID: ${user?.uid}")
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(u)
                    .build()

                user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                    _user.value = UserResponse(id = user.uid, username = u, email = e)
                    loadUserCart()
                    getFirstItem()
                    _loading.value = false
                }
            } else {
                val errorMsg = task.exception?.message ?: "Registration failed"
                Log.e("REGISTER", "Registration failed: $errorMsg")
                _authError.value = errorMsg
                _loading.value = false
            }
        }
    }

    /** Allows user to browse without logging in */
    fun startGuestSession() { _isGuestSession.value = true }

    // ════════════════════════════════════════════════════════════════════════════
    //  ITEM SCREEN (ItemScreen.kt) & START SCREEN (StartScreen.kt)
    //  Methods: getFirstItem(), getFilteredItems(), triggerAddToCartAnimation(),
    //           addToCart(), updateClickText(), updateSelectedCategory()
    // ════════════════════════════════════════════════════════════════════════════

    /** Fetches all grocery items from Firebase Realtime Database */
    fun getFirstItem() {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { itemUiState = ItemUiState.Loading }
            try {
                Log.d("ITEMS_LOAD", "Fetching items from Firebase Realtime Database...")
                var element: kotlinx.serialization.json.JsonElement? = null

                try {
                    element = FirstApi.retrofitService.getItems(null)
                    Log.d("ITEMS_LOAD", "Fetched without auth. Response: $element")
                } catch (e: Exception) {
                    Log.w("ITEMS_LOAD", "Public fetch failed: ${e.message}, trying with auth token...")
                    val token = getIdToken()
                    Log.d("ITEMS_LOAD", "Auth token available: ${token != null}")
                    element = FirstApi.retrofitService.getItems(token)
                    Log.d("ITEMS_LOAD", "Fetched with auth. Response: $element")
                }

                val rawItems = parseItems(element)
                Log.d("ITEMS_LOAD", "Parsed ${rawItems.size} raw items from response")

                val items = rawItems.mapNotNull { jsonItem ->
                    try {
                        val decoded = json.decodeFromJsonElement<InternetItem>(jsonItem)
                        Log.d("ITEMS_LOAD", "Decoded item: ${decoded.itemName} (${decoded.itemCategory})")
                        decoded
                    } catch (e: Exception) {
                        Log.e("ITEMS_LOAD", "Failed to decode item: $jsonItem, error: ${e.message}")
                        null
                    }
                }

                Log.d("ITEMS_LOAD", "Total items loaded: ${items.size}")
                withContext(Dispatchers.Main) {
                    itemUiState = ItemUiState.Success(items)
                    _allItems.value = items
                }
            } catch (e: Exception) {
                Log.e("ITEMS_LOAD", "FAILED to load items: ${e.message}", e)
                withContext(Dispatchers.Main) { itemUiState = ItemUiState.Error }
            }
        }
    }

    /** Filters items by search query — used in FirstApp.kt SearchBar */
    fun getFilteredItems(query: String): List<InternetItem> {
        return if (query.trim().isEmpty()) {
            _allItems.value
        } else {
            _allItems.value.filter {
                it.itemName.contains(query, ignoreCase = true) ||
                        it.itemCategory.contains(query, ignoreCase = true)
            }
        }
    }

    /** Triggers fly-to-cart animation on item add — used in ItemScreen & StartScreen */
    fun triggerAddToCartAnimation(item: InternetItem) {
        _animatingItem.value = item
        viewModelScope.launch {
            delay(800)
            _animatingItem.value = null
        }
    }

    /** Adds an item to user's Firebase cart — used in ItemScreen, StartScreen & CartScreen (+) */
    fun addToCart(item: InternetItem) {
        val userId = _user.value?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val token = getIdToken()
                val existing = _cartItems.value.find { it.id == item.id }
                val cartItem = com.grocart.first.data.CartItemResponse(
                    id = item.id,
                    itemName = item.itemName,
                    itemPrice = item.itemPrice,
                    imageUrl = item.imageUrl,
                    quantity = (existing?.quantity ?: 0) + 1
                )

                val response = FirstApi.retrofitService.addCartItem(userId, item.id, cartItem, token)
                if (response.isSuccessful) {
                    loadUserCart()
                }
            } catch (e: Exception) {
                Log.e("GROCART_DEBUG", "Add Cart error: ${e.message}")
            }
        }
    }

    /** Updates the selected category label — used in CategoryScreen & StartScreen */
    fun updateClickText(t: String) { _uiState.update { it.copy(clickStatus = t) } }

    /** Updates selected category tab — used in FirstApp, CategoryScreen */
    fun updateSelectedCategory(cat: Int) { _uiState.update { it.copy(selectedCategory = cat) } }

    // ════════════════════════════════════════════════════════════════════════════
    //  CART SCREEN (CartScreen.kt)
    //  Methods: loadUserCart(), decreaseItemCount(), proceedToPay(),
    //           cancelPayment(), setPaymentCountdown(), completePayment(),
    //           placeOrder()
    // ════════════════════════════════════════════════════════════════════════════

    /** Loads user's cart from Firebase — also called after login/register */
    fun loadUserCart() {
        val userId = _user.value?.id ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val token = getIdToken()
                val response = FirstApi.retrofitService.getUserCart(userId, token)
                if (response.isSuccessful) {
                    val element = response.body()
                    val items = parseItems(element)

                    val decodedItems = items.mapNotNull {
                        try {
                            json.decodeFromJsonElement<com.grocart.first.data.CartItemResponse>(it)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    _cartItems.update { decodedItems }
                    Log.d("GROCART_DEBUG", "Cart Synced from Firebase: ${decodedItems.size}")
                } else {
                    Log.e("GROCART_DEBUG", "Cart sync failed: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("GROCART_DEBUG", "Cart sync failed: ${e.message}")
            }
        }
    }

    /** Decreases item quantity or removes it from cart if quantity is 1 */
    fun decreaseItemCount(item: com.grocart.first.data.CartItemResponse) {
        val userId = _user.value?.id ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val token = getIdToken()
                val currentQuantity = item.quantity
                if (currentQuantity <= 1) {
                    FirstApi.retrofitService.decreaseCartItem(userId, item.id, token)
                } else {
                    val updatedItem = item.copy(quantity = currentQuantity - 1)
                    FirstApi.retrofitService.addCartItem(userId, item.id, updatedItem, token)
                }
                loadUserCart()
            } catch (e: Exception) {
                Log.e("GROCART_DEBUG", "Decrease Cart error: ${e.message}")
            }
        }
    }

    /** Opens the payment/checkout overlay */
    fun proceedToPay() { _showPaymentScreen.value = true }

    /** Cancels payment and closes the overlay */
    fun cancelPayment() { _showPaymentScreen.value = false }

    /** Updates the countdown timer shown during payment */
    fun setPaymentCountdown(v: Int) { _paymentCountdown.value = v }

    /** Completes payment locally — clears cart and adds order to local list */
    fun completePayment() {
        val currentItems = _cartItems.value.toList()
        if (currentItems.isNotEmpty()) {
            val order = Order(items = currentItems, timestamp = System.currentTimeMillis())
            _orders.update { it + order }
            Log.d("GROCART", "Order added to local list. Total orders: ${_orders.value.size}")
        }
        _cartItems.value = emptyList()
        _showPaymentScreen.value = false
    }

    /** Places order on Firebase — clears remote cart and reloads orders */
    fun placeOrder(total: Int) {
        val userId = _user.value?.id ?: return
        val itemsToOrder = _cartItems.value.toList()
        if (itemsToOrder.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val token = getIdToken()
                val order = Order(items = itemsToOrder, timestamp = System.currentTimeMillis())
                val response = FirstApi.retrofitService.placeOrder(userId, order, token)
                if (response.isSuccessful) {
                    FirstApi.retrofitService.clearUserCart(userId, token)
                    withContext(Dispatchers.Main) {
                        Log.d("GROCART", "Firebase: Order placed and cart cleared!")
                        _cartItems.value = emptyList()
                    }
                    loadOrders()
                }
            } catch (e: Exception) {
                Log.e("GROCART", "Order placement Failed: ${e.message}")
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  MY ORDERS SCREEN (MyOrdersScreen.kt)
    //  Methods: loadOrders()
    // ════════════════════════════════════════════════════════════════════════════

    /** Loads order history from Firebase */
    fun loadOrders() {
        val userId = _user.value?.id ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val token = getIdToken()
                val element = FirstApi.retrofitService.getOrders(userId, token)
                val rawOrders = parseItems(element)
                val decodedOrders = rawOrders.mapNotNull {
                    try {
                        json.decodeFromJsonElement<Order>(it)
                    } catch (e: Exception) {
                        Log.e("GROCART", "Failed to decode order: ${e.message}")
                        null
                    }
                }
                withContext(Dispatchers.Main) {
                    _orders.value = decodedOrders
                    Log.d("GROCART", "Orders loaded from Firebase: ${decodedOrders.size}")
                }
            } catch (e: Exception) {
                Log.e("GROCART", "Failed to load orders: ${e.message}")
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    //  FIRST APP / NAVIGATION (FirstApp.kt) & PROFILE SCREEN (ProfileScreen.kt)
    //  Methods: setLogoutClicked(), endGuestSession(), logout(), clearData()
    // ════════════════════════════════════════════════════════════════════════════

    /** Toggles logout confirmation dialog visibility */
    fun setLogoutClicked(v: Boolean) { _logoutClicked.value = v }

    /** Ends guest session and redirects to login — used in FirstApp.kt */
    fun endGuestSession() { _isGuestSession.value = false }

    /** Signs out from Firebase, clears all local state */
    fun logout() {
        auth.signOut()
        sessionManager.logout()
        _user.value = null
        _cartItems.value = emptyList()
        _isGuestSession.value = false
        _logoutClicked.value = false
        _orders.value = emptyList()
    }

    /** Alias for logout — used in FirstApp.kt logout dialog */
    fun clearData() { logout() }

    // ════════════════════════════════════════════════════════════════════════════
    //  PAYMENT HELPERS
    // ════════════════════════════════════════════════════════════════════════════

    /** Sets the selected payment method */
    fun setPaymentMethod(method: PaymentMethod?) { _selectedPaymentMethod.value = method }

    /** Applies a coupon if it exists and meets minimum order criteria */
    fun applyCoupon(code: String, totalPrice: Int): String? {
        val found = couponOffers.find { it.code.equals(code.trim(), ignoreCase = true) }
        return when {
            found == null -> "Invalid coupon code"
            totalPrice < found.minOrder -> "Minimum order ₹${found.minOrder} required"
            else -> {
                _appliedCoupon.value = found
                null // No error
            }
        }
    }

    /** Removes the currently applied coupon */
    fun removeCoupon() { _appliedCoupon.value = null }

    /** Safely resolves icons for payment methods using explicit imports to prevent ANR */
    fun getPaymentIcon(method: PaymentMethod): ImageVector = when (method) {
        PaymentMethod.CARD -> Icons.Default.CreditCard
        PaymentMethod.COD -> Icons.Default.LocalShipping
        PaymentMethod.UPI -> Icons.Default.Phone
        PaymentMethod.WALLET -> Icons.Default.AccountBalanceWallet
    }
}