// AddPatientScreen.kt — Form to register new patient
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
import com.srcardiocare.core.security.ErrorHandler
import com.srcardiocare.core.security.InputValidator
import com.srcardiocare.data.firebase.FirebaseService
import com.srcardiocare.ui.theme.DesignTokens
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DesignTokens.Spacing.XL)
        ) {
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = InputValidator.limitLength(it, InputValidator.MaxLength.NAME) },
                label = { Text("Full Name") }, placeholder = { Text("e.g. John Doe") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            OutlinedTextField(
                value = age,
                onValueChange = { newVal ->
                    // Only allow digits, max 3 chars
                    if (newVal.all { it.isDigit() } && newVal.length <= 3) age = newVal
                },
                label = { Text("Age") }, placeholder = { Text("e.g. 32") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                value = phone,
                onValueChange = { phone = InputValidator.limitLength(it, InputValidator.MaxLength.PHONE) },
                label = { Text("Phone") }, placeholder = { Text("e.g. +1 555 123 4567") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            OutlinedTextField(
                value = injuryType,
                onValueChange = { injuryType = InputValidator.limitLength(it, InputValidator.MaxLength.INJURY_TYPE) },
                label = { Text("Injury Type") }, placeholder = { Text("e.g. ACL Injury") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = InputValidator.limitLength(it, InputValidator.MaxLength.NOTES) },
                label = { Text("Additional Notes") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))

            // Password info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                colors = CardDefaults.cardColors(containerColor = DesignTokens.Colors.PrimaryLight.copy(alpha = 0.3f))
            ) {
                Text(
                    "Default password: password@123. The patient will be prompted to change it on first login.",
                    modifier = Modifier.padding(DesignTokens.Spacing.MD),
                    style = MaterialTheme.typography.bodySmall,
                    color = DesignTokens.Colors.PrimaryDark
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

            Button(
                onClick = {
                    // Validate name
                    val nameValidation = InputValidator.validateName(fullName, "Full Name")
                    if (!nameValidation.isValid) {
                        errorMessage = nameValidation.errorMessage
                        return@Button
                    }

                    // Validate email
                    val emailValidation = InputValidator.validateEmail(email)
                    if (!emailValidation.isValid) {
                        errorMessage = emailValidation.errorMessage
                        return@Button
                    }

                    // Validate phone (optional but must be valid format if provided)
                    val phoneValidation = InputValidator.validatePhone(phone)
                    if (!phoneValidation.isValid) {
                        errorMessage = phoneValidation.errorMessage
                        return@Button
                    }

                    // Validate age (optional but must be valid if provided)
                    val ageValidation = InputValidator.validateAge(age)
                    if (!ageValidation.isValid) {
                        errorMessage = ageValidation.errorMessage
                        return@Button
                    }

                    isLoading = true
                    val nameParts = nameValidation.sanitizedValue.split(" ", limit = 2)
                    val firstName = nameParts.firstOrNull() ?: ""
                    val lastName = if (nameParts.size > 1) nameParts[1] else ""
                    val creatingUserUid = FirebaseService.currentUID

                    scope.launch {
                        try {
                            // Use default password - user can change on first login
                            val defaultPassword = "password@123"

                            // Create account WITHOUT switching auth session
                            val newPatientUid = FirebaseService.registerOther(
                                email = emailValidation.sanitizedValue,
                                password = defaultPassword,
                                firstName = firstName,
                                lastName = lastName,
                                role = "patient"
                            )

                            // Write extra fields directly to the new patient's doc
                            val extraFields = mutableMapOf<String, Any>()
                            if (phoneValidation.sanitizedValue.isNotBlank()) {
                                extraFields["phone"] = phoneValidation.sanitizedValue
                            }
                            if (injuryType.isNotBlank()) {
                                extraFields["injuries"] = listOf(injuryType.trim())
                            }
                            if (ageValidation.sanitizedValue.isNotBlank()) {
                                extraFields["age"] = ageValidation.sanitizedValue.toInt()
                            }
                            extraFields["gender"] = genders[selectedGender]
                            if (notes.isNotBlank()) extraFields["notes"] = notes.trim()
                            if (creatingUserUid != null) {
                                val creatorRole = (FirebaseService.fetchUser(creatingUserUid)["role"] as? String ?: "").lowercase()
                                if (creatorRole == "doctor") {
                                    extraFields["assignedDoctorId"] = creatingUserUid
                                }
                            }
                            // Flag for first login password change prompt
                            extraFields["mustChangePassword"] = true

                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("users").document(newPatientUid)
                                .update(extraFields)
                                .await()

                            // Show success and navigate back
                            snackbarHostState.showSnackbar("Patient account created! Default password: password@123")
                            onSaved()
                        } catch (e: Exception) {
                            isLoading = false
                            errorMessage = ErrorHandler.getDisplayMessage(e, "add patient")
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
