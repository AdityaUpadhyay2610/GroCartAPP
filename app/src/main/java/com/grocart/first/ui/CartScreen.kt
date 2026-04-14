package com.grocart.first.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.grocart.first.R
import com.grocart.first.data.CartItemResponse
import com.grocart.first.data.InternetItem

@Composable
fun CartScreen(
    groViewModel: GroViewModel,
    onHomeButtonClicked: () -> Unit
) {
    val cartItems by groViewModel.cartItems.collectAsState()
    val showPaymentScreen by groViewModel.showPaymentScreen.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        if (cartItems.isNotEmpty()) {

            val totalPrice = cartItems.sumOf { (it.itemPrice * 75 / 100) * it.quantity }
            val handlingCharge = (totalPrice * 0.01).toInt()
            val deliveryFee = 30
            val grandTotal = totalPrice + handlingCharge + deliveryFee

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Review Items",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(cartItems) { item ->
                    CartCard(
                        item = item,
                        quantity = item.quantity,
                        onAddItem = { 
                            val baseItem = InternetItem(id = item.id, itemName = item.itemName, itemPrice = item.itemPrice, imageUrl = item.imageUrl)
                            groViewModel.addToCart(baseItem) 
                        },
                        onRemoveItem = { groViewModel.decreaseItemCount(item) }
                    )
                }

                item {
                    Text(
                        text = "Bill Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 80.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Bill Details", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            cartItems.forEach { cartItem ->
                                val lineItemPrice = (cartItem.itemPrice * 75 / 100)
                                val lineTotal = lineItemPrice * cartItem.quantity
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    val leftText = "${cartItem.quantity}x ${cartItem.itemName}"
                                    val displayName = if (leftText.length > 25) leftText.take(22) + "..." else leftText
                                    Text(text = displayName, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "Rs. $lineTotal", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            BillRow("Item Total", totalPrice, FontWeight.Normal)
                            BillRow("Handling Charge", handlingCharge, FontWeight.Normal)
                            BillRow("Delivery Fee", deliveryFee, FontWeight.Normal)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            BillRow("To Pay", grandTotal, FontWeight.ExtraBold)
                        }
                    }
                }
            } // end of LazyColumn

            // Pinned Bottom Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { groViewModel.proceedToPay() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Proceed to Pay  •  Rs. $grandTotal", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        } else {
            EmptyCartUI(onHomeButtonClicked)
        }

        // Payment Method Screen Overlay
        if (showPaymentScreen) {
            val paymentTotalPrice = cartItems.sumOf { (it.itemPrice * 75 / 100) * it.quantity }
            val paymentHandlingCharge = (paymentTotalPrice * 0.01).toInt()
            val paymentDeliveryFee = 30

            PaymentMethodScreen(
                totalPrice = paymentTotalPrice,
                handlingCharge = paymentHandlingCharge,
                deliveryFee = paymentDeliveryFee,
                groViewModel = groViewModel,
                onPaymentConfirmed = { _, finalTotal, couponDiscount ->
                    groViewModel.completePayment()
                    groViewModel.placeOrder(finalTotal, couponDiscount)
                    onHomeButtonClicked()
                },
                onBack = { groViewModel.cancelPayment() }
            )
        }
    }
}

@Composable
fun EmptyCartUI(onHomeButtonClicked: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.emptycart),
            contentDescription = "Empty Cart",
            modifier = Modifier.size(250.dp)
        )
        Text(
            text = "Your Cart is Empty",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(20.dp)
        )
        Button(
            onClick = onHomeButtonClicked,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(text = "Browse Products", fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}

@Composable
fun CartCard(
    item: CartItemResponse,
    quantity: Int,
    onAddItem: () -> Unit,
    onRemoveItem: () -> Unit
) {
    val lineItemTotalPrice = (item.itemPrice * 75 / 100) * quantity

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.itemName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(text = item.itemName, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
                Text(text = "Rs. ${item.itemPrice * 75 / 100}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                QuantitySelector(quantity = quantity, onAddItem = onAddItem, onRemoveItem = onRemoveItem)
                Text(
                    text = "Rs. $lineItemTotalPrice",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun QuantitySelector(
    quantity: Int,
    onAddItem: () -> Unit,
    onRemoveItem: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        IconButton(
            onClick = onRemoveItem,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Remove", tint = MaterialTheme.colorScheme.primary)
        }
        Text(text = "$quantity", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        IconButton(
            onClick = onAddItem,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

// Helper function for Billing - Styled nicely
@Composable
fun BillRow(itemName: String, itemPrice: Int, fontWeight: FontWeight) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = itemName, fontWeight = fontWeight, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(text = "Rs. $itemPrice", fontWeight = fontWeight, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
    }
}