package com.grocart.first.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grocart.first.data.DataSource

// Blinkit-style category palette — each category pair gets a distinct warm/cool tint
private val categoryPalettes = listOf(
    Color(0xFFFFF3E0) to Color(0xFFF57C00), // Fruits       — amber
    Color(0xFFF3E5F5) to Color(0xFF8E24AA), // Bread        — purple
    Color(0xFFE8F5E9) to Color(0xFF43A047), // Sweet        — green
    Color(0xFFE3F2FD) to Color(0xFF1E88E5), // Bath         — blue
    Color(0xFFFCE4EC) to Color(0xFFE91E63), // Beverages    — pink
    Color(0xFFF1F8E9) to Color(0xFF7CB342), // Kitchen      — light-green
    Color(0xFFFFF8E1) to Color(0xFFFFB300), // Munchies     — yellow
    Color(0xFFE0F7FA) to Color(0xFF00ACC1), // Packed food  — cyan
    Color(0xFFFBE9E7) to Color(0xFFE64A19), // Sweet 2      — deep-orange
    Color(0xFFE8F5E9) to Color(0xFF2E7D32), // Vegetables   — dark-green
    Color(0xFFEDE7F6) to Color(0xFF512DA8), // Cleaning     — deep-purple
    Color(0xFFFFF9C4) to Color(0xFFF9A825), // Stationery   — yellow-700
    Color(0xFFFFECB3) to Color(0xFFFF6F00), // Pet food     — amber-900
)

@Composable
fun CategoryScreen(
    groViewModel: GroViewModel,
    onCategoryClicked: (Int) -> Unit
) {
    val allCategories = DataSource.loadCategories()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FB))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF7C3AED), Color(0xFF0D9488))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column {
                Text(
                    text = "All Categories",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = Color.White
                )
                Text(
                    text = "What are you looking for?",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(allCategories) { category ->
                val index = allCategories.indexOf(category)
                val (bgColor, accentColor) = categoryPalettes[index % categoryPalettes.size]
                BlinkitCategoryCard(
                    stringResourceId = category.stringResourceId,
                    imageResourceId = category.imageResourceId,
                    backgroundColor = bgColor,
                    accentColor = accentColor,
                    groViewModel = groViewModel,
                    onCategoryClicked = onCategoryClicked
                )
            }
        }
    }
}

@Composable
fun BlinkitCategoryCard(
    stringResourceId: Int,
    imageResourceId: Int,
    backgroundColor: Color,
    accentColor: Color,
    groViewModel: GroViewModel,
    onCategoryClicked: (Int) -> Unit
) {
    val name = stringResource(id = stringResourceId)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable {
                groViewModel.updateClickText(name)
                onCategoryClicked(stringResourceId)
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon box
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = imageResourceId),
                    contentDescription = name,
                    modifier = Modifier.size(50.dp),
                    contentScale = ContentScale.Fit
                )
            }

            // Name + accent chip
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF1E293B),
                    maxLines = 2,
                    lineHeight = 16.sp
                )
                Surface(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "Shop →",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
