// PatientProfileScreen.kt — Patient detail view for doctors with exercise assignment
package com.srcardiocare.ui.screens.doctor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.srcardiocare.data.firebase.FirebaseService
import com.srcardiocare.ui.theme.DesignTokens
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientProfileScreen(patientId: String, onBack: () -> Unit, onVideoUpload: () -> Unit) {
    var patientName by remember { mutableStateOf("") }
    var patientCondition by remember { mutableStateOf("") }
    var patientInitials by remember { mutableStateOf("") }
    var complianceText by remember { mutableStateOf("--") }
    var painText by remember { mutableStateOf("--") }
    var progressText by remember { mutableStateOf("--") }
    var exerciseItems by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Exercise assignment state
    var showAssignDialog by remember { mutableStateOf(false) }
    var availableExercises by remember { mutableStateOf<List<Pair<String, Map<String, Any?>>>>(emptyList()) }
    var isLoadingExercises by remember { mutableStateOf(false) }
    var assignMessage by remember { mutableStateOf<String?>(null) }

    // Admin doctor assignment state
    var currentUserRole by remember { mutableStateOf("") }
    var allDoctors by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) } // id to name
    var currentAssignedDoctorId by remember { mutableStateOf("") }
    var showDoctorPicker by remember { mutableStateOf(false) }
    var isAssigningDoctor by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar when assignMessage changes
    LaunchedEffect(assignMessage) {
        assignMessage?.let {
            snackbarHostState.showSnackbar(it)
            assignMessage = null
        }
    }

    // Refresh function to reload patient data
    suspend fun loadPatientData() {
        try {
            val userData = FirebaseService.fetchUser(patientId)
            val firstName = userData["firstName"] as? String ?: ""
            val lastName = userData["lastName"] as? String ?: ""
            patientName = "$firstName $lastName".trim()
            patientInitials = "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}".uppercase()
            val injuries = (userData["injuries"] as? List<*>)?.firstOrNull()?.toString() ?: ""
            patientCondition = injuries

            val plans = FirebaseService.fetchPlans(patientId)
            val activePlan = plans.firstOrNull { (it.second["isActive"] as? Boolean) == true }
            if (activePlan != null) {
                val exercises = activePlan.second["exercises"] as? List<*> ?: emptyList<Any>()
                exerciseItems = exercises.mapNotNull { ex ->
                    val exMap = ex as? Map<*, *> ?: return@mapNotNull null
                    val name = exMap["name"] as? String ?: "Exercise"
                    val sets = (exMap["customSets"] as? Number)?.toInt() ?: 0
                    val reps = (exMap["customReps"] as? Number)?.toInt() ?: 0
                    name to "$sets Sets • $reps Reps"
                }
            }

            val workouts = FirebaseService.fetchWorkouts(patientId)
            val totalWorkouts = workouts.size
            val completedWorkouts = workouts.count { it.second["completedAt"] != null }
            if (totalWorkouts > 0) {
                complianceText = "${(completedWorkouts * 100 / totalWorkouts)}%"
            }
        } catch (_: Exception) { }
    }

    LaunchedEffect(patientId) {
        // Load current user role
        try {
            val uid = FirebaseService.currentUID
            if (uid != null) {
                val me = FirebaseService.fetchUser(uid)
                currentUserRole = (me["role"] as? String ?: "").lowercase()
            }
        } catch (_: Exception) { }

        loadPatientData()

        // Fetch assigned doctor and all doctors (for admin picker)
        try {
            val userData = FirebaseService.fetchUser(patientId)
            currentAssignedDoctorId = userData["assignedDoctorId"] as? String ?: ""
            if (currentUserRole == "admin") {
                val doctors = FirebaseService.fetchAllDoctors()
                allDoctors = doctors.map { (id, data) ->
                    val fName = data["firstName"] as? String ?: ""
                    val lName = data["lastName"] as? String ?: ""
                    id to "Dr. $lName".let { if (lName.isBlank()) fName else it }
                }
            }
        } catch (_: Exception) { }

        isLoading = false
    }

    // Exercise assignment dialog
    if (showAssignDialog) {
        AlertDialog(
            onDismissRequest = { showAssignDialog = false },
            title = { Text("Assign Exercise", fontWeight = FontWeight.Bold) },
            text = {
                if (isLoadingExercises) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DesignTokens.Colors.Primary)
                    }
                } else if (availableExercises.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📁", style = MaterialTheme.typography.displaySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No exercises in library", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Upload some videos first", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(availableExercises) { (id, data) ->
                            val name = data["name"] as? String ?: data["title"] as? String ?: "Unnamed Exercise"
                            val category = data["category"] as? String ?: ""
                            val difficulty = data["difficulty"] as? String ?: ""

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        scope.launch {
                                            try {
                                                val exerciseData = mapOf<String, Any>(
                                                    "exerciseId" to id,
                                                    "name" to name,
                                                    "category" to category,
                                                    "difficulty" to difficulty,
                                                    "customSets" to 3,
                                                    "customReps" to 10
                                                )
                                                FirebaseService.assignExerciseToPatient(patientId, exerciseData)
                                                showAssignDialog = false
                                                assignMessage = "✅ \"$name\" assigned to $patientName"
                                                // Refresh exercise list
                                                loadPatientData()
                                            } catch (e: Exception) {
                                                assignMessage = "❌ Failed: ${e.message}"
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(DesignTokens.Spacing.MD),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(DesignTokens.Radius.Base))
                                            .background(DesignTokens.Colors.PrimaryLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🏋️", style = MaterialTheme.typography.bodyLarge)
                                    }
                                    Spacer(modifier = Modifier.width(DesignTokens.Spacing.MD))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                        if (category.isNotBlank() || difficulty.isNotBlank()) {
                                            Text(
                                                listOfNotNull(category.ifBlank { null }, difficulty.ifBlank { null }).joinToString(" • "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text("＋", color = DesignTokens.Colors.Primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAssignDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patient Profile", fontWeight = FontWeight.Bold) },
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
        } else {
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

            // Avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(DesignTokens.Colors.PrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Text(patientInitials, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = DesignTokens.Colors.PrimaryDark)
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
            Text(patientName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(patientCondition, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

            // Stats row
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.XL),
                shape = RoundedCornerShape(DesignTokens.Radius.LG),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(DesignTokens.Spacing.MD),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(complianceText, "Compliance")
                    StatItem(painText, "Pain Level")
                    StatItem(progressText, "Progress")
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

            // Action buttons — 3 buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.XL),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.SM)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.MD)) {
                    Button(
                        onClick = { /* Send feedback */ },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(DesignTokens.Radius.Base),
                        colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.Primary)
                    ) {
                        Text("Send Feedback", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(
                        onClick = { /* Edit plan */ },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(DesignTokens.Radius.Base),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DesignTokens.Colors.Primary)
                    ) {
                        Text("Edit Plan", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    }
                }
                // Assign Exercise button — full width
                Button(
                    onClick = {
                        showAssignDialog = true
                        isLoadingExercises = true
                        scope.launch {
                            try {
                                availableExercises = FirebaseService.fetchExercises()
                            } catch (_: Exception) {
                                availableExercises = emptyList()
                            }
                            isLoadingExercises = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(DesignTokens.Radius.Base),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DesignTokens.Colors.Primary.copy(alpha = 0.12f),
                        contentColor = DesignTokens.Colors.Primary
                    )
                ) {
                    Text("📹  Assign Exercise", fontWeight = FontWeight.SemiBold)
                }
            }

            // ── Admin Doctor Assignment ───────────────────────────────────
            if (currentUserRole == "admin" && allDoctors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

                Text(
                    "Assigned Doctor",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = DesignTokens.Spacing.XL)
                )
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))

                val currentDoctorName = allDoctors.firstOrNull { it.first == currentAssignedDoctorId }?.second ?: "Not Assigned"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.XL)
                        .clickable { showDoctorPicker = true },
                    shape = RoundedCornerShape(DesignTokens.Radius.LG),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(DesignTokens.Spacing.MD),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Current: $currentDoctorName", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Tap to change", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isAssigningDoctor) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DesignTokens.Colors.Primary, strokeWidth = 2.dp)
                        } else {
                            Text("✏️", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                if (showDoctorPicker) {
                    AlertDialog(
                        onDismissRequest = { showDoctorPicker = false },
                        title = { Text("Assign Doctor", fontWeight = FontWeight.Bold) },
                        text = {
                            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                                items(allDoctors) { (docId, docName) ->
                                    val isSelected = docId == currentAssignedDoctorId
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable {
                                                isAssigningDoctor = true
                                                showDoctorPicker = false
                                                scope.launch {
                                                    try {
                                                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                            .collection("users").document(patientId)
                                                            .update("assignedDoctorId", docId)
                                                            .await()
                                                        currentAssignedDoctorId = docId
                                                        assignMessage = "✅ Doctor changed to $docName"
                                                    } catch (e: Exception) {
                                                        assignMessage = "❌ Failed: ${e.message}"
                                                    }
                                                    isAssigningDoctor = false
                                                }
                                            },
                                        shape = RoundedCornerShape(DesignTokens.Radius.Base),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) DesignTokens.Colors.Primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(DesignTokens.Spacing.MD),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(docName, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                                            if (isSelected) {
                                                Text("✓", color = DesignTokens.Colors.Primary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showDoctorPicker = false }) { Text("Close") }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

            // Exercises header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.XL),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Assigned Exercises", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "${exerciseItems.size} total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (exerciseItems.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.SM),
                    shape = RoundedCornerShape(DesignTokens.Radius.LG),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(DesignTokens.Spacing.XXL),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏋️", style = MaterialTheme.typography.displaySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No exercises assigned yet", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Text("Tap 'Assign Exercise' above to get started", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Exercise items loaded from Firebase
            exerciseItems.forEach { (name, detail) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.XS),
                    shape = RoundedCornerShape(DesignTokens.Radius.LG),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(DesignTokens.Spacing.MD),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(DesignTokens.Radius.Base))
                                .background(DesignTokens.Colors.NeutralLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏋️", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.width(DesignTokens.Spacing.MD))
                        Column {
                            Text(name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XXL))

            // ── Delete Patient Section ────────────────────────────────────
            var showDeleteDialog by remember { mutableStateOf(false) }
            var isDeleting by remember { mutableStateOf(false) }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { if (!isDeleting) showDeleteDialog = false },
                    title = { Text("Delete Patient", color = DesignTokens.Colors.Error) },
                    text = { Text("Are you sure you want to delete this patient? This will remove all their data (plans, workouts, etc.) and cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                isDeleting = true
                                scope.launch {
                                    try {
                                        FirebaseService.deletePatient(patientId)
                                        showDeleteDialog = false
                                        onBack() // Navigate back on success
                                    } catch (e: Exception) {
                                        assignMessage = "❌ Failed to delete: ${e.message}"
                                        isDeleting = false
                                        showDeleteDialog = false
                                    }
                                }
                            },
                            enabled = !isDeleting
                        ) {
                            if (isDeleting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DesignTokens.Colors.Error, strokeWidth = 2.dp)
                            } else {
                                Text("Delete", color = DesignTokens.Colors.Error, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }, enabled = !isDeleting) {
                            Text("Cancel")
                        }
                    }
                )
            }

            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.XL)
                    .height(48.dp),
                shape = RoundedCornerShape(DesignTokens.Radius.Button),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DesignTokens.Colors.Error),
                border = androidx.compose.foundation.BorderStroke(1.dp, DesignTokens.Colors.Error)
            ) {
                Text("Delete Patient", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XXL))
        }
        }
    }
}


@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DesignTokens.Colors.Primary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
