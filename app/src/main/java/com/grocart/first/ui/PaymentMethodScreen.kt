package com.grocart.first.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grocart.first.data.CouponOffer
import com.grocart.first.data.PaymentMethod
import kotlinx.coroutines.delay

// ── Color Palette ──

private val PurplePrimary = Color(0xFF7C3AED)
private val PurpleLight = Color(0xFFEDE9FE)
private val PurpleSurface = Color(0xFFFAF5FF)
private val GreenAccent = Color(0xFF16A34A)
private val GreenLight = Color(0xFFDCFCE7)
private val DarkText = Color(0xFF1A1A2E)
private val GraySubtle = Color(0xFFF8F9FA)

// ════════════════════════════════════════════════════════════════════════════
//  MAIN SCREEN
// ════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodScreen(
    totalPrice: Int,
    handlingCharge: Int,
    deliveryFee: Int,
    groViewModel: GroViewModel,
    onPaymentConfirmed: (paymentMethod: PaymentMethod, finalTotal: Int, couponDiscount: Int) -> Unit,
    onBack: () -> Unit
) {
    val selectedMethod by groViewModel.selectedPaymentMethod.collectAsState()
    val appliedCoupon by groViewModel.appliedCoupon.collectAsState()
    val couponOffers = groViewModel.couponOffers
    
    var showCouponSection by remember { mutableStateOf(false) }
    var couponInput by remember { mutableStateOf("") }
    var couponError by remember { mutableStateOf<String?>(null) }
    var showOrderPlaced by remember { mutableStateOf(false) }

    val couponDiscount = if (appliedCoupon != null) {
        (totalPrice * appliedCoupon!!.discountPercent / 100)
    } else 0
    val grandTotal = totalPrice + handlingCharge + deliveryFee - couponDiscount

    // Order placed animation overlay
    if (showOrderPlaced) {
        OrderPlacedOverlay(
            paymentMethod = selectedMethod ?: PaymentMethod.COD,
            finalTotal = grandTotal,
            groViewModel = groViewModel,
            onComplete = {
                onPaymentConfirmed(selectedMethod ?: PaymentMethod.COD, grandTotal, couponDiscount)
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Payment",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = DarkText
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Order Summary Card ──
            OrderSummaryCard(totalPrice, handlingCharge, deliveryFee, couponDiscount, grandTotal, appliedCoupon)

            Spacer(modifier = Modifier.height(16.dp))

            // ── Payment Methods Section ──
            Text(
                "Choose Payment Method",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = DarkText,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            PaymentMethod.entries.forEach { method ->
                PaymentMethodCard(
                    method = method,
                    isSelected = selectedMethod == method,
                    isEnabled = true, // All methods enabled as requested
                    groViewModel = groViewModel,
                    onClick = {
                        groViewModel.setPaymentMethod(method)
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Coupon Section ──
            CouponApplySection(
                couponInput = couponInput,
                onCouponInputChange = { couponInput = it },
                appliedCoupon = appliedCoupon,
                couponError = couponError,
                showCouponSection = showCouponSection,
                onToggle = { showCouponSection = !showCouponSection },
                totalPrice = totalPrice,
                couponOffers = couponOffers,
                onApply = { code ->
                    couponError = groViewModel.applyCoupon(code, totalPrice)
                    if (couponError == null) {
                        showCouponSection = false
                    }
                },
                onRemove = {
                    groViewModel.removeCoupon()
                    couponInput = ""
                    couponError = null
                },
                onCouponTap = { coupon ->
                    couponInput = coupon.code
                    couponError = groViewModel.applyCoupon(coupon.code, totalPrice)
                    if (couponError == null) {
                        showCouponSection = false
                    }
                }
            )

            Spacer(modifier = Modifier.height(100.dp)) // Space for bottom button
        }

        // ── Bottom Buy Button ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.BottomCenter
        ) {
            BuyButton(
                grandTotal = grandTotal,
                selectedMethod = selectedMethod,
                onClick = {
                    if (selectedMethod != null) {
                        showOrderPlaced = true
                    }
                }
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  ORDER SUMMARY CARD
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun OrderSummaryCard(
    totalPrice: Int,
    handlingCharge: Int,
    deliveryFee: Int,
    couponDiscount: Int,
    grandTotal: Int,
    appliedCoupon: CouponOffer?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PurpleLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Receipt, "Receipt", tint = PurplePrimary, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Order Summary", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = DarkText)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(12.dp))

            SummaryRow("Item Total", "₹$totalPrice")
            SummaryRow("Handling Charge", "₹$handlingCharge")
            SummaryRow("Delivery Fee", "₹$deliveryFee")

            if (appliedCoupon != null && couponDiscount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalOffer, "Coupon", tint = GreenAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            appliedCoupon.code,
                            fontSize = 14.sp,
                            color = GreenAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text("- ₹$couponDiscount", color = GreenAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = DarkText)
                Text("₹$grandTotal", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = PurplePrimary)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, color = DarkText, fontWeight = FontWeight.Medium)
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  PAYMENT METHOD CARD
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun PaymentMethodCard(
    method: PaymentMethod,
    isSelected: Boolean,
    isEnabled: Boolean,
    groViewModel: GroViewModel,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        if (isSelected) PurplePrimary else if (!isEnabled) Color(0xFFE0E0E0) else Color(0xFFEEEEEE),
        label = "border"
    )
    val bgColor by animateColorAsState(
        if (isSelected) PurpleSurface else if (!isEnabled) Color(0xFFFAFAFA) else Color.White,
        label = "bg"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(enabled = isEnabled) { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) PurplePrimary else if (isEnabled) PurpleLight else Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    groViewModel.getPaymentIcon(method),
                    method.label,
                    tint = if (isSelected) Color.White else if (isEnabled) PurplePrimary else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    method.label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isEnabled) DarkText else Color.Gray
                )
                Text(
                    method.subtitle,
                    fontSize = 13.sp,
                    color = if (isEnabled) Color.Gray else Color.LightGray
                )
            }

            if (!isEnabled) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFFF3E0)
                ) {
                    Text(
                        "Soon",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            } else if (isSelected) {
                Icon(Icons.Default.CheckCircle, "Selected", tint = PurplePrimary, modifier = Modifier.size(28.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(2.dp, Color(0xFFCCCCCC), CircleShape)
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  COUPON SECTION
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun CouponApplySection(
    couponInput: String,
    onCouponInputChange: (String) -> Unit,
    appliedCoupon: CouponOffer?,
    couponError: String?,
    showCouponSection: Boolean,
    onToggle: () -> Unit,
    totalPrice: Int,
    couponOffers: List<CouponOffer>,
    onApply: (String) -> Unit,
    onRemove: () -> Unit,
    onCouponTap: (CouponOffer) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (appliedCoupon != null) BorderStroke(1.5.dp, GreenAccent) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (appliedCoupon != null) GreenLight else PurpleLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LocalOffer,
                            "Coupon",
                            tint = if (appliedCoupon != null) GreenAccent else PurplePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            if (appliedCoupon != null) "Coupon Applied!" else "Apply Coupon",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (appliedCoupon != null) GreenAccent else DarkText
                        )
                        if (appliedCoupon != null) {
                            Text(
                                "${appliedCoupon.code} — ${appliedCoupon.discountPercent}% off",
                                fontSize = 13.sp,
                                color = GreenAccent
                            )
                        } else {
                            Text("Save more on your order", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                }

                if (appliedCoupon != null) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, "Remove", tint = Color.Red, modifier = Modifier.size(20.dp))
                    }
                } else {
                    Icon(
                        if (showCouponSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        "Toggle",
                        tint = PurplePrimary
                    )
                }
            }

            // Expandable content
            AnimatedVisibility(
                visible = showCouponSection && appliedCoupon == null,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {

                    // Input field
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = couponInput,
                            onValueChange = onCouponInputChange,
                            placeholder = { Text("Enter coupon code", fontSize = 14.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurplePrimary,
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onApply(couponInput) },
                            enabled = couponInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PurplePrimary,
                                disabledContainerColor = Color(0xFFCCCCCC)
                            ),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text("Apply", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Error
                    if (couponError != null) {
                        Text(
                            couponError,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Available coupons label
                    Text(
                        "Available Coupons",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Coupon cards
                    couponOffers.forEach { coupon ->
                        CouponOfferCard(coupon = coupon, onTap = { onCouponTap(coupon) })
                    }
                }
            }
        }
    }
}

@Composable
private fun CouponOfferCard(coupon: CouponOffer, onTap: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onTap() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PurpleSurface),
        border = BorderStroke(1.dp, Color(0xFFE9D5FF))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Discount badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(listOf(PurplePrimary, Color(0xFF9333EA)))
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    "${coupon.discountPercent}%",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    coupon.code,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = PurplePrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    coupon.description,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = PurplePrimary.copy(alpha = 0.1f)
            ) {
                Text(
                    "TAP",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = PurplePrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  BUY BUTTON
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun BuyButton(
    grandTotal: Int,
    selectedMethod: PaymentMethod?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (selectedMethod == null) {
                Text(
                    "Please select a payment method",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = onClick,
                enabled = selectedMethod != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PurplePrimary,
                    disabledContainerColor = Color(0xFFCCCCCC)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.ShoppingCart, "Buy", tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Buy It  •  ₹$grandTotal",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
//  ORDER PLACED ANIMATION OVERLAY
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun OrderPlacedOverlay(
    paymentMethod: PaymentMethod,
    finalTotal: Int,
    groViewModel: GroViewModel,
    onComplete: () -> Unit
) {
    var stage by remember { mutableIntStateOf(0) } // 0 = processing, 1 = success

    LaunchedEffect(Unit) {
        delay(2000)
        stage = 1
        delay(2000)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            AnimatedContent(
                targetState = stage,
                transitionSpec = {
                    fadeIn(tween(400)) + scaleIn(tween(400)) togetherWith
                            fadeOut(tween(200)) + scaleOut(tween(200))
                },
                label = "order_anim"
            ) { currentStage ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (currentStage == 0) {
                        // Processing animation
                        CircularProgressIndicator(
                            color = PurplePrimary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Placing your order...",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Please wait a moment",
                            fontSize = 15.sp,
                            color = Color.Gray
                        )
                    } else {
                        // Success
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(GreenAccent, Color(0xFF4ADE80))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                "Success",
                                tint = Color.White,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Order Placed! 🎉",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Payment: ${paymentMethod.label}",
                            fontSize = 15.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Total: ₹$finalTotal",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimary
                        )
                    }
                }
            }
        }
    }
}