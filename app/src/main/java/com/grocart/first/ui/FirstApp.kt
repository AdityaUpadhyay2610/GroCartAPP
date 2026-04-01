package com.grocart.first.ui

import android.util.Log
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.grocart.first.data.InternetItem
import com.grocart.first.R
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
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

var canNavigateBack = false

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

    canNavigateBack = navController.previousBackStackEntry != null

    if (user == null && !isGuest) {
        LoginUi(groViewModel = groViewModel)
    } else {
        Scaffold(
            topBar = {
                FirstAppTopHeader(
                    currentScreen = currentScreen,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onProfileClick = { navController.navigate(GroAppScreen.Profile.name) },
                    onLogoutClick = { groViewModel.setLogoutClicked(true) },
                    canNavigateBack = canNavigateBack,
                    onNavigateUp = { navController.navigateUp() }
                )
            },
            bottomBar = {
                FirstAppBar(navController = navController, currentScreen = currentScreen, cartItems = cartItems, groViewModel = groViewModel)
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
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
            quadraticBezierTo(0f, 0f, cornerRadius, 0f)

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
            quadraticBezierTo(size.width, 0f, size.width, cornerRadius)

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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(Color.Transparent) 
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val itemWidth = widthPx / tabs.size
        
        val selectedIndex = tabs.indexOfFirst { it.first == currentScreen }.takeIf { it >= 0 } ?: 0
        val cutPosition by animateFloatAsState(
            targetValue = (selectedIndex * itemWidth) + (itemWidth / 2f),
            animationSpec = spring(dampingRatio = 0.65f, stiffness = androidx.compose.animation.core.Spring.StiffnessLow),
            label = "cutout"
        )
        
        val cutRadiusPx = with(LocalDensity.current) { 30.dp.toPx() }
        val cornerRadiusPx = with(LocalDensity.current) { 24.dp.toPx() }
        
        // Background shape
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
                .graphicsLayer {
                    shape = CurvedBottomBarShape(cutPosition, cutRadiusPx, cornerRadiusPx)
                    clip = true
                }
                .background(Color(0xFF43A047))
        )
        
        // The unselected icons
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, pair ->
                val (screen, icon) = pair
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (screen == GroAppScreen.Orders || screen == GroAppScreen.Profile) {
                                if (isGuest) {
                                    showLoginPrompt = true
                                    return@clickable
                                }
                            }
                            navController.navigate(screen.name) {
                                if (screen == GroAppScreen.Start) popUpTo(0)
                                launchSingleTop = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (!isSelected) {
                        BadgedBox(
                            badge = {
                                if (screen == GroAppScreen.Cart && cartItems.isNotEmpty()) {
                                    Badge(containerColor = Color.Red, modifier = Modifier.offset((-8).dp, 8.dp)) { Text(cartItems.size.toString(), color = Color.White) }
                                }
                            }
                        ) {
                            Icon(imageVector = icon, contentDescription = screen.title, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
        
        // The floating circle (selected icon)
        Box(
            modifier = Modifier
                .offset { IntOffset((cutPosition - 30.dp.toPx()).toInt(), (-2).dp.toPx().toInt()) }
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            val (selectedScreen, selectedIcon) = tabs[selectedIndex]
            BadgedBox(
                badge = {
                    if (selectedScreen == GroAppScreen.Cart && cartItems.isNotEmpty()) {
                        Badge(containerColor = Color.Red, modifier = Modifier.offset((-4).dp, 4.dp)) { Text(cartItems.size.toString(), color = Color.White) }
                    }
                }
            ) {
                Icon(imageVector = selectedIcon, contentDescription = "Selected", tint = Color(0xFF43A047), modifier = Modifier.size(28.dp))
            }
        }
    }

    if (showLoginPrompt) {
        AlertDialog(
            onDismissRequest = { showLoginPrompt = false },
            title = { Text("Login Required") },
            text = { Text("Please login to access this section.") },
            confirmButton = { TextButton(onClick = { groViewModel.endGuestSession(); showLoginPrompt = false }) { Text("Login") } },
            dismissButton = { TextButton(onClick = { showLoginPrompt = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AppNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Icon(icon, label)
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
    var locationPermissionGranted by remember { 
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        locationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || 
                                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    LaunchedEffect(locationPermissionGranted) {
        if (!locationPermissionGranted) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
            locationText = "Location Required"
        } else {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val providers = locationManager.getProviders(true)
                var bestLocation: Location? = null
                for (provider in providers) {
                    val l = locationManager.getLastKnownLocation(provider) ?: continue
                    if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                        bestLocation = l
                    }
                }
                
                if (bestLocation != null) {
                    withContext(Dispatchers.IO) {
                        try {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            val addresses = geocoder.getFromLocation(bestLocation.latitude, bestLocation.longitude, 1)
                            if (addresses != null && addresses.isNotEmpty()) {
                                val address = addresses[0]
                                locationText = "${address.subLocality ?: address.locality ?: "Unknown"}, ${address.adminArea ?: ""}"
                            } else {
                                locationText = "Location not found"
                            }
                        } catch (e: Exception) {
                            locationText = "Location unavailable"
                        }
                    }
                } else {
                    locationText = "Please enable GPS"
                }
            } catch (e: SecurityException) {
                locationText = "Permission denied"
            }
        }
    }

    var menuExpanded by remember { mutableStateOf(false) }

    Surface(color = Color.White, shadowElevation = 0.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            // Location and Profile Row
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
                            fontSize = 18.sp
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Drop Down",
                            tint = Color.Black
                        )
                    }
                    Text(
                        text = "Home - $locationText",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
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
                        containerColor = Color.White
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

            // Search Bar (Only on Start and Item screens)
            if (currentScreen == GroAppScreen.Start || currentScreen == GroAppScreen.Item) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color.Black),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Search \"milk\", \"bread\"...", color = Color.Gray, fontSize = 16.sp)
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}