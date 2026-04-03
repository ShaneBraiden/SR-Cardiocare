// DoctorProfileScreen.kt — Doctor / Admin profile & account settings
package com.srcardiocare.ui.screens.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.srcardiocare.core.auth.AuthManager
import com.srcardiocare.data.firebase.FirebaseService
import com.srcardiocare.ui.components.InitialsAvatar
import com.srcardiocare.ui.components.LogoutConfirmDialog
import com.srcardiocare.ui.components.ProfileInfoRow
import com.srcardiocare.ui.theme.DesignTokens
import kotlinx.coroutines.launch

private data class AdminAccessUser(
    val id: String,
    val name: String,
    val role: String,
    val isBlocked: Boolean,
    val blockReason: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onChangePassword: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var speciality by remember { mutableStateOf<String?>(null) }
    var clinic by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Admin settings states
    var sessionLocksEnabled by remember { mutableStateOf(true) }
    var blockAllPatients by remember { mutableStateOf(false) }
    var blockAllDoctors by remember { mutableStateOf(false) }
    var isGlobalSettingsLoading by remember { mutableStateOf(false) }

    // Admin user-level blocking states
    var accessUsers by remember { mutableStateOf<List<AdminAccessUser>>(emptyList()) }
    var isAccessUsersLoading by remember { mutableStateOf(false) }
    var updatingUserIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var accessFilter by remember { mutableStateOf("all") }
    var adminStatusMessage by remember { mutableStateOf<String?>(null) }

    val isAdmin = role.equals("Admin", ignoreCase = true)

    suspend fun loadAdminAccessControls() {
        isGlobalSettingsLoading = true
        isAccessUsersLoading = true
        try {
            val settings = FirebaseService.fetchAccessControlSettings()
            sessionLocksEnabled = settings.sessionLocksEnabled
            blockAllPatients = settings.blockAllPatients
            blockAllDoctors = settings.blockAllDoctors

            val currentUid = FirebaseService.currentUID
            val users = FirebaseService.fetchAllUsers()
            accessUsers = users.mapNotNull { (uid, data) ->
                if (uid == currentUid) return@mapNotNull null

                val userRole = (data["role"] as? String ?: "").lowercase()
                if (userRole != "patient" && userRole != "doctor") return@mapNotNull null

                val first = data["firstName"] as? String ?: ""
                val last = data["lastName"] as? String ?: ""
                val name = "$first $last".trim().ifBlank { data["email"] as? String ?: "Unknown" }

                AdminAccessUser(
                    id = uid,
                    name = name,
                    role = userRole,
                    isBlocked = data["isBlocked"] as? Boolean ?: false,
                    blockReason = data["blockReason"] as? String ?: ""
                )
            }.sortedWith(compareBy<AdminAccessUser> { it.role }.thenBy { it.name })
        } catch (e: Exception) {
            adminStatusMessage = "Failed to load admin settings: ${e.message}"
        }
        isGlobalSettingsLoading = false
        isAccessUsersLoading = false
    }

    LaunchedEffect(Unit) {
        try {
            val uid = FirebaseService.currentUID ?: return@LaunchedEffect
            val data = FirebaseService.fetchUser(uid)
            firstName = data["firstName"] as? String ?: ""
            lastName = data["lastName"] as? String ?: ""
            email = data["email"] as? String ?: ""
            role = (data["role"] as? String ?: "").replaceFirstChar { it.uppercase() }
            speciality = data["speciality"] as? String
            clinic = data["clinicName"] as? String

            if ((data["role"] as? String ?: "").equals("admin", ignoreCase = true)) {
                loadAdminAccessControls()
            }
        } catch (_: Exception) {
            adminStatusMessage = "Unable to load profile details."
        }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
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
                .padding(horizontal = DesignTokens.Spacing.XL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

            val initials = "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}".uppercase()
            InitialsAvatar(initials = initials, size = 88.dp)

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

            if (isAdmin) {
                AdminBlockingSettingsCard(
                    sessionLocksEnabled = sessionLocksEnabled,
                    blockAllPatients = blockAllPatients,
                    blockAllDoctors = blockAllDoctors,
                    isLoading = isGlobalSettingsLoading,
                    onSessionLocksChanged = { enabled ->
                        sessionLocksEnabled = enabled
                        scope.launch {
                            try {
                                FirebaseService.updateAccessControlSettings(sessionLocksEnabled = enabled)
                                adminStatusMessage = "Session lock policy updated."
                            } catch (e: Exception) {
                                adminStatusMessage = "Failed to update session lock policy: ${e.message}"
                            }
                        }
                    },
                    onBlockAllPatientsChanged = { blocked ->
                        blockAllPatients = blocked
                        scope.launch {
                            try {
                                FirebaseService.updateAccessControlSettings(blockAllPatients = blocked)
                                adminStatusMessage = if (blocked) {
                                    "All patient API access is blocked."
                                } else {
                                    "Patient API access policy restored."
                                }
                            } catch (e: Exception) {
                                adminStatusMessage = "Failed to update patient policy: ${e.message}"
                            }
                        }
                    },
                    onBlockAllDoctorsChanged = { blocked ->
                        blockAllDoctors = blocked
                        scope.launch {
                            try {
                                FirebaseService.updateAccessControlSettings(blockAllDoctors = blocked)
                                adminStatusMessage = if (blocked) {
                                    "All doctor API access is blocked."
                                } else {
                                    "Doctor API access policy restored."
                                }
                            } catch (e: Exception) {
                                adminStatusMessage = "Failed to update doctor policy: ${e.message}"
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

                AdminUserAccessCard(
                    users = accessUsers,
                    accessFilter = accessFilter,
                    onFilterChanged = { accessFilter = it },
                    isLoading = isAccessUsersLoading,
                    updatingUserIds = updatingUserIds,
                    onRefresh = {
                        scope.launch { loadAdminAccessControls() }
                    },
                    onUserBlockChanged = { user, blocked ->
                        updatingUserIds = updatingUserIds + user.id
                        accessUsers = accessUsers.map {
                            if (it.id == user.id) {
                                it.copy(
                                    isBlocked = blocked,
                                    blockReason = if (blocked) {
                                        if (it.blockReason.isBlank()) "Blocked from admin settings" else it.blockReason
                                    } else {
                                        ""
                                    }
                                )
                            } else {
                                it
                            }
                        }

                        scope.launch {
                            try {
                                FirebaseService.setUserAccessBlocked(
                                    uid = user.id,
                                    blocked = blocked,
                                    reason = if (blocked) "Blocked from admin settings" else null
                                )
                                adminStatusMessage = if (blocked) {
                                    "${user.name} access blocked."
                                } else {
                                    "${user.name} access restored."
                                }
                            } catch (e: Exception) {
                                adminStatusMessage = "Failed to update ${user.name}: ${e.message}"
                                // Reload to ensure UI stays consistent with backend.
                                loadAdminAccessControls()
                            }
                            updatingUserIds = updatingUserIds - user.id
                        }
                    }
                )

                adminStatusMessage?.let { message ->
                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Card),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(DesignTokens.Spacing.XL)) {
                    Text("Account Information", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
                    ProfileInfoRow(label = "First Name", value = firstName)
                    ProfileInfoRow(label = "Last Name", value = lastName)
                    ProfileInfoRow(label = "Email", value = email)
                    ProfileInfoRow(label = "Role", value = role)
                    if (!speciality.isNullOrBlank()) ProfileInfoRow(label = "Speciality", value = speciality!!)
                    if (!clinic.isNullOrBlank()) ProfileInfoRow(label = "Clinic", value = clinic!!)
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

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

@Composable
private fun AdminBlockingSettingsCard(
    sessionLocksEnabled: Boolean,
    blockAllPatients: Boolean,
    blockAllDoctors: Boolean,
    isLoading: Boolean,
    onSessionLocksChanged: (Boolean) -> Unit,
    onBlockAllPatientsChanged: (Boolean) -> Unit,
    onBlockAllDoctorsChanged: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.Radius.Card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.XL)) {
            Text("Admin Blocking Settings", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XS))
            Text(
                "All blocking controls are managed here for API/session access.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = DesignTokens.Colors.Primary)
            } else {
                AdminPolicyToggleRow(
                    title = "Enforce Session Locks",
                    subtitle = "Block completed or expired workout sessions.",
                    checked = sessionLocksEnabled,
                    onCheckedChange = onSessionLocksChanged
                )
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))
                AdminPolicyToggleRow(
                    title = "Block All Patients API",
                    subtitle = "Stops all patient accounts from app/API access.",
                    checked = blockAllPatients,
                    onCheckedChange = onBlockAllPatientsChanged
                )
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))
                AdminPolicyToggleRow(
                    title = "Block All Doctors API",
                    subtitle = "Stops all doctor accounts from app/API access.",
                    checked = blockAllDoctors,
                    onCheckedChange = onBlockAllDoctorsChanged
                )
            }
        }
    }
}

@Composable
private fun AdminPolicyToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(DesignTokens.Spacing.SM))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = DesignTokens.Colors.Primary)
        )
    }
}

