// PatientProfileSelfScreen.kt — Patient's own profile screen
package com.srcardiocare.ui.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
fun PatientProfileSelfScreen(
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
    var condition by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var assignedDoctor by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Editable fields (staging buffer)
    var editFirstName by remember { mutableStateOf("") }
    var editLastName by remember { mutableStateOf("") }
    var editCondition by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val uid = FirebaseService.currentUID ?: return@LaunchedEffect
            val userData = FirebaseService.fetchUser(uid)
            firstName = userData["firstName"] as? String ?: ""
            lastName = userData["lastName"] as? String ?: ""
            email = userData["email"] as? String ?: ""
            phone = userData["phone"] as? String ?: ""
            val injuryList = userData["injuries"] as? List<*>
            condition = injuryList?.joinToString(", ") ?: ""

            val doctorId = userData["assignedDoctorId"] as? String
            if (doctorId != null) {
                try {
                    val doctorData = FirebaseService.fetchUser(doctorId)
                    val dFirst = doctorData["firstName"] as? String ?: ""
                    val dLast = doctorData["lastName"] as? String ?: ""
                    assignedDoctor = "Dr. $dLast"
                    if (assignedDoctor == "Dr. ") assignedDoctor = "$dFirst $dLast".trim()
                } catch (_: Exception) {
                    assignedDoctor = "Unknown"
                }
            } else {
                assignedDoctor = "Not assigned"
            }
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

    val initials = "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}".uppercase()

    fun beginEdit() {
        editFirstName = firstName
        editLastName = lastName
        editCondition = condition
        editPhone = phone
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
                    "phone" to phoneValidation.sanitizedValue
                )
                val trimmedCondition = editCondition.trim()
                updates["injuries"] = if (trimmedCondition.isBlank()) emptyList<String>() else listOf(trimmedCondition)
                FirebaseService.updateUser(updates)
                firstName = editFirstName.trim()
                lastName = editLastName.trim()
                phone = phoneValidation.sanitizedValue
                condition = trimmedCondition
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
                title = { Text("My Profile", fontWeight = FontWeight.Bold) },
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
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.XXXL))

                InitialsAvatar(initials = initials.ifBlank { "?" }, size = 96.dp)

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
                Text(
                    "$firstName $lastName".trim().ifBlank { "Patient" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.XXL))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.XL),
                    shape = RoundedCornerShape(DesignTokens.Radius.Card),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(DesignTokens.Spacing.XL)) {
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
                            Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))
                            OutlinedTextField(
                                value = editCondition,
                                onValueChange = { editCondition = InputValidator.limitLength(it, InputValidator.MaxLength.INJURY_TYPE) },
                                label = { Text("Condition") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))
                            ProfileInfoRow(label = "Email (not editable)", value = email, showDivider = false)
                            ProfileInfoRow(label = "Assigned Doctor", value = assignedDoctor, showDivider = false)
                        } else {
                            ProfileInfoRow(label = "First Name", value = firstName)
                            ProfileInfoRow(label = "Last Name", value = lastName)
                            ProfileInfoRow(label = "Phone", value = phone.ifBlank { "—" })
                            ProfileInfoRow(label = "Condition", value = condition.ifBlank { "None listed" })
                            ProfileInfoRow(label = "Assigned Doctor", value = assignedDoctor)
                            ProfileInfoRow(label = "Email", value = email, showDivider = false)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

                if (isEditing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DesignTokens.Spacing.XL),
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.MD)
                    ) {
                        OutlinedButton(
                            onClick = { isEditing = false },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(DesignTokens.Radius.Base),
                            enabled = !isSaving
                        ) { Text("Cancel") }

                        Button(
                            onClick = { saveEdits() },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(DesignTokens.Radius.Base),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.XL)
                        .height(52.dp),
                    shape = RoundedCornerShape(DesignTokens.Radius.Base),
                    colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.Primary)
                ) {
                    Text("Change Password", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.XL)
                        .height(52.dp),
                    shape = RoundedCornerShape(DesignTokens.Radius.Base),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DesignTokens.Colors.Error.copy(alpha = 0.1f),
                        contentColor = DesignTokens.Colors.Error
                    )
                ) {
                    Text("Sign Out", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.XXXL))
            }
        }
    }
}
