package com.grocart.first.ui

import android.annotation.SuppressLint
import android.util.Log
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.grocart.first.data.InternetItem
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import com.grocart.first.data.CartItemResponse
import com.grocart.first.data.DataSource

/** Enum class to define available screens and their titles */
enum class GroAppScreen(val title: String) {
    Start("Home"),
    Item("Items"),
    Cart("Cart"),
    Orders("My Orders"),
    Profile("Edit Profile"),
    Category("Categories")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstApp(
    groViewModel: GroViewModel = viewModel(),
    navController: NavHostController = rememberNavController(),
) {
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    val user by groViewModel.user.collectAsState()
    val logoutClicked by groViewModel.logoutClicked.collectAsState()
    val cartItems by groViewModel.cartItems.collectAsState()
    val isGuest by groViewModel.isGuestSession.collectAsState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = GroAppScreen.valueOf(
        backStackEntry?.destination?.route ?: GroAppScreen.Start.name
    )

    // Sync dark mode status into the ViewModel so all screens can observe it
    val isDark = isSystemInDarkTheme()
    LaunchedEffect(isDark) { groViewModel.setDarkTheme(isDark) }

    // Properly observe navigation state instead of using a global variable
    val canNavigateBack = currentScreen != GroAppScreen.Start

    val showPaymentScreen by groViewModel.showPaymentScreen.collectAsState()

    if (user == null && !isGuest) {
        LoginUi(groViewModel = groViewModel)
    } else {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (!showPaymentScreen) {
                    FirstAppTopHeader(
                        currentScreen = currentScreen,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onProfileClick = { navController.navigate(GroAppScreen.Profile.name) },
                        onLogoutClick = { groViewModel.setLogoutClicked(true) },
                        canNavigateBack = canNavigateBack,
                        onNavigateUp = {
                            if (navController.previousBackStackEntry != null) {
                                navController.navigateUp()
                            } else {
                                navController.navigate(GroAppScreen.Start.name) {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                }
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (!showPaymentScreen) {
                    FirstAppBar(
                        navController = navController, 
                        currentScreen = currentScreen, 
                        cartItems = cartItems, 
                        groViewModel = groViewModel
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).background(getSeasonalGradient())) {
                // Seasonal overlay on top of the background but below/above content? We'll put it below NavHost so it acts as dynamic background, wait, no, the user wants colors ON screen.
                // Or we put it above NavHost but with pointer intercept? Canvas doesn't intercept by default. Let's place it above NavHost!
                NavHost(navController = navController, startDestination = GroAppScreen.Start.name) {
                    composable(route = GroAppScreen.Start.name) {
                        StartScreen(groViewModel = groViewModel, onCategoryClicked = { cat ->
                            groViewModel.updateSelectedCategory(cat)
                            navController.navigate(GroAppScreen.Item.name)
                        })
                    }
                    composable(route = GroAppScreen.Item.name) {
                        InternetItemScreen(groViewModel = groViewModel, itemUiState = groViewModel.itemUiState)
                    }
                    composable(route = GroAppScreen.Cart.name) {
                        CartScreen(groViewModel = groViewModel, onHomeButtonClicked = {
                            navController.navigate(GroAppScreen.Start.name) { popUpTo(0) }
                        })
                    }
                    composable(GroAppScreen.Orders.name) {
                        MyOrdersScreen(groViewModel = groViewModel)
                    }
                    composable(GroAppScreen.Profile.name) {
                        ProfileScreen(
                            groViewModel = groViewModel,
                            onNavigateBack = { navController.navigateUp() }
                        )
                    }
                    composable(GroAppScreen.Category.name) {
                        CategoryScreen(
                            groViewModel = groViewModel,
                            onCategoryClicked = { cat ->
                                groViewModel.updateSelectedCategory(cat)
                                navController.navigate(GroAppScreen.Item.name)
                            }
                        )
                    }
                }

                // Transparent Seasonal Overlay lightly overlaying all app screens
                SeasonalAnimationOverlay(groViewModel = groViewModel, modifier = Modifier.fillMaxSize())

                if (searchQuery.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        PredictiveResultList(
                            query = searchQuery,
                            groViewModel = groViewModel,
                            onItemClick = { item ->
                                searchQuery = "" // Reset search bar
                                val categoryList = DataSource.loadCategories()
                                val matchedCategory = categoryList.find { cat ->
                                    context.getString(cat.stringResourceId) == item.itemCategory
                                }

                                if (matchedCategory != null) {
                                    groViewModel.updateSelectedCategory(matchedCategory.stringResourceId)
                                    navController.navigate(GroAppScreen.Item.name)
                                } else {
                                    Log.e("GROCART_ERROR", "Category name ${item.itemCategory} not found in DataSource")
                                }
                            }
                        )
                    }
                }
            }

            if (logoutClicked) {
                AlertCheck(
                    onYesButtonPressed = {
                        groViewModel.setLogoutClicked(false)
                        groViewModel.clearData()
                    },
                    onNoButtonPressed = { groViewModel.setLogoutClicked(false) }
                )
            }
        }
    }
}

@Composable
fun PredictiveResultList(
    query: String,
    groViewModel: GroViewModel,
    onItemClick: (InternetItem) -> Unit
) {
    val filteredResults = groViewModel.getFilteredItems(query)

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(filteredResults) { item ->
            ListItem(
                headlineContent = { Text(item.itemName, fontWeight = FontWeight.Bold) },
                supportingContent = { Text("Category: ${item.itemCategory}") },
                trailingContent = { Text("₹${item.itemPrice}", color = Color(0xFF388E3C), fontWeight = FontWeight.Bold) },
                modifier = Modifier.clickable { onItemClick(item) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
        }
    }
}

class CurvedBottomBarShape(
    private val cutPosition: Float,
    private val cutRadius: Float,
    private val cornerRadius: Float
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            moveTo(0f, cornerRadius)
            quadraticTo(0f, 0f, cornerRadius, 0f)

            val startCut = cutPosition - cutRadius * 1.6f
            if (startCut > cornerRadius) {
                lineTo(startCut, 0f)
            } else {
                lineTo(cornerRadius, 0f)
            }

            val cutDepth = cutRadius * 1.5f

            cubicTo(
                cutPosition - cutRadius * 0.9f, 0f,
                cutPosition - cutRadius * 0.9f, cutDepth,
                cutPosition, cutDepth
            )
            cubicTo(
                cutPosition + cutRadius * 0.9f, cutDepth,
                cutPosition + cutRadius * 0.9f, 0f,
                cutPosition + cutRadius * 1.6f, 0f
            )

            lineTo(size.width - cornerRadius, 0f)
            quadraticTo(size.width, 0f, size.width, cornerRadius)

            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun FirstAppBar(
    navController: NavHostController,
    currentScreen: GroAppScreen,
    cartItems: List<CartItemResponse>,
    groViewModel: GroViewModel
) {
    val isGuest by groViewModel.isGuestSession.collectAsState()
    var showLoginPrompt by remember { mutableStateOf(false) }

    val tabs = listOf(
        GroAppScreen.Start to Icons.Filled.Home,
        GroAppScreen.Category to Icons.Filled.GridView,
        GroAppScreen.Cart to Icons.Filled.ShoppingCart,
        GroAppScreen.Orders to Icons.Filled.ShoppingBag,
        GroAppScreen.Profile to Icons.Filled.AccountCircle
    )

    // Fixed height for the bottom bar container to prevent infinite measurement loops
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(Color.Transparent)
    ) {
        val density = LocalDensity.current
        val cutRadiusPx = with(density) { 32.dp.toPx() }
        val cornerRadiusPx = with(density) { 20.dp.toPx() }
        val fabOffsetYPx = with(density) { 12.dp.toPx() }.toInt()
        val fabHalfWidthPx = with(density) { 28.dp.toPx() }.toInt()

        val widthPx = this.constraints.maxWidth.toFloat()
        val itemWidth = widthPx / tabs.size
        
        val selectedIndex = tabs.indexOfFirst { it.first == currentScreen }.takeIf { it >= 0 } ?: 0
        val cutPosition by animateFloatAsState(
            targetValue = (selectedIndex * itemWidth) + (itemWidth / 2f),
            animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow),
            label = "cutout"
        )
        
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(65.dp),
            shape = CurvedBottomBarShape(cutPosition, cutRadiusPx, cornerRadiusPx),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { (screen, icon) ->
                    val isSelected = currentScreen == screen
                    
                    IconButton(
                        onClick = {
                            if (isGuest && (screen == GroAppScreen.Orders || screen == GroAppScreen.Profile)) {
                                showLoginPrompt = true
                            } else {
                                navController.navigate(screen.name) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = screen.title,
                                tint = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp)
                            )
                            if (screen == GroAppScreen.Cart && cartItems.isNotEmpty()) {
                                Badge(
                                    containerColor = Color.Red,
                                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp)
                                ) {
                                    Text(cartItems.size.toString(), color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button for selected item
        Box(
            modifier = Modifier
                .offset { IntOffset(cutPosition.toInt() - fabHalfWidthPx, -fabOffsetYPx) }
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0xFF7C3AED))
                .clickable { /* Already on current screen */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tabs[selectedIndex].second,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }

    if (showLoginPrompt) {
        AlertDialog(
            onDismissRequest = { showLoginPrompt = false },
            title = { Text("Login Required") },
            text = { Text("Please login to access this feature.") },
            confirmButton = { TextButton(onClick = { groViewModel.endGuestSession(); showLoginPrompt = false }) { Text("Login") } },
            dismissButton = { TextButton(onClick = { showLoginPrompt = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AlertCheck(onYesButtonPressed: () -> Unit, onNoButtonPressed: () -> Unit) {
    AlertDialog(
        onDismissRequest = onNoButtonPressed,
        title = { Text("Logout?", fontWeight = FontWeight.ExtraBold) },
        text = { Text("Are you sure you want to logout?") },
        confirmButton = { TextButton(onClick = onYesButtonPressed) { Text("Yes") } },
        dismissButton = { TextButton(onClick = onNoButtonPressed) { Text("No") } }
    )
}

@Composable
fun FirstAppTopHeader(
    currentScreen: GroAppScreen,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    canNavigateBack: Boolean,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    var locationText by remember { mutableStateOf("Fetching location...") }

    // Reactive: updated explicitly after the permission dialog returns
    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Re-check after result so LaunchedEffect re-triggers
        locationPermissionGranted = permissions.values.any { it }
    }

    LaunchedEffect(locationPermissionGranted) {
        if (!locationPermissionGranted) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
            locationText = "Location Required"
        } else {
            locationText = "Fetching location..."
            withContext(Dispatchers.IO) {
                try {
                    val location = getBestLocation(context)
                    if (location != null) {
                        val addressText = reverseGeocode(context, location.latitude, location.longitude)
                        withContext(Dispatchers.Main) { locationText = addressText }
                    } else {
                        withContext(Dispatchers.Main) { locationText = "Please enable GPS" }
                    }
                } catch (e: Exception) {
                    Log.e("LOCATION", "Error: ${e.message}", e)
                    withContext(Dispatchers.Main) { locationText = "Location unavailable" }
                }
            }
        }
    }

    var menuExpanded by remember { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()
    val surfaceBg = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val searchBarBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF5F5F5)
    val hintColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray

    Surface(color = surfaceBg, shadowElevation = 0.dp) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.Transparent),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canNavigateBack) {
                    IconButton(onClick = onNavigateUp, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (currentScreen == GroAppScreen.Start) "Grocery in 10 minutes" else currentScreen.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = onSurface
                        )
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = onSurface)
                    }
                    Text(text = "Home - $locationText", fontSize = 12.sp, color = hintColor, maxLines = 1)
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            tint = Color.Gray,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Profile", fontWeight = FontWeight.Medium) },
                            onClick = {
                                menuExpanded = false
                                onProfileClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Logout", color = Color.Red, fontWeight = FontWeight.Bold) },
                            onClick = {
                                menuExpanded = false
                                onLogoutClick()
                            }
                        )
                    }
                }
            }

            if (currentScreen == GroAppScreen.Start || currentScreen == GroAppScreen.Item) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = searchBarBg),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = hintColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(fontSize = 16.sp, color = onSurface),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Search \"milk\", \"bread\"...", color = hintColor, fontSize = 16.sp)
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = hintColor)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Location helpers
// ---------------------------------------------------------------------------

/**
 * Returns the best available [Location].
 * 1. Tries cached last-known fixes from all enabled providers.
 * 2. If nothing is cached, requests a single fresh update (max 10 s wait).
 */
@SuppressLint("MissingPermission")
suspend fun getBestLocation(context: Context): Location? {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = locationManager.getProviders(true)

    // 1. Try cached fixes first
    var best: Location? = null
    for (provider in providers) {
        try {
            val loc = locationManager.getLastKnownLocation(provider) ?: continue
            if (best == null || loc.accuracy < best.accuracy) best = loc
        } catch (e: SecurityException) {
            Log.w("LOCATION", "Cached fix denied for $provider: ${e.message}")
        }
    }
    if (best != null) return best

    // 2. No cached fix — request a live single update (coroutine-suspended)
    val preferredProvider = when {
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> return null
    }

    return withTimeoutOrNull(10_000L) {
        suspendCancellableCoroutine { cont ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    if (cont.isActive) cont.resume(location)
                }
                // Required on API < 29
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {
                    if (cont.isActive) cont.resume(null)
                }
            }
            try {
                locationManager.requestSingleUpdate(preferredProvider, listener, null)
                cont.invokeOnCancellation { locationManager.removeUpdates(listener) }
            } catch (e: SecurityException) {
                Log.e("LOCATION", "Permission error requesting update: ${e.message}")
                cont.resume(null)
            }
        }
    }
}

/**
 * Converts [lat]/[lng] to a human-readable locality string.
 * Uses the new callback-based [Geocoder.getFromLocation] on API 33+ and
 * falls back to the (deprecated) synchronous version on older devices.
 */
suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): String {
    val geocoder = Geocoder(context, Locale.getDefault())

    fun formatAddress(addresses: List<Address>?): String {
        if (addresses.isNullOrEmpty()) return "Location not found"
        val addr = addresses[0]
        val locality = addr.subLocality ?: addr.locality ?: addr.adminArea ?: "Unknown"
        val state = addr.adminArea ?: ""
        return if (state.isNotBlank()) "$locality, $state" else locality
    }

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // API 33+ — callback-based, non-blocking
        suspendCancellableCoroutine { cont ->
            geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    if (cont.isActive) cont.resume(formatAddress(addresses))
                }
                override fun onError(errorMessage: String?) {
                    Log.e("GEOCODE", "GeocodeListener error: $errorMessage")
                    if (cont.isActive) cont.resume("Location unavailable")
                }
            })
        }
    } else {
        // API < 33 — run the blocking call off the main thread
        withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                formatAddress(addresses)
            } catch (e: Exception) {
                Log.e("GEOCODE", "Legacy geocode error: ${e.message}")
                "Location unavailable"
            }
        }
    }
}