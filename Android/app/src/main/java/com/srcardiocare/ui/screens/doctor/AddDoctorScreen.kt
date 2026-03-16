// AddDoctorScreen.kt — Admin-only form to register a new doctor
package com.srcardiocare.ui.screens.doctor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.srcardiocare.data.firebase.FirebaseService
import com.srcardiocare.ui.theme.DesignTokens
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDoctorScreen(onSaved: () -> Unit, onBack: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var speciality by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var clinicName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Remember admin credentials to re-sign-in after creating the doctor
    var adminEmail by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val uid = FirebaseService.currentUID ?: return@LaunchedEffect
            val data = FirebaseService.fetchUser(uid)
            adminEmail = data["email"] as? String
        } catch (_: Exception) { }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Doctor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DesignTokens.Spacing.XL)
        ) {
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            OutlinedTextField(
                value = fullName, onValueChange = { fullName = it },
                label = { Text("Full Name") }, placeholder = { Text("e.g. Dr. Jane Smith") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") }, placeholder = { Text("e.g. jane@clinic.com") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            OutlinedTextField(
                value = phone, onValueChange = { phone = it },
                label = { Text("Phone") }, placeholder = { Text("e.g. +1 555 123 4567") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            OutlinedTextField(
                value = speciality, onValueChange = { speciality = it },
                label = { Text("Speciality") }, placeholder = { Text("e.g. Cardiology") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            OutlinedTextField(
                value = licenseNumber, onValueChange = { licenseNumber = it },
                label = { Text("License Number") }, placeholder = { Text("e.g. MED-12345") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            OutlinedTextField(
                value = clinicName, onValueChange = { clinicName = it },
                label = { Text("Clinic Name") }, placeholder = { Text("e.g. City Heart Clinic") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))

            // Default password info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                colors = CardDefaults.cardColors(containerColor = DesignTokens.Colors.PrimaryLight.copy(alpha = 0.3f))
            ) {
                Text(
                    "ℹ️ Default password: password@123",
                    modifier = Modifier.padding(DesignTokens.Spacing.MD),
                    style = MaterialTheme.typography.bodySmall,
                    color = DesignTokens.Colors.PrimaryDark
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

            Button(
                onClick = {
                    val nameParts = fullName.trim().split(" ", limit = 2)
                    val firstName = nameParts.firstOrNull() ?: ""
                    val lastName = if (nameParts.size > 1) nameParts[1] else ""
                    val trimmedEmail = email.trim()

                    if (firstName.isBlank() || lastName.isBlank() || trimmedEmail.isBlank()) {
                        errorMessage = "First Name, Last Name, and Email are required"
                        return@Button
                    }
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                        errorMessage = "Please enter a valid email address"
                        return@Button
                    }
                    isLoading = true

                    scope.launch {
                        try {
                            // Create account WITHOUT switching auth session
                            val newDoctorUid = FirebaseService.registerOther(
                                email = email.trim(),
                                password = "password@123",
                                firstName = firstName,
                                lastName = lastName,
                                role = "doctor"
                            )

                            // Write extra fields directly to the new doctor's doc
                            val extraFields = mutableMapOf<String, Any>()
                            if (phone.isNotBlank()) extraFields["phone"] = phone.trim()
                            if (speciality.isNotBlank()) extraFields["speciality"] = speciality.trim()
                            if (licenseNumber.isNotBlank()) extraFields["licenseNumber"] = licenseNumber.trim()
                            if (clinicName.isNotBlank()) extraFields["clinicName"] = clinicName.trim()

                            if (extraFields.isNotEmpty()) {
                                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("users").document(newDoctorUid)
                                    .update(extraFields)
                                    .await()
                            }

                            // Show success and navigate back
                            snackbarHostState.showSnackbar("✅ Doctor account created successfully!")
                            onSaved()
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = "Failed to add doctor: ${e.message}"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.Primary),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Save Doctor", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XXL))
        }
    }
}
