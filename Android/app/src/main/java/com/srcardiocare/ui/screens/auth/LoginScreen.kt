// LoginScreen.kt — Auth screen composable
package com.srcardiocare.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.srcardiocare.core.auth.AuthManager
import com.srcardiocare.core.security.ErrorHandler
import com.srcardiocare.data.repository.RehabRepository
import com.srcardiocare.ui.theme.DesignTokens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: (role: String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { RehabRepository() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = DesignTokens.Spacing.XL)
                    .padding(bottom = 48.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))

                Text(
                    text = "Sign in to continue your cardiac care journey",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.XXL))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email address") },
                    modifier = Modifier.fillMaxWidth()
                        .semantics { contentType = ContentType.EmailAddress },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(DesignTokens.Radius.Base),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DesignTokens.Colors.Primary,
                        cursorColor = DesignTokens.Colors.Primary
                    )
                )

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth()
                        .semantics { contentType = ContentType.Password },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(DesignTokens.Radius.Base),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DesignTokens.Colors.Primary,
                        cursorColor = DesignTokens.Colors.Primary
                    )
                )

                errorMessage?.let {
                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))
                    Text(text = it, color = DesignTokens.Colors.Error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

                Button(
                    onClick = {
                        val trimmedEmail = email.trim()
                        if (trimmedEmail.isBlank() || password.isBlank()) {
                            errorMessage = "Please enter your email and password."
                            return@Button
                        }
                        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                            errorMessage = "Please enter a valid email address."
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            val result = repository.login(email.trim(), password)
                            result.onSuccess { userData ->
                                val role = (userData["role"] as? String)?.uppercase() ?: "PATIENT"
                                val authManager = AuthManager(context)
                                authManager.userRole = role
                                if (role != "PATIENT") {
                                    authManager.onboardingCompleted = true
                                }
                                onLoginSuccess(role)
                            }.onFailure { e ->
                                errorMessage = ErrorHandler.getDisplayMessage(e as Exception, "login")
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DesignTokens.Spacing.XXL + DesignTokens.Spacing.MD),
                    shape = RoundedCornerShape(DesignTokens.Radius.Base),
                    colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.Primary),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(DesignTokens.Spacing.XL),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = DesignTokens.Spacing.XXS
                        )
                    } else {
                        Text("Sign In", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            
            Text(
                text = "Powered by SRET-AIDA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = DesignTokens.Spacing.MD)
            )
        }
    }
}
