package com.grocart.first.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.grocart.first.R
import com.grocart.first.data.InternetItem
import com.grocart.first.data.Order
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import com.grocart.first.ui.theme.AestheticBackgroundStart
import com.grocart.first.ui.theme.AestheticBackgroundEnd
import com.grocart.first.ui.theme.ModernPrimary
import com.grocart.first.utils.PdfGenerator
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextAlign

@Composable
fun MyOrdersScreen(groViewModel: GroViewModel) {
    val orders by groViewModel.orders.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AestheticBackgroundStart, AestheticBackgroundEnd)))
    ) {
        if (orders.isNotEmpty()) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Order History",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(orders.reversed()) { order ->
                    OrderCard(order = order)
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.empty_box),
                    contentDescription = "No Orders",
                    modifier = Modifier.size(180.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "No Orders Yet",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your shopping journey hasn't started yet. Let's find something for you!",
                    fontSize = 15.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun OrderCard(order: Order) {
    val itemsWithQuantity = order.items
        .groupBy { it.itemName }
        .map { (_, items) ->
            com.grocart.first.data.InternetItemWithQuantity(items.first().toInternetItem(), items.size)
        }

    val orderTotal = order.items.sumOf { it.itemPrice * 75 / 100 }

    val context = LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatTimestamp(order.timestamp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "Order #${order.timestamp.toString().takeLast(6)}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Surface(
                    color = Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "₹$orderTotal",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF16A34A),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 1.dp, color = Color(0xFFF1F5F9))

            itemsWithQuantity.forEach { itemWithQuantity ->
                OrderItemRow(
                    item = itemWithQuantity.internetItem,
                    quantity = itemWithQuantity.quantity
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        PdfGenerator.generateInvoicePdf(context, order)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = ModernPrimary),
                border = BorderStroke(1.dp, ModernPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Download Invoice",
                    tint = ModernPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Invoice", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OrderItemRow(item: InternetItem, quantity: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = item.itemName,
            modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF8FAFC))
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(text = item.itemName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF334155))
            Text(
                text = "Qty: $quantity",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        Text(text = "₹${(item.itemPrice * 75 / 100) * quantity}", fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, yyyy 'at' hh:mm a", Locale.getDefault())
    val date = Date(timestamp)
    return sdf.format(date)
}
