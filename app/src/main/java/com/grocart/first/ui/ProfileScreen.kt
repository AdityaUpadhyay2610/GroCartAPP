package com.grocart.first.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.grocart.first.ui.theme.AestheticBackgroundStart
import com.grocart.first.ui.theme.AestheticBackgroundEnd
import com.grocart.first.ui.theme.ModernPrimary
import com.grocart.first.ui.theme.ModernSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    groViewModel: GroViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val user by groViewModel.user.collectAsState()
    
    var nameState by remember(user) { mutableStateOf(user?.username ?: "") }
    var emailState by remember(user) { mutableStateOf(user?.email ?: "") }
    var phoneState by remember { mutableStateOf("+91 9876543210") }
    var addressState by remember { mutableStateOf("Sector-4, GroCart City") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AestheticBackgroundStart, AestheticBackgroundEnd)))
            .verticalScroll(rememberScrollState())
    ) {
        // Aesthetic Gradient Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(ModernPrimary, ModernSecondary)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Circular Avatar
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(4.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile Picture",
                        tint = Color.White,
                        modifier = Modifier.size(70.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = nameState.ifEmpty { "User" },
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = emailState.ifEmpty { "No Email" },
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Editable Form Fields
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .offset(y = (-30).dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Personal Information",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    ProfileTextField(
                        value = nameState,
                        onValueChange = { nameState = it },
                        label = "Full Name",
                        icon = Icons.Default.Person
                    )

                    ProfileTextField(
                        value = emailState,
                        onValueChange = { emailState = it },
                        label = "Email Address",
                        icon = Icons.Default.Email
                    )

                    ProfileTextField(
                        value = phoneState,
                        onValueChange = { phoneState = it },
                        label = "Phone Number",
                        icon = Icons.Default.Phone
                    )

                    ProfileTextField(
                        value = addressState,
                        onValueChange = { addressState = it },
                        label = "Delivery Address",
                        icon = Icons.Default.LocationOn,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                Toast.makeText(context, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ModernPrimary)
                    ) {
                        Text(
                            text = "Save Profile",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    maxLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = ModernPrimary) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = RoundedCornerShape(12.dp),
        maxLines = maxLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ModernPrimary,
            focusedLabelColor = ModernPrimary,
            unfocusedContainerColor = Color(0xFFF8FAFC),
            focusedContainerColor = Color.White,
            unfocusedBorderColor = Color(0xFFE2E8F0)
        )
    )
}