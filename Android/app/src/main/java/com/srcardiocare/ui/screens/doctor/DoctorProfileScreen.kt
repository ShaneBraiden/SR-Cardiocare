// DoctorProfileScreen.kt — Doctor / Admin profile (editable, profile-only)
package com.srcardiocare.ui.screens.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.srcardiocare.core.auth.AuthManager
import com.srcardiocare.core.security.InputValidator
import com.srcardiocare.data.firebase.FirebaseService
import com.srcardiocare.ui.components.InitialsAvatar
import com.srcardiocare.ui.components.LogoutConfirmDialog
import com.srcardiocare.ui.components.ProfileInfoRow
import com.srcardiocare.ui.theme.DesignTokens
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onChangePassword: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var licenseNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    var editFirstName by remember { mutableStateOf("") }
    var editLastName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editLicenseNumber by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val uid = FirebaseService.currentUID ?: return@LaunchedEffect
            val data = FirebaseService.fetchUser(uid)
            firstName = data["firstName"] as? String ?: ""
            lastName = data["lastName"] as? String ?: ""
            email = data["email"] as? String ?: ""
            phone = data["phone"] as? String ?: ""
            role = (data["role"] as? String ?: "").replaceFirstChar { it.uppercase() }
            licenseNumber = data["licenseNumber"] as? String ?: ""
        } catch (_: Exception) { }
        isLoading = false
    }

    LogoutConfirmDialog(
        show = showLogoutDialog,
        onDismiss = { showLogoutDialog = false },
        onConfirm = {
            FirebaseService.logout()
            AuthManager(context).clearAll()
            onLogout()
        }
    )

    fun beginEdit() {
        editFirstName = firstName
        editLastName = lastName
        editPhone = phone
        editLicenseNumber = licenseNumber
        isEditing = true
    }

    fun saveEdits() {
        val nameValidation = InputValidator.validateName(
            "${editFirstName.trim()} ${editLastName.trim()}".trim(),
            "Name"
        )
        if (!nameValidation.isValid) {
            scope.launch { snackbarHostState.showSnackbar(nameValidation.errorMessage ?: "Invalid name") }
            return
        }
        val phoneValidation = InputValidator.validatePhone(editPhone)
        if (!phoneValidation.isValid) {
            scope.launch { snackbarHostState.showSnackbar(phoneValidation.errorMessage ?: "Invalid phone") }
            return
        }
        isSaving = true
        scope.launch {
            try {
                val updates = mutableMapOf<String, Any>(
                    "firstName" to editFirstName.trim(),
                    "lastName" to editLastName.trim(),
                    "phone" to phoneValidation.sanitizedValue,
                    "licenseNumber" to editLicenseNumber.trim()
                )
                FirebaseService.updateUser(updates)
                firstName = editFirstName.trim()
                lastName = editLastName.trim()
                phone = phoneValidation.sanitizedValue
                licenseNumber = editLicenseNumber.trim()
                isEditing = false
                snackbarHostState.showSnackbar("Profile updated")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(e.message ?: "Failed to update profile")
            }
            isSaving = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isEditing && !isLoading) {
                        IconButton(onClick = { beginEdit() }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit profile")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DesignTokens.Colors.Primary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = DesignTokens.Spacing.XL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

            val initials = "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}".uppercase()
            InitialsAvatar(initials = initials.ifBlank { "?" }, size = 88.dp)

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            Text(
                text = if (role.equals("Doctor", ignoreCase = true)) "Dr. $firstName $lastName" else "$firstName $lastName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XS))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(DesignTokens.Radius.Full))
                    .background(DesignTokens.Colors.PrimaryLight)
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    role,
                    style = MaterialTheme.typography.labelMedium,
                    color = DesignTokens.Colors.PrimaryDark,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XXL))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Card),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(DesignTokens.Spacing.XL)) {
                    Text("Account Information", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

                    if (isEditing) {
                        OutlinedTextField(
                            value = editFirstName,
                            onValueChange = { editFirstName = InputValidator.limitLength(it, InputValidator.MaxLength.NAME) },
                            label = { Text("First Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))
                        OutlinedTextField(
                            value = editLastName,
                            onValueChange = { editLastName = InputValidator.limitLength(it, InputValidator.MaxLength.NAME) },
                            label = { Text("Last Name (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))
                        OutlinedTextField(
                            value = editPhone,
                            onValueChange = { editPhone = InputValidator.limitLength(it, InputValidator.MaxLength.PHONE) },
                            label = { Text("Phone") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        if (role.equals("Doctor", ignoreCase = true)) {
                            Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))
                            OutlinedTextField(
                                value = editLicenseNumber,
                                onValueChange = { editLicenseNumber = InputValidator.limitLength(it, InputValidator.MaxLength.LICENSE_NUMBER) },
                                label = { Text("License Number") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))
                        ProfileInfoRow(label = "Email (not editable)", value = email, showDivider = false)
                    } else {
                        ProfileInfoRow(label = "First Name", value = firstName)
                        ProfileInfoRow(label = "Last Name", value = lastName)
                        ProfileInfoRow(label = "Email", value = email)
                        ProfileInfoRow(label = "Phone", value = phone.ifBlank { "—" })
                        ProfileInfoRow(label = "Role", value = role, showDivider = role.equals("Doctor", ignoreCase = true) && licenseNumber.isNotBlank())
                        if (role.equals("Doctor", ignoreCase = true) && licenseNumber.isNotBlank()) {
                            ProfileInfoRow(label = "License Number", value = licenseNumber, showDivider = false)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

            if (isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.MD)
                ) {
                    OutlinedButton(
                        onClick = { isEditing = false },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(DesignTokens.Radius.Button),
                        enabled = !isSaving
                    ) { Text("Cancel") }

                    Button(
                        onClick = { saveEdits() },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(DesignTokens.Radius.Button),
                        colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.Primary),
                        enabled = !isSaving
                    ) {
                        if (isSaving) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
            }

            Button(
                onClick = onChangePassword,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Button),
                colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.Primary)
            ) {
                Text("Change Password", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            OutlinedButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Button),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DesignTokens.Colors.Error),
                border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.Error)
            ) {
                Text("Sign Out", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))
        }
    }
}
