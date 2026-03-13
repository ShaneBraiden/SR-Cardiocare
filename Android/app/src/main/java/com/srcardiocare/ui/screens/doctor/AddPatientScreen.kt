// AddPatientScreen.kt — Form to register new patient
package com.srcardiocare.ui.screens.doctor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.srcardiocare.data.firebase.FirebaseService
import com.srcardiocare.ui.theme.DesignTokens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPatientScreen(onSaved: () -> Unit, onBack: () -> Unit) {
    var fullName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var selectedGender by remember { mutableIntStateOf(0) }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var injuryType by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val genders = listOf("Male", "Female", "Other")
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Patient", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DesignTokens.Spacing.XL)
        ) {
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            OutlinedTextField(
                value = fullName, onValueChange = { fullName = it },
                label = { Text("Full Name") }, placeholder = { Text("e.g. John Doe") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            OutlinedTextField(
                value = age, onValueChange = { age = it },
                label = { Text("Age") }, placeholder = { Text("e.g. 32") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            // Gender
            Text("Gender", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                genders.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedGender == index,
                        onClick = { selectedGender = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = genders.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = DesignTokens.Colors.Primary.copy(alpha = 0.15f),
                            activeContentColor = DesignTokens.Colors.Primary
                        )
                    ) { Text(label) }
                }
            }
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") }, placeholder = { Text("e.g. john@email.com") },
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
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            OutlinedTextField(
                value = injuryType, onValueChange = { injuryType = it },
                label = { Text("Injury Type") }, placeholder = { Text("e.g. ACL Injury") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("Additional Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
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
                    if (fullName.isBlank() || email.isBlank()) {
                        errorMessage = "Name and Email are required"
                        return@Button
                    }
                    isLoading = true
                    val nameParts = fullName.trim().split(" ", limit = 2)
                    val firstName = nameParts.firstOrNull() ?: ""
                    val lastName = if (nameParts.size > 1) nameParts[1] else ""

                    scope.launch {
                        try {
                            // Register the patient with default password
                            val userData = FirebaseService.register(
                                email = email.trim(),
                                password = "password@123",
                                firstName = firstName,
                                lastName = lastName,
                                role = "patient"
                            )

                            // Now sign back in as the doctor (register signs in as the new user)
                            // We need to update the patient's profile with extra fields
                            val patientUid = FirebaseService.currentUID
                            if (patientUid != null) {
                                val extraFields = mutableMapOf<String, Any>()
                                if (phone.isNotBlank()) extraFields["phone"] = phone.trim()
                                if (injuryType.isNotBlank()) extraFields["injuries"] = listOf(injuryType.trim())
                                if (age.isNotBlank()) extraFields["age"] = age.trim().toIntOrNull() ?: age.trim()
                                extraFields["gender"] = genders[selectedGender]
                                if (notes.isNotBlank()) extraFields["notes"] = notes.trim()

                                if (extraFields.isNotEmpty()) {
                                    FirebaseService.updateUser(extraFields)
                                }
                            }

                            onSaved()
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = "Failed to add patient: ${e.message}"
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
                    Text("Save Patient", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XXL))
        }
    }
}
