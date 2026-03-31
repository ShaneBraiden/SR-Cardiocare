// DoctorProfileScreen.kt — Doctor / Admin profile & account settings
package com.srcardiocare.ui.screens.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onChangePassword: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var firstName  by remember { mutableStateOf("") }
    var lastName   by remember { mutableStateOf("") }
    var email      by remember { mutableStateOf("") }
    var role       by remember { mutableStateOf("") }
    var speciality by remember { mutableStateOf<String?>(null) }
    var clinic     by remember { mutableStateOf<String?>(null) }
    var isLoading  by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val uid = FirebaseService.currentUID ?: return@LaunchedEffect
            val data = FirebaseService.fetchUser(uid)
            firstName  = data["firstName"]  as? String ?: ""
            lastName   = data["lastName"]   as? String ?: ""
            email      = data["email"]      as? String ?: ""
            role       = (data["role"]      as? String ?: "").replaceFirstChar { it.uppercase() }
            speciality = data["speciality"] as? String
            clinic     = data["clinicName"] as? String
        } catch (_: Exception) { }
        isLoading = false
    }

    // ── Logout confirm dialog ────────────────────────────────────────────
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                .padding(horizontal = DesignTokens.Spacing.XL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

            // ── Avatar ──────────────────────────────────────────────────
            val initials = "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}".uppercase()
            InitialsAvatar(initials = initials, size = 88.dp)

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            Text(
                text = if (role.lowercase() == "doctor") "Dr. $firstName $lastName" else "$firstName $lastName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Role badge
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XS))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(DesignTokens.Radius.Full))
                    .background(DesignTokens.Colors.PrimaryLight)
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(role, style = MaterialTheme.typography.labelMedium, color = DesignTokens.Colors.PrimaryDark, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XXL))

            // ── Admin Settings ──────────────────────────────────────────
            if (role.lowercase() == "admin") {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = DesignTokens.Spacing.XL),
                    shape = RoundedCornerShape(DesignTokens.Radius.Card),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(DesignTokens.Spacing.XL),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Enforce Session Locks", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                            Text("Block completed or expired workouts so patients cannot bypass daily limits.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        var sessionLocksEnabled by remember { mutableStateOf(true) }
                        LaunchedEffect(Unit) {
                            sessionLocksEnabled = FirebaseService.fetchSessionLocksEnabled()
                        }
                        Switch(
                            checked = sessionLocksEnabled,
                            onCheckedChange = { isChecked ->
                                sessionLocksEnabled = isChecked
                                scope.launch {
                                    FirebaseService.updateSessionLocksEnabled(isChecked)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = DesignTokens.Colors.Primary)
                        )
                    }
                }
            }

            // ── Info Card ───────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Card),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(DesignTokens.Spacing.XL)) {
                    Text("Account Information", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
                    ProfileInfoRow(label = "First Name", value = firstName)
                    ProfileInfoRow(label = "Last Name",  value = lastName)
                    ProfileInfoRow(label = "Email",      value = email)
                    ProfileInfoRow(label = "Role",       value = role)
                    if (!speciality.isNullOrBlank()) ProfileInfoRow(label = "Speciality", value = speciality!!)
                    if (!clinic.isNullOrBlank())     ProfileInfoRow(label = "Clinic",     value = clinic!!)
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

            // ── Change Password ──────────────────────────────────────────
            Button(
                onClick = onChangePassword,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(DesignTokens.Radius.Button),
                colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.Primary)
            ) {
                Text("Change Password", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            // ── Sign Out ────────────────────────────────────────────────
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
