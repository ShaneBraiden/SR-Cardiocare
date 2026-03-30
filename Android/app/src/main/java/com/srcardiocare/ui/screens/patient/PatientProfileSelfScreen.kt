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
import com.srcardiocare.ui.theme.DesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientProfileSelfScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onChangePassword: () -> Unit = {}
) {
    val context = LocalContext.current
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var injuries by remember { mutableStateOf("") }
    var assignedDoctor by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val uid = FirebaseService.currentUID ?: return@LaunchedEffect
            val userData = FirebaseService.fetchUser(uid)
            firstName = userData["firstName"] as? String ?: ""
            lastName = userData["lastName"] as? String ?: ""
            email = userData["email"] as? String ?: ""
            val injuryList = userData["injuries"] as? List<*>
            injuries = injuryList?.joinToString(", ") ?: "None listed"

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

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(onClick = {
                    FirebaseService.logout()
                    AuthManager(context).clearAll()
                    onLogout()
                }) {
                    Text("Sign Out", color = DesignTokens.Colors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val initials = "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}".uppercase()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
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
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.XXXL))

                // Avatar
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(DesignTokens.Colors.PrimaryLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        initials.ifBlank { "?" },
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = DesignTokens.Colors.PrimaryDark
                    )
                }

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
                Text(
                    "$firstName $lastName".trim().ifBlank { "Patient" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.XXL))

                // Info card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.XL),
                    shape = RoundedCornerShape(DesignTokens.Radius.Card),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(DesignTokens.Spacing.XL)) {
                        ProfileInfoRow("Injuries / Condition", injuries)
                        HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.MD), color = DesignTokens.Colors.NeutralLight)
                        ProfileInfoRow("Assigned Doctor", assignedDoctor)
                        HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.MD), color = DesignTokens.Colors.NeutralLight)
                        ProfileInfoRow("Email", email)
                    }
                }

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

                // Change password button
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

                // Sign out button
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

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}
