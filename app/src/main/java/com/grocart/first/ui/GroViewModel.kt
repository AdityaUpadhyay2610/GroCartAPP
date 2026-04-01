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

class GroViewModel(private val sessionManager: SessionManager) : ViewModel() {
    
    private val auth = FirebaseAuth.getInstance()
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    // Auth error message for UI feedback
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()
    fun clearAuthError() { _authError.value = null }
    
    private val _user = MutableStateFlow<UserResponse?>(null)
    val user: StateFlow<UserResponse?> = _user.asStateFlow()

    private val _allItems = MutableStateFlow<List<InternetItem>>(emptyList())
    val allItems: StateFlow<List<InternetItem>> = _allItems.asStateFlow()

    private val _cartItems = MutableStateFlow<List<com.grocart.first.data.CartItemResponse>>(emptyList())
    val cartItems: StateFlow<List<com.grocart.first.data.CartItemResponse>> = _cartItems.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _isGuestSession = MutableStateFlow(false)
    val isGuestSession: StateFlow<Boolean> = _isGuestSession.asStateFlow()

    private val _logoutClicked = MutableStateFlow(false)
    val logoutClicked: StateFlow<Boolean> = _logoutClicked.asStateFlow()

    private val _showPaymentScreen = MutableStateFlow(false)
    val showPaymentScreen: StateFlow<Boolean> = _showPaymentScreen.asStateFlow()

    private val _paymentCountdown = MutableStateFlow(10)
    val paymentCountdown: StateFlow<Int> = _paymentCountdown.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _uiState = MutableStateFlow(GroUiState())
    val uiState: StateFlow<GroUiState> = _uiState.asStateFlow()

    var itemUiState: ItemUiState by mutableStateOf(ItemUiState.Loading)
        private set
    private val _animatingItem = MutableStateFlow<InternetItem?>(null)
    val animatingItem = _animatingItem.asStateFlow()

    sealed interface ItemUiState {
        data class Success(val items: List<InternetItem>) : ItemUiState
        object Error : ItemUiState
        object Loading : ItemUiState
    }

    init {
        checkExistingSession()
        // Fetch items immediately - no auth needed for public items
        viewModelScope.launch {
            launch { getFirstItem() }
            delay(500)
            launch { loadUserCart() }
        }
    }

    private suspend fun getIdToken(): String? {
        return try {
            auth.currentUser?.getIdToken(false)?.await()?.token
        } catch (e: Exception) {
            Log.e("AUTH_TOKEN", "Failed to get token: ${e.message}")
            null
        }
    }

    private fun checkExistingSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _user.value = UserResponse(id = currentUser.uid, username = currentUser.displayName ?: "User", email = currentUser.email ?: "")
            _isGuestSession.value = false
        }
    }

    fun triggerAddToCartAnimation(item: InternetItem) {
        _animatingItem.value = item
        viewModelScope.launch {
            delay(800)
            _animatingItem.value = null
        }
    }

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

    private fun parseItems(element: JsonElement?): List<JsonElement> {
        if (element == null || element is JsonNull) return emptyList()
        return when (element) {
            is JsonObject -> element.values.toList()
            is JsonArray -> element.filter { it !is JsonNull }
            else -> emptyList()
        }
    }

    fun placeOrder(total: Int) {
        val userId = _user.value?.id ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val token = getIdToken()
                val order = Order(items = _cartItems.value, timestamp = System.currentTimeMillis())
                val response = FirstApi.retrofitService.placeOrder(userId, order, token)
                if (response.isSuccessful) {
                    FirstApi.retrofitService.clearUserCart(userId, token)
                    withContext(Dispatchers.Main) {
                        Log.d("GROCART", "Firebase: Order placed and cart cleared!")
                        _cartItems.value = emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e("GROCART", "Order placement Failed: ${e.message}")
            }
        }
    }

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

    fun getFirstItem() {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { itemUiState = ItemUiState.Loading }
            try {
                // Items are public - try without auth first, fallback to auth token
                Log.d("ITEMS_LOAD", "Fetching items from Firebase Realtime Database...")
                var element: kotlinx.serialization.json.JsonElement? = null
                
                try {
                    // First try without auth (items should be public)
                    element = FirstApi.retrofitService.getItems(null)
                    Log.d("ITEMS_LOAD", "Fetched without auth. Response: $element")
                } catch (e: Exception) {
                    Log.w("ITEMS_LOAD", "Public fetch failed: ${e.message}, trying with auth token...")
                    // Fallback: try with auth token
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

    fun setLogoutClicked(v: Boolean) { _logoutClicked.value = v }
    fun endGuestSession() { _isGuestSession.value = false }
    fun startGuestSession() { _isGuestSession.value = true }
    fun proceedToPay() { _showPaymentScreen.value = true }
    fun cancelPayment() { _showPaymentScreen.value = false }
    fun setPaymentCountdown(v: Int) { _paymentCountdown.value = v }
    fun updateSelectedCategory(cat: Int) { _uiState.update { it.copy(selectedCategory = cat) } }
    fun updateClickText(t: String) { _uiState.update { it.copy(clickStatus = t) } }

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
                    // Re-fetch items in case they failed before login
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

    fun logout() {
        auth.signOut()
        sessionManager.logout()
        _user.value = null
        _cartItems.value = emptyList()
        _isGuestSession.value = false
        _logoutClicked.value = false
        _orders.value = emptyList()
    }

    fun clearData() { logout() }

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
                    // Re-fetch items after registration
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

    fun completePayment() {
        if (_cartItems.value.isNotEmpty()) {
            val order = Order(items = _cartItems.value, timestamp = System.currentTimeMillis())
            _orders.update { it + order }
            _cartItems.value = emptyList()
        }
        _showPaymentScreen.value = false
    }

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
}