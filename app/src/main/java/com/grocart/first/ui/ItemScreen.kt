package com.grocart.first.ui

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.grocart.first.R
import com.grocart.first.data.InternetItem
import com.grocart.first.ui.theme.AestheticBackgroundStart
import com.grocart.first.ui.theme.AestheticBackgroundEnd

@Composable
fun InternetItemScreen(
    groViewModel: GroViewModel,
    itemUiState: GroViewModel.ItemUiState
){
    when(itemUiState){
        is GroViewModel.ItemUiState.Loading -> LoadingScreen()
        is GroViewModel.ItemUiState.Success -> {
            ItemScreen(groViewModel = groViewModel, items = itemUiState.items)
        }

        else -> ErrorScreen(groViewModel = groViewModel)
    }
}

@Composable
fun ItemScreen(
    groViewModel: GroViewModel,
    items : List<InternetItem>
) {
    val groUiState by groViewModel.uiState.collectAsState()
    val selectedCategory = stringResource(groUiState.selectedCategory)

    val animatingItem by groViewModel.animatingItem.collectAsState()

    val database = remember(items, selectedCategory) {
        if (items.isEmpty()) emptyList()
        else items.filter { 
            val dbCat = it.itemCategory.trim()
            val uiCat = selectedCategory.trim()
            dbCat.equals(uiCat, ignoreCase = true) || 
            uiCat.contains(dbCat, ignoreCase = true) || 
            dbCat.contains(uiCat, ignoreCase = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(AestheticBackgroundStart, AestheticBackgroundEnd)))) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {

            item(span = { GridItemSpan(1) }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                ) {
                    Text(
                        text = "$selectedCategory (${database.size})",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                }
            }

            items(database) { currentItem ->
                ItemCard(
                    itemId = currentItem.id,
                    itemName = currentItem.itemName,
                    imageUrl = currentItem.imageUrl,
                    quantityLabel = currentItem.itemQuantity,
                    itemPrice = currentItem.itemPrice,
                    itemCategory = currentItem.itemCategory,
                    groViewModel = groViewModel
                )
            }
        }

        animatingItem?.let { item ->
            FlyingItemAnimation(item)
        }
    }
}

@Composable
fun FlyingItemAnimation(item: InternetItem) {
    var isStarted by remember { mutableStateOf(false) }

    val animatedY by animateFloatAsState(
        targetValue = if (isStarted) 1000f else 0f,
        animationSpec = tween(700),
        label = "yAnimation"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isStarted) 0f else 1f,
        animationSpec = tween(700),
        label = "alphaAnimation"
    )

    LaunchedEffect(Unit) { isStarted = true }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(70.dp)
                .offset(y = animatedY.dp)
                .alpha(animatedAlpha)
                .align(Alignment.Center)
        )
    }
}

@Composable
fun ItemCard(
    itemId: Long,
    itemName: String,
    imageUrl: String,
    quantityLabel: String,
    itemPrice: Int,
    itemCategory: String,
    groViewModel: GroViewModel
) {
    val context = LocalContext.current

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = itemName,
                    modifier = Modifier.size(100.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = itemName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().height(40.dp)
            )
            Text(
                text = quantityLabel,
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Rs. $itemPrice", fontSize = 10.sp, color = Color.Gray, textDecoration = TextDecoration.LineThrough)
                    Text(text = "Rs. ${itemPrice * 75 / 100}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFFEDE9FE), RoundedCornerShape(4.dp))
                        .clickable {
                            val currentItem = InternetItem(
                                id = itemId,
                                itemName = itemName,
                                imageUrl = imageUrl,
                                itemQuantity = quantityLabel,
                                itemPrice = itemPrice,
                                itemCategory = itemCategory
                            )
                            groViewModel.triggerAddToCartAnimation(currentItem)
                            groViewModel.addToCart(currentItem)
                            // The user's snippet had item.name and item, but the original code uses currentItem and itemName.
                            // Keeping the original logic for Toast message to avoid breaking functionality.
                            Toast.makeText(context, "Added", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("ADD", color = Color(0xFF7C3AED), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorScreen(groViewModel: GroViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painter = painterResource(id = R.drawable.error), contentDescription = "Error")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { groViewModel.getFirstItem() }) {
            Text("Retry")
        }
    }
}