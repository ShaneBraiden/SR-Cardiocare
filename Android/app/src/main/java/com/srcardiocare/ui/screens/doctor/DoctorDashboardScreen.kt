// DoctorDashboardScreen.kt — Doctor's patient overview
package com.srcardiocare.ui.screens.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.srcardiocare.data.firebase.FirebaseService
import com.srcardiocare.ui.theme.DesignTokens

private enum class PatientStatus { ON_TRACK, NEEDS_ATTENTION, INACTIVE }

private data class PatientItem(
    val id: String,
    val name: String,
    val condition: String,
    val status: PatientStatus,
    val compliance: Int,
    val isOnline: Boolean = false,
    val initials: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDashboardScreen(
    onPatientTap: (String) -> Unit,
    onAddPatient: () -> Unit,
    onAddDoctor: () -> Unit,
    onExerciseLibrary: () -> Unit,
    onSchedule: () -> Unit,
    onVideoUpload: () -> Unit = {},
    onProfile: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var allPatients by remember { mutableStateOf<List<PatientItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var doctorName by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val uid = FirebaseService.currentUID
            if (uid == null) {
                errorMessage = "Not signed in. Please restart the app."
                isLoading = false
                return@LaunchedEffect
            }
            val userData = FirebaseService.fetchUser(uid)
            val firstName = userData["firstName"] as? String ?: ""
            val lastName  = userData["lastName"]  as? String ?: ""
            val role      = (userData["role"]     as? String ?: "").lowercase()
            userRole = role
            doctorName = when (role) {
                "doctor" -> "Dr. $lastName"
                "admin"  -> "$firstName $lastName (Admin)"
                else     -> "$firstName $lastName"
            }

            // Admins see all patients; doctors see only their assigned patients
            val patients = if (role == "admin") {
                FirebaseService.fetchAllPatients()
            } else {
                FirebaseService.fetchPatients(uid)
            }

            allPatients = patients.map { (id, data) ->
                val fName = data["firstName"] as? String ?: ""
                val lName = data["lastName"]  as? String ?: ""
                val injuries = (data["injuries"] as? List<*>)?.firstOrNull()?.toString() ?: "No condition listed"
                val initials = "${fName.firstOrNull() ?: ""}${lName.firstOrNull() ?: ""}".uppercase()

                // Compute online status from lastSeen
                val lastSeen = data["lastSeen"]
                val isOnline = when (lastSeen) {
                    is com.google.firebase.Timestamp -> {
                        val seenMs = lastSeen.toDate().time
                        val nowMs = System.currentTimeMillis()
                        (nowMs - seenMs) < 5 * 60 * 1000 // Active within 5 minutes
                    }
                    else -> false
                }

                PatientItem(
                    id          = id,
                    name        = "$fName $lName".trim().ifBlank { "Unknown" },
                    condition   = injuries,
                    status      = PatientStatus.ON_TRACK,
                    compliance  = 0,
                    isOnline    = isOnline,
                    initials    = initials.ifBlank { "?" }
                )
            }
        } catch (e: Exception) {
            errorMessage = "Could not load patients: ${e.message}"
        }
        isLoading = false
    }

    val filteredPatients = if (searchQuery.isBlank()) allPatients else allPatients.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.condition.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (userRole == "admin") {
                    ExtendedFloatingActionButton(
                        onClick = onAddDoctor,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = DesignTokens.Colors.Primary,
                        icon = { Icon(Icons.Default.Add, contentDescription = "Add Doctor") },
                        text = { Text("Add Doctor", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                FloatingActionButton(
                    onClick = onAddPatient,
                    containerColor = DesignTokens.Colors.Primary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Patient", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(selected = true, onClick = {}, label = { Text("Patients") }, icon = { Text("👥") })
                NavigationBarItem(selected = false, onClick = onExerciseLibrary, label = { Text("Exercises") }, icon = { Text("🏋️") })
                NavigationBarItem(selected = false, onClick = onVideoUpload, label = { Text("Videos") }, icon = { Text("📹") })
                NavigationBarItem(selected = false, onClick = onSchedule, label = { Text("Schedule") }, icon = { Text("📅") })
                NavigationBarItem(selected = false, onClick = onProfile, label = { Text("Profile") }, icon = { Text("👤") })
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Header
            item {
                Column(modifier = Modifier.padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.MD)) {
                    Text("Welcome back,", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(doctorName.ifBlank { "Loading…" }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }
            }

            // Stats summary card
            if (!isLoading && errorMessage == null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DesignTokens.Spacing.XL),
                        shape = RoundedCornerShape(DesignTokens.Radius.Card),
                        colors = CardDefaults.cardColors(containerColor = DesignTokens.Colors.Primary)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(DesignTokens.Spacing.XL),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatItem(value = allPatients.size.toString(), label = "Total Patients")
                            StatItem(value = allPatients.count { it.status == PatientStatus.ON_TRACK }.toString(), label = "On Track")
                            StatItem(value = allPatients.count { it.isOnline }.toString(), label = "Online Now")
                        }
                    }
                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
                }
            }

            // Search
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search patients…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.XL),
                    shape = RoundedCornerShape(DesignTokens.Radius.Base),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DesignTokens.Colors.Primary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
            }

            // Section header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.SM),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Patient Overview", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        "${filteredPatients.size} patient${if (filteredPatients.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Loading
            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.XXXL), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DesignTokens.Colors.Primary)
                    }
                }
            }

            // Error state
            errorMessage?.let { msg ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.SM),
                        shape = RoundedCornerShape(DesignTokens.Radius.Card),
                        colors = CardDefaults.cardColors(containerColor = DesignTokens.Colors.Error.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(DesignTokens.Spacing.XL)) {
                            Text("Failed to load patients", fontWeight = FontWeight.SemiBold, color = DesignTokens.Colors.Error)
                            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XS))
                            Text(msg, style = MaterialTheme.typography.bodySmall, color = DesignTokens.Colors.Error)
                            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
                            Text(
                                "Tip: Make sure you have run seed_data.py and Firestore rules allow authenticated reads.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Empty state (no patients and no error)
            if (!isLoading && errorMessage == null && filteredPatients.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.SM),
                        shape = RoundedCornerShape(DesignTokens.Radius.Card),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(DesignTokens.Spacing.XXXL),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("👥", style = MaterialTheme.typography.displaySmall)
                            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
                            Text(
                                if (searchQuery.isBlank()) "No patients yet" else "No patients match \"$searchQuery\"",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XS))
                            Text(
                                if (searchQuery.isBlank()) "Tap + to add your first patient" else "Try a different search term",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Patient rows
            items(filteredPatients) { patient ->
                PatientRow(patient = patient, onClick = { onPatientTap(patient.id) })
            }
        }
    }
}

@Composable
private fun PatientRow(patient: PatientItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.XS)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(DesignTokens.Radius.LG),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DesignTokens.Colors.PrimaryLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(patient.initials, fontWeight = FontWeight.Bold, color = DesignTokens.Colors.PrimaryDark)
                }
                if (patient.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(DesignTokens.Colors.Success)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(DesignTokens.Spacing.MD))

            Column(modifier = Modifier.weight(1f)) {
                Text(patient.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(patient.condition, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Status badge
            val (statusText, statusColor) = when (patient.status) {
                PatientStatus.ON_TRACK -> "On Track" to DesignTokens.Colors.Success
                PatientStatus.NEEDS_ATTENTION -> "Attention" to DesignTokens.Colors.Warning
                PatientStatus.INACTIVE -> "Inactive" to DesignTokens.Colors.NeutralDark
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(DesignTokens.Radius.Full))
                    .background(statusColor.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(statusText, style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
        )
    }
}
