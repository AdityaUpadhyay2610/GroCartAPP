package com.grocart.first.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grocart.first.R
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import com.grocart.first.ui.theme.AestheticBackgroundStart
import com.grocart.first.ui.theme.AestheticBackgroundEnd

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginUi(groViewModel: GroViewModel) {
    var isSignupMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isLoading by groViewModel.loading.collectAsState()
    val authError by groViewModel.authError.collectAsState()
    val context = LocalContext.current

    // Show auth errors as Toast
    LaunchedEffect(authError) {
        authError?.let {
            Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
            groViewModel.clearAuthError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AestheticBackgroundStart, AestheticBackgroundEnd)))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.otp),
            contentDescription = "Login Illustration",
            modifier = Modifier.size(140.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isSignupMode) "Create Account" else "Welcome Back",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (isSignupMode) "Sign up to start shopping for fresh groceries" else "Log in with your email to continue",
            fontSize = 14.sp,
            color = Color.DarkGray,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        if (isSignupMode) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.large
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description = if (passwordVisible) "Hide password" else "Show password"
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
        } else {
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank() || (isSignupMode && username.isBlank())) {
                        Toast.makeText(context, "All fields are required!", Toast.LENGTH_SHORT).show()
                    } else {
                        if (isSignupMode) {
                            groViewModel.register(username, email, password)
                        } else {
                            groViewModel.login(email, password)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = if (isSignupMode) "Sign Up" else "Log In",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { isSignupMode = !isSignupMode }) {
            Text(
                text = if (isSignupMode) "Already have an account? Log In" else "Don't have an account? Sign Up",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        }

        TextButton(onClick = { groViewModel.startGuestSession() }) {
            Text("Continue as Guest", color = Color.Gray, fontWeight = FontWeight.Medium)
        }
    }
}