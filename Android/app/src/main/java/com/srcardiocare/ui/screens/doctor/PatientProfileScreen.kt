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
import com.srcardiocare.ui.components.InitialsAvatar
import com.srcardiocare.ui.theme.DesignTokens
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientProfileScreen(patientId: String, onBack: () -> Unit, onVideoUpload: () -> Unit) {
    var patientName by remember { mutableStateOf("") }
    var patientCondition by remember { mutableStateOf("") }
    var patientInitials by remember { mutableStateOf("") }
    var exerciseItems by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var feedbacks by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // New actions state
    var isEditingPlan by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("") }
    var isSendingFeedback by remember { mutableStateOf(false) }

    // Exercise assignment state
    var showAssignDialog by remember { mutableStateOf(false) }
    var availableExercises by remember { mutableStateOf<List<Pair<String, Map<String, Any?>>>>(emptyList()) }
    var isLoadingExercises by remember { mutableStateOf(false) }
    var assignMessage by remember { mutableStateOf<String?>(null) }

    // Prescription dialog state
    var showPrescriptionDialog by remember { mutableStateOf(false) }
    var selectedExercise by remember { mutableStateOf<Pair<String, Map<String, Any?>>?>(null) }
    var prescriptionDays by remember { mutableStateOf("7") }
    var prescriptionMode by remember { mutableStateOf("days") } // "days" or "date"
    var prescriptionEndDate by remember { mutableStateOf("") }

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
                    (ex as? Map<*, *>)?.mapKeys { it.key.toString() } as? Map<String, Any>
                }
            }

            val workouts = FirebaseService.fetchWorkouts(patientId)
            val totalWorkouts = workouts.size
            
            try {
                feedbacks = FirebaseService.fetchPatientFeedbacks(patientId).map { it.second }
            } catch (e: Exception) { }
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
                                        // Show prescription dialog instead of assigning directly
                                        selectedExercise = id to data
                                        prescriptionDays = "7"
                                        prescriptionEndDate = ""
                                        prescriptionMode = "days"
                                        showAssignDialog = false
                                        showPrescriptionDialog = true
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

    if (showFeedbackDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSendingFeedback) showFeedbackDialog = false },
            title = { Text("Send Feedback to Patient", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = feedbackMessage,
                    onValueChange = { feedbackMessage = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(DesignTokens.Radius.Base),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isSendingFeedback = true
                        scope.launch {
                            try {
                                FirebaseService.sendFeedback(patientId, feedbackMessage.trim())
                                assignMessage = "✅ Feedback sent"
                                showFeedbackDialog = false
                                feedbackMessage = ""
                            } catch (e: Exception) {
                                assignMessage = "❌ Failed to send feedback"
                            }
                            isSendingFeedback = false
                        }
                    },
                    enabled = !isSendingFeedback && feedbackMessage.isNotBlank()
                ) {
                    if (isSendingFeedback) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DesignTokens.Colors.Primary, strokeWidth = 2.dp)
                    } else {
                        Text("Send")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackDialog = false }, enabled = !isSendingFeedback) {
                    Text("Cancel")
                }
            }
        )
    }

    // Prescription Dialog
    if (showPrescriptionDialog && selectedExercise != null) {
        val (exId, exData) = selectedExercise!!
        val exName = exData["name"] as? String ?: exData["title"] as? String ?: "Exercise"
        val videoUrl = exData["videoUrl"] as? String
        val instructions = exData["instructions"]
        val defaultSets = (exData["sets"] as? Number)?.toInt() ?: 3
        val defaultReps = (exData["reps"] as? Number)?.toInt() ?: 10
        val category = exData["category"] as? String ?: ""
        val difficulty = exData["difficulty"] as? String ?: ""

        var isAssigning by remember { mutableStateOf(false) }

        // Calculate end date based on mode
        val calculatedEndDate = remember(prescriptionMode, prescriptionDays, prescriptionEndDate) {
            when (prescriptionMode) {
                "days" -> {
                    val days = prescriptionDays.toIntOrNull() ?: 7
                    java.time.LocalDate.now().plusDays(days.toLong()).toString()
                }
                else -> prescriptionEndDate.ifBlank { java.time.LocalDate.now().plusDays(7).toString() }
            }
        }

        AlertDialog(
            onDismissRequest = { if (!isAssigning) showPrescriptionDialog = false },
            title = { Text("Prescribe Exercise", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Selected exercise info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(DesignTokens.Radius.Base),
                        colors = CardDefaults.cardColors(containerColor = DesignTokens.Colors.PrimaryLight.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(DesignTokens.Spacing.MD),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🏋️", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.width(DesignTokens.Spacing.SM))
                            Column {
                                Text(exName, fontWeight = FontWeight.SemiBold)
                                if (category.isNotBlank()) {
                                    Text(category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.LG))

                    Text("Prescription Duration", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))

                    // Toggle between days and specific date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.SM)
                    ) {
                        FilterChip(
                            selected = prescriptionMode == "days",
                            onClick = { prescriptionMode = "days" },
                            label = { Text("Number of Days") }
                        )
                        FilterChip(
                            selected = prescriptionMode == "date",
                            onClick = { prescriptionMode = "date" },
                            label = { Text("End Date") }
                        )
                    }

                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

                    if (prescriptionMode == "days") {
                        OutlinedTextField(
                            value = prescriptionDays,
                            onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) prescriptionDays = it },
                            label = { Text("Number of Days") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(DesignTokens.Radius.Base),
                            singleLine = true,
                            trailingIcon = { Text("days", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        )
                    } else {
                        OutlinedTextField(
                            value = prescriptionEndDate,
                            onValueChange = { prescriptionEndDate = it },
                            label = { Text("End Date (YYYY-MM-DD)") },
                            placeholder = { Text("e.g., 2026-04-15") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(DesignTokens.Radius.Base),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

                    // Show calculated end date
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(DesignTokens.Radius.Base),
                        colors = CardDefaults.cardColors(containerColor = DesignTokens.Colors.Success.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(DesignTokens.Spacing.MD),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📅", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.width(DesignTokens.Spacing.SM))
                            Column {
                                Text("Plan ends on", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(calculatedEndDate, fontWeight = FontWeight.Bold, color = DesignTokens.Colors.Success)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isAssigning = true
                        scope.launch {
                            try {
                                val expiryDays = if (prescriptionMode == "days") {
                                    prescriptionDays.toIntOrNull() ?: 7
                                } else {
                                    // Calculate days from end date
                                    try {
                                        val endDate = java.time.LocalDate.parse(prescriptionEndDate)
                                        java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), endDate).toInt().coerceAtLeast(1)
                                    } catch (_: Exception) { 7 }
                                }
                                
                                // Individual exercise expiry date
                                val exerciseExpiryDate = calculatedEndDate

                                val exerciseData = mapOf<String, Any>(
                                    "exerciseId" to exId,
                                    "name" to exName,
                                    "category" to category,
                                    "difficulty" to difficulty,
                                    "customSets" to defaultSets,
                                    "customReps" to defaultReps,
                                    "videoUrl" to (videoUrl ?: ""),
                                    "instructions" to when (instructions) {
                                        is String -> instructions
                                        is List<*> -> instructions.joinToString("\n") { it?.toString().orEmpty() }
                                        else -> ""
                                    },
                                    "assignedDate" to java.time.LocalDate.now().toString(),
                                    "expiryDate" to exerciseExpiryDate // Individual workout expiry
                                )

                                FirebaseService.assignExerciseToPatientWithPrescription(
                                    patientId = patientId,
                                    exerciseData = exerciseData,
                                    expiryDays = expiryDays,
                                    expiryDate = calculatedEndDate
                                )

                                showPrescriptionDialog = false
                                selectedExercise = null
                                assignMessage = "✅ \"$exName\" prescribed to $patientName until $calculatedEndDate"
                                loadPatientData()
                            } catch (e: Exception) {
                                assignMessage = "❌ Failed: ${e.message}"
                            }
                            isAssigning = false
                        }
                    },
                    enabled = !isAssigning && (prescriptionMode == "days" && prescriptionDays.isNotBlank() || prescriptionMode == "date" && prescriptionEndDate.isNotBlank())
                ) {
                    if (isAssigning) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DesignTokens.Colors.Primary, strokeWidth = 2.dp)
                    } else {
                        Text("Prescribe", color = DesignTokens.Colors.Primary)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrescriptionDialog = false; selectedExercise = null }, enabled = !isAssigning) {
                    Text("Cancel")
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
            InitialsAvatar(
                initials = patientInitials,
                size = 80.dp
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
            Text(patientName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(patientCondition, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

            // Metrics / Feedbacks Chart
            if (feedbacks.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.XL),
                    shape = RoundedCornerShape(DesignTokens.Radius.LG),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(DesignTokens.Spacing.XL)) {
                        Text("Recent Health Metrics", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
                        
                        // Recent up to 7 feedbacks
                        val recent = feedbacks.take(7).reversed()
                        Row(
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            recent.forEach { f ->
                                val resp = (f["respiratoryDifficulty"] as? Number)?.toFloat() ?: 1f
                                val stress = f["stress"] as? Boolean ?: false
                                val strain = f["strain"] as? Boolean ?: false
                                
                                val barColor = if (stress || strain) DesignTokens.Colors.Warning else DesignTokens.Colors.Success
                                val barHeight = (resp / 10f * 60f).coerceAtLeast(4f)
                                
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .width(16.dp)
                                            .height(barHeight.dp)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(barColor)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))
                        Text(
                            "Respiratory Difficulty (1-10) with Stress/Strain (Yellow)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.XL),
                    shape = RoundedCornerShape(DesignTokens.Radius.LG),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.LG),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No feedback data available yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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
                        onClick = { showFeedbackDialog = true },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(DesignTokens.Radius.Base),
                        colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.Primary)
                    ) {
                        Text("Send Feedback", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(
                        onClick = { isEditingPlan = !isEditingPlan },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(DesignTokens.Radius.Base),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DesignTokens.Colors.Primary)
                    ) {
                        Text(if (isEditingPlan) "Done Editing" else "Edit Plan", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
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
            exerciseItems.forEach { ex ->
                val name = ex["name"] as? String ?: "Exercise"
                val sets = (ex["customSets"] as? Number)?.toInt() ?: 0
                val reps = (ex["customReps"] as? Number)?.toInt() ?: 0
                val detail = "$sets Sets • $reps Reps"

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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isEditingPlan) {
                            IconButton(onClick = {
                                scope.launch {
                                    try {
                                        FirebaseService.removeExerciseFromPlan(patientId, ex)
                                        assignMessage = "✅ Removed $name"
                                        loadPatientData() // reload list
                                    } catch (e: Exception) {
                                        assignMessage = "❌ Failed to remove exercise"
                                    }
                                }
                            }) {
                                Text("🗑️")
                            }
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
