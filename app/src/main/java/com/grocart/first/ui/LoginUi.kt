package com.grocart.first.ui

import android.util.Patterns
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
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
    val isEmailVerified by groViewModel.isEmailVerified.collectAsState()
    val user by groViewModel.user.collectAsState()
    val context = LocalContext.current

    // ── Level 1: Instant format validation ──
    // Only show error after user has typed something (not on first render)
    val emailTouched = remember { mutableStateOf(false) }
    val isEmailFormatValid = remember(email) {
        email.isEmpty() || Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Show auth errors as Toast
    LaunchedEffect(authError) {
        authError?.let {
            // Don't show "verification email sent" message as error — it's a success message
            if (it.contains("Verification", ignoreCase = true)) {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show()
            }
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

        // ── Level 2: Email verification banner ──
        // Shown to logged-in users who haven't verified their email yet
        AnimatedVisibility(
            visible = user != null && !isEmailVerified,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Email,
                        contentDescription = "Unverified",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Verify your email",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            "Check your inbox for a verification link.",
                            fontSize = 12.sp,
                            color = Color(0xFFB45309)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TextButton(
                            onClick = { groViewModel.refreshEmailVerification() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.MarkEmailRead,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF7C3AED)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Done", fontSize = 12.sp, color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
                        }
                        TextButton(
                            onClick = { groViewModel.resendVerificationEmail() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Resend", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }

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

        // ── Level 1: Email field with inline validation ──
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailTouched.value = true
            },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            isError = emailTouched.value && !isEmailFormatValid,
            supportingText = {
                if (emailTouched.value && !isEmailFormatValid) {
                    Text(
                        "Enter a valid email (e.g. user@gmail.com)",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
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
                    } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                    } else {
                        if (isSignupMode) {
                            groViewModel.register(username, email, password)
                        } else {
                            groViewModel.login(email, password)
                        }
                    }
                },
                // ── Disable if email format is invalid (and user has typed something) ──
                enabled = !isLoading && (email.isEmpty() || isEmailFormatValid),
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