@Composable
private fun AdminUserAccessCard(
    users: List<AdminAccessUser>,
    accessFilter: String,
    onFilterChanged: (String) -> Unit,
    isLoading: Boolean,
    updatingUserIds: Set<String>,
    onRefresh: () -> Unit,
    onUserBlockChanged: (AdminAccessUser, Boolean) -> Unit
) {
    val filteredUsers = users.filter {
        when (accessFilter) {
            "doctors" -> it.role == "doctor"
            "patients" -> it.role == "patient"
            else -> true
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.Radius.Card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.XL)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("User-Level Blocking", fontWeight = FontWeight.Bold)
                    Text(
                        "Block/unblock specific patient and doctor accounts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = onRefresh) {
                    Text("Refresh")
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))

            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.SM)) {
                FilterChip(
                    selected = accessFilter == "all",
                    onClick = { onFilterChanged("all") },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = accessFilter == "doctors",
                    onClick = { onFilterChanged("doctors") },
                    label = { Text("Doctors") }
                )
                FilterChip(
                    selected = accessFilter == "patients",
                    onClick = { onFilterChanged("patients") },
                    label = { Text("Patients") }
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = DesignTokens.Colors.Primary
                    )
                }

                filteredUsers.isEmpty() -> {
                    Text(
                        "No users available for this filter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    filteredUsers.forEach { user ->
                        val isUpdating = updatingUserIds.contains(user.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(user.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${user.role.replaceFirstChar { it.uppercase() }} • ${if (user.isBlocked) "Blocked" else "Active"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (user.blockReason.isNotBlank()) {
                                    Text(
                                        user.blockReason,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height(22.dp),
                                    strokeWidth = 2.dp,
                                    color = DesignTokens.Colors.Primary
                                )
                            } else {
                                Switch(
                                    checked = user.isBlocked,
                                    onCheckedChange = { checked -> onUserBlockChanged(user, checked) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = DesignTokens.Colors.Primary)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
