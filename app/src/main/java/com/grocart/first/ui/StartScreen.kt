package com.grocart.first.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grocart.first.R
import com.grocart.first.data.DataSource
import com.grocart.first.data.InternetItem
import coil.compose.AsyncImage
import com.grocart.first.ui.theme.AestheticBackgroundStart
import com.grocart.first.ui.theme.AestheticBackgroundEnd
import androidx.compose.ui.graphics.Brush

@Composable
fun StartScreen(
    groViewModel: GroViewModel,
    onCategoryClicked: (Int) -> Unit
) {
    val context = LocalContext.current
    val allCategories = remember { DataSource.loadCategories() }
    val allItems by groViewModel.allItems.collectAsState()

    val recommendedItems = remember(allItems) {
        if (allItems.isNotEmpty()) allItems.shuffled().take(6) else emptyList()
    }
    
    val pastelColors = listOf(
        Color(0xFFF0FDF4),
        Color(0xFFE8EAF6),
        Color(0xFFE0F7FA),
        Color(0xFFFFF3E0),
        Color(0xFFF3E5F5),
        Color(0xFFE8F5E9)
    )

    Column(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(AestheticBackgroundStart, AestheticBackgroundEnd)))) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BannerSection()
            }

            if (recommendedItems.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    RecommendedSection(
                        items = recommendedItems,
                        groViewModel = groViewModel,
                        context = context
                    )
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Shop By Category",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(allCategories) { item ->
                val index = allCategories.indexOf(item)
                CategoryCardSleek(
                    context = context,
                    stringResourceId = item.stringResourceId,
                    imageResourceId = item.imageResourceId,
                    groViewModel = groViewModel,
                    onCategoryClicked = onCategoryClicked,
                    backgroundColor = pastelColors[index % pastelColors.size]
                )
            }
        }
    }
}

@Composable
fun BannerSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFFFDE68A), Color(0xFFFCD34D))
                )
            )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.padding(20.dp).weight(1f)) {
                Surface(
                    color = Color.White.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "LIMITED OFFER",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Mega Sale",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    color = Color(0xFF78350F)
                )
                Text(
                    text = "Up to 50% OFF",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF92400E)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { /*TODO*/ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF78350F)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Shop Now", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Image(
                painter = painterResource(id = R.drawable.munchies),
                contentDescription = null,
                modifier = Modifier.size(120.dp).padding(end = 16.dp).alpha(0.8f),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun RecommendedSection(
    items: List<InternetItem>,
    groViewModel: GroViewModel,
    context: Context
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            text = "Recommended for you",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = Color(0xFF1E293B),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items) { currentItem ->
                SmallItemCard(
                    item = currentItem,
                    groViewModel = groViewModel,
                    context = context
                )
            }
        }
    }
}

@Composable
fun SmallItemCard(
    item: InternetItem,
    groViewModel: GroViewModel,
    context: Context
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.width(140.dp),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.itemName,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = item.itemName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = item.itemQuantity,
                fontSize = 11.sp,
                color = Color.Gray,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₹${item.itemPrice * 75 / 100}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B)
                )
                Surface(
                    modifier = Modifier
                        .clickable {
                            groViewModel.triggerAddToCartAnimation(item)
                            groViewModel.addToCart(item)
                            Toast.makeText(context, "Added", Toast.LENGTH_SHORT).show()
                        },
                    color = Color(0xFFF5F3FF),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "ADD",
                        color = Color(0xFF7C3AED),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryCardSleek(
    context: Context,
    stringResourceId: Int,
    imageResourceId: Int,
    groViewModel: GroViewModel,
    onCategoryClicked: (Int) -> Unit,
    backgroundColor: Color = Color(0xFFE8EAF6)
) {
    val categoryName = stringResource(id = stringResourceId)

    Column(
        modifier = Modifier
            .clickable {
                groViewModel.updateClickText(categoryName)
                onCategoryClicked(stringResourceId)
            }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(backgroundColor, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = imageResourceId),
                contentDescription = categoryName,
                modifier = Modifier.size(52.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = categoryName,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = Color(0xFF334155)
        )
    }
}
