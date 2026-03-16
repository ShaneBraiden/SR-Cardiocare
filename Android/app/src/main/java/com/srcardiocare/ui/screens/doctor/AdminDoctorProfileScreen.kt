// AdminDoctorProfileScreen.kt — Admin view to edit and delete doctor accounts
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.srcardiocare.data.firebase.FirebaseService
import com.srcardiocare.ui.theme.DesignTokens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDoctorProfileScreen(
    doctorId: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var speciality by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var clinicName by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(doctorId) {
        try {
            val data = FirebaseService.fetchUser(doctorId)
            firstName = data["firstName"] as? String ?: ""
            lastName = data["lastName"] as? String ?: ""
            email = data["email"] as? String ?: ""
            phone = data["phone"] as? String ?: ""
            speciality = data["speciality"] as? String ?: ""
            licenseNumber = data["licenseNumber"] as? String ?: ""
            clinicName = data["clinicName"] as? String ?: ""
        } catch (e: Exception) {
            errorMessage = "Failed to load doctor details: ${e.message}"
        }
        isLoading = false
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Doctor") },
            text = { Text("Are you sure you want to delete Dr. $firstName $lastName? This will remove their Firestore profile.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        isDeleting = true
                        scope.launch {
                            try {
                                FirebaseService.deleteUser(doctorId)
                                onDeleted()
                            } catch (e: Exception) {
                                isDeleting = false
                                errorMessage = "Failed to delete: ${e.message}"
                            }
                        }
                    }
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Delete", color = DesignTokens.Colors.Error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
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
                title = { Text("Manage Doctor", fontWeight = FontWeight.SemiBold) },
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
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DesignTokens.Colors.Primary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DesignTokens.Spacing.XL)
        ) {
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            // Non-editable email
            OutlinedTextField(
                value = email, onValueChange = {},
                label = { Text("Email (Cannot be changed)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                shape = RoundedCornerShape(DesignTokens.Radius.Base)
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            OutlinedTextField(
                value = firstName, onValueChange = { firstName = it },
                label = { Text("First Name") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Base), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            OutlinedTextField(
                value = lastName, onValueChange = { lastName = it },
                label = { Text("Last Name") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Base), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            OutlinedTextField(
                value = phone, onValueChange = { phone = it },
                label = { Text("Phone") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(DesignTokens.Radius.Base), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            OutlinedTextField(
                value = speciality, onValueChange = { speciality = it },
                label = { Text("Speciality") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Base), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            OutlinedTextField(
                value = licenseNumber, onValueChange = { licenseNumber = it },
                label = { Text("License Number") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Base), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            OutlinedTextField(
                value = clinicName, onValueChange = { clinicName = it },
                label = { Text("Clinic Name") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Base), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

            // Save changes button
            Button(
                onClick = {
                    if (firstName.isBlank() || lastName.isBlank()) {
                        errorMessage = "Names cannot be blank"
                        return@Button
                    }
                    isSaving = true
                    scope.launch {
                        try {
                            val updates = mapOf(
                                "firstName" to firstName.trim(),
                                "lastName" to lastName.trim(),
                                "phone" to phone.trim(),
                                "speciality" to speciality.trim(),
                                "licenseNumber" to licenseNumber.trim(),
                                "clinicName" to clinicName.trim()
                            )
                            FirebaseService.updateUserById(doctorId, updates)
                            snackbarHostState.showSnackbar("✅ Doctor updated successfully")
                        } catch (e: Exception) {
                            errorMessage = "Failed to update: ${e.message}"
                        }
                        isSaving = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.Primary),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text("Save Changes", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

            // Delete user button
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DesignTokens.Colors.Error),
                border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.Error)
            ) {
                Text("Delete Doctor", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XXL))
        }
    }
}
