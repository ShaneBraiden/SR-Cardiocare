package com.srcardiocare.ui.screens.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.srcardiocare.core.security.ErrorHandler
import com.srcardiocare.core.security.InputValidator
import com.srcardiocare.data.firebase.FirebaseService
import com.srcardiocare.ui.theme.DesignTokens
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private data class DayItem(
    val dayName: String,
    val date: Int,
    val fullDate: LocalDate
)

private data class SelectablePatient(
    val id: String,
    val name: String
)

private data class ApptItem(
    val id: String,
    val time: String,
    val title: String,
    val type: String,
    val notes: String,
    val status: String,
    val color: Color,
    val apptDate: LocalDate?,
    val apptEpochMs: Long?,
    val patientId: String?,
    val doctorId: String?,
    val requestedByRole: String
)

private fun currentWeekDays(): List<DayItem> {
    val today = LocalDate.now()
    val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
    return (0L..6L).map { offset ->
        val d = startOfWeek.plusDays(offset)
        DayItem(
            dayName = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            date = d.dayOfMonth,
            fullDate = d
        )
    }
}

private fun statusColor(status: String): Color {
    return when (status.lowercase()) {
        "confirmed", "completed" -> DesignTokens.Colors.Success
        "cancelled" -> DesignTokens.Colors.Error
        "pending", "scheduled" -> DesignTokens.Colors.Warning
        else -> DesignTokens.Colors.Slate500
    }
}

private fun prettyStatus(status: String): String {
    if (status.isBlank()) return "Unknown"
    return status.replaceFirstChar { it.uppercase() }
}

private fun parseDateTime(raw: Any?): Triple<String, LocalDate?, Long?> {
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    return when (raw) {
        is String -> {
            try {
                val instant = Instant.parse(raw)
                val zdt = instant.atZone(ZoneId.systemDefault())
                Triple(zdt.format(timeFormatter), zdt.toLocalDate(), zdt.toInstant().toEpochMilli())
            } catch (_: Exception) {
                Triple(raw, null, null)
            }
        }

        is Timestamp -> {
            val instant = raw.toDate().toInstant()
            val zdt = instant.atZone(ZoneId.systemDefault())
            Triple(zdt.format(timeFormatter), zdt.toLocalDate(), zdt.toInstant().toEpochMilli())
        }

        else -> Triple("", null, null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(onBack: () -> Unit) {
    var selectedDay by remember { mutableIntStateOf(LocalDate.now().dayOfWeek.value - 1) }
    val days = remember { currentWeekDays() }

    var appointments by remember { mutableStateOf<List<ApptItem>>(emptyList()) }
    var patients by remember { mutableStateOf<List<SelectablePatient>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var userRole by remember { mutableStateOf("") }
    var currentUid by remember { mutableStateOf<String?>(null) }
    var assignedDoctorId by remember { mutableStateOf<String?>(null) }

    var showAddDialog by remember { mutableStateOf(false) }
    var apptHour by remember { mutableStateOf("10") }
    var apptMinute by remember { mutableStateOf("00") }
    var apptNotes by remember { mutableStateOf("") }
    var selectedPatientId by remember { mutableStateOf<String?>(null) }
    var patientMenuExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    var reloadKey by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    suspend fun resolveUserName(uid: String?, cache: MutableMap<String, String>): String {
        if (uid.isNullOrBlank()) return "Unknown"
        cache[uid]?.let { return it }

        return try {
            val user = FirebaseService.fetchUser(uid)
            val first = user["firstName"] as? String ?: ""
            val last = user["lastName"] as? String ?: ""
            val role = (user["role"] as? String ?: "").lowercase()
            val base = "$first $last".trim().ifBlank { "Unknown" }
            val result = if (role == "doctor") "Dr. $base" else base
            cache[uid] = result
            result
        } catch (_: Exception) {
            "Unknown"
        }
    }

    suspend fun loadSchedule() {
        try {
            val uid = FirebaseService.currentUID
            if (uid == null) {
                loadError = "Not signed in"
                appointments = emptyList()
                patients = emptyList()
                isLoading = false
                return
            }
            currentUid = uid

            val userData = FirebaseService.fetchUser(uid)
            val role = (userData["role"] as? String ?: "patient").lowercase()
            userRole = role
            assignedDoctorId = userData["assignedDoctorId"] as? String

            patients = if (role == "doctor") {
                FirebaseService.fetchPatients(uid).map { (patientId, data) ->
                    val first = data["firstName"] as? String ?: ""
                    val last = data["lastName"] as? String ?: ""
                    SelectablePatient(patientId, "$first $last".trim().ifBlank { "Unknown" })
                }.sortedBy { it.name }
            } else if (role == "admin") {
                FirebaseService.fetchAllPatients().map { (patientId, data) ->
                    val first = data["firstName"] as? String ?: ""
                    val last = data["lastName"] as? String ?: ""
                    SelectablePatient(patientId, "$first $last".trim().ifBlank { "Unknown" })
                }.sortedBy { it.name }
            } else {
                emptyList()
            }

            val rawAppts = try {
                FirebaseService.fetchAppointments(uid, role)
            } catch (_: Exception) {
                FirebaseService.fetchAppointmentsUnordered(uid, role)
            }

            val nameCache = mutableMapOf<String, String>()
            patients.forEach { nameCache[it.id] = it.name }

            appointments = rawAppts.map { (id, data) ->
                val dateTime = parseDateTime(data["dateTime"])
                val status = (data["status"] as? String ?: "scheduled").lowercase()
                val type = data["type"] as? String ?: "Appointment"
                val notes = data["notes"] as? String ?: ""
                val patientId = data["patientId"] as? String
                val doctorId = data["doctorId"] as? String
                val requestedByRole = (data["requestedByRole"] as? String ?: "doctor").lowercase()

                val title = if (role == "doctor" || role == "admin") {
                    resolveUserName(patientId, nameCache)
                } else {
                    resolveUserName(doctorId, nameCache)
                }

                ApptItem(
                    id = id,
                    time = dateTime.first,
                    title = title,
                    type = type,
                    notes = notes,
                    status = status,
                    color = statusColor(status),
                    apptDate = dateTime.second,
                    apptEpochMs = dateTime.third,
                    patientId = patientId,
                    doctorId = doctorId,
                    requestedByRole = requestedByRole
                )
            }.sortedBy { it.apptEpochMs ?: Long.MAX_VALUE }

            loadError = null
        } catch (e: Exception) {
            loadError = ErrorHandler.getDisplayMessage(e, "load schedule")
        }

        isLoading = false
    }

    LaunchedEffect(reloadKey) {
        isLoading = true
        loadSchedule()
    }

    val selectedDate = days.getOrNull(selectedDay)?.fullDate
    val filteredAppts = if (selectedDate != null) {
        appointments.filter { it.apptDate == selectedDate }
    } else {
        appointments
    }

    suspend fun updateStatusWithNotification(
        appt: ApptItem,
        newStatus: String,
        notifyUserId: String?,
        title: String,
        body: String,
        action: String
    ) {
        if (notifyUserId.isNullOrBlank()) {
            throw Exception("Missing target user")
        }

        FirebaseService.updateAppointment(
            appt.id,
            mapOf(
                "status" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        FirebaseService.createNotification(
            userId = notifyUserId,
            title = title,
            body = body,
            type = "appointment_update",
            appointmentId = appt.id,
            action = action
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showAddDialog = false },
            title = { Text("New Appointment", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (userRole == "doctor" || userRole == "admin") {
                        Text("Patient", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)

                        Box {
                            OutlinedTextField(
                                value = patients.firstOrNull { it.id == selectedPatientId }?.name ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select patient") },
                                trailingIcon = { Text(if (patientMenuExpanded) "▲" else "▼") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isSaving && patients.isNotEmpty()) {
                                        patientMenuExpanded = !patientMenuExpanded
                                    },
                                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
                            )

                            DropdownMenu(
                                expanded = patientMenuExpanded,
                                onDismissRequest = { patientMenuExpanded = false }
                            ) {
                                patients.forEach { patient ->
                                    DropdownMenuItem(
                                        text = { Text(patient.name) },
                                        onClick = {
                                            selectedPatientId = patient.id
                                            patientMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (patients.isEmpty()) {
                            Text(
                                "No patients available. Add a patient first.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DesignTokens.Colors.Warning
                            )
                        }
                    }

                    Text("Time", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = apptHour,
                            onValueChange = { newVal ->
                                if (newVal.length <= 2 && newVal.all { it.isDigit() }) {
                                    val hourInt = newVal.toIntOrNull()
                                    if (hourInt == null || hourInt <= 23) {
                                        apptHour = newVal
                                    }
                                }
                            },
                            label = { Text("HH") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(DesignTokens.Radius.Base),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
                        )

                        Text(":", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)

                        OutlinedTextField(
                            value = apptMinute,
                            onValueChange = { newVal ->
                                if (newVal.length <= 2 && newVal.all { it.isDigit() }) {
                                    val minInt = newVal.toIntOrNull()
                                    if (minInt == null || minInt <= 59) {
                                        apptMinute = newVal
                                    }
                                }
                            },
                            label = { Text("MM") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(DesignTokens.Radius.Base),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
                        )
                    }

                    OutlinedTextField(
                        value = apptNotes,
                        onValueChange = { apptNotes = InputValidator.limitLength(it, InputValidator.MaxLength.NOTES) },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(DesignTokens.Radius.Base),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val type = "Consultation"
                        val hour = apptHour.toIntOrNull() ?: 10
                        val minute = apptMinute.toIntOrNull() ?: 0
                        val apptDate = days.getOrNull(selectedDay)?.fullDate ?: LocalDate.now()
                        val dateTime = ZonedDateTime.of(
                            apptDate,
                            LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)),
                            ZoneId.systemDefault()
                        )

                        isSaving = true
                        scope.launch {
                            try {
                                val uid = currentUid ?: FirebaseService.currentUID ?: throw Exception("Not signed in")
                                val role = userRole

                                val appointmentData = mutableMapOf<String, Any>(
                                    "type" to type,
                                    "dateTime" to dateTime.toInstant().toString(),
                                    "notes" to apptNotes,
                                    "durationMinutes" to 30
                                )

                                if (role == "doctor" || role == "admin") {
                                    val patientId = selectedPatientId
                                    if (patientId.isNullOrBlank()) {
                                        throw Exception("Please select a patient")
                                    }

                                    appointmentData["status"] = "confirmed"
                                    appointmentData["doctorId"] = uid
                                    appointmentData["patientId"] = patientId
                                    appointmentData["requestedByRole"] = if (role == "admin") "admin" else "doctor"

                                    val appointmentId = FirebaseService.createAppointment(appointmentData)
                                    FirebaseService.createNotification(
                                        userId = patientId,
                                        title = "New appointment scheduled",
                                        body = "$type at ${dateTime.format(DateTimeFormatter.ofPattern("h:mm a, MMM d"))}",
                                        type = "appointment",
                                        appointmentId = appointmentId,
                                        action = "created"
                                    )

                                    snackbarHostState.showSnackbar("Appointment created")
                                } else {
                                    val doctorId = assignedDoctorId
                                    if (doctorId.isNullOrBlank()) {
                                        throw Exception("No assigned doctor found")
                                    }

                                    appointmentData["status"] = "pending"
                                    appointmentData["doctorId"] = doctorId
                                    appointmentData["patientId"] = uid
                                    appointmentData["requestedByRole"] = "patient"

                                    val appointmentId = FirebaseService.createAppointment(appointmentData)
                                    FirebaseService.createNotification(
                                        userId = doctorId,
                                        title = "New appointment request",
                                        body = "Patient requested $type at ${dateTime.format(DateTimeFormatter.ofPattern("h:mm a, MMM d"))}",
                                        type = "appointment_request",
                                        appointmentId = appointmentId,
                                        action = "requested"
                                    )

                                    snackbarHostState.showSnackbar("Request sent. Waiting for doctor approval")
                                }

                                showAddDialog = false
                                apptNotes = ""
                                apptHour = "10"
                                apptMinute = "00"
                                selectedPatientId = null
                                reloadKey++
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(ErrorHandler.getDisplayMessage(e, "create appointment"))
                            }
                            isSaving = false
                        }
                    },
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = DesignTokens.Colors.Primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Create", color = DesignTokens.Colors.Primary, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }, enabled = !isSaving) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { selectedDay = LocalDate.now().dayOfWeek.value - 1 }) {
                        Text("Today", color = DesignTokens.Colors.Primary, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = DesignTokens.Colors.Primary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "New appointment", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyRow(
                contentPadding = PaddingValues(
                    horizontal = DesignTokens.Spacing.XL,
                    vertical = DesignTokens.Spacing.MD
                ),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.SM)
            ) {
                items(days.size) { index ->
                    val day = days[index]
                    val selected = index == selectedDay

                    Card(
                        modifier = Modifier
                            .width(56.dp)
                            .clickable { selectedDay = index },
                        shape = RoundedCornerShape(DesignTokens.Radius.LG),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) DesignTokens.Colors.Primary else MaterialTheme.colorScheme.surface
                        ),
                        elevation = if (selected) CardDefaults.cardElevation(defaultElevation = 4.dp) else CardDefaults.cardElevation()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = DesignTokens.Spacing.MD),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                day.dayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${day.date}",
                                fontWeight = FontWeight.Bold,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            selectedDate?.let {
                Text(
                    it.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.SM)
                )
            }

            HorizontalDivider(color = DesignTokens.Colors.NeutralLight)
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DesignTokens.Colors.Primary)
                    }
                }

                loadError != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Could not load schedule", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                loadError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = {
                                isLoading = true
                                scope.launch { loadSchedule() }
                            }) {
                                Text("Retry")
                            }
                        }
                    }
                }

                filteredAppts.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No appointments for this day", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Tap + to create one",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.MD),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.SM),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredAppts) { appt ->
                            Card(
                                shape = RoundedCornerShape(DesignTokens.Radius.LG),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(DesignTokens.Spacing.MD)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.width(72.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                appt.time,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = DesignTokens.Colors.Primary
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(40.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(appt.color)
                                        )

                                        Spacer(modifier = Modifier.width(DesignTokens.Spacing.MD))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(appt.title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                            Text(appt.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            if (appt.notes.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(appt.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(DesignTokens.Radius.Full))
                                                .background(appt.color.copy(alpha = 0.12f))
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                prettyStatus(appt.status),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = appt.color
                                            )
                                        }
                                    }

                                    if (appt.status == "pending" && (userRole == "doctor" || userRole == "admin")) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextButton(
                                                onClick = {
                                                    scope.launch {
                                                        try {
                                                            updateStatusWithNotification(
                                                                appt = appt,
                                                                newStatus = "confirmed",
                                                                notifyUserId = appt.patientId,
                                                                title = "Appointment request accepted",
                                                                body = "Your ${appt.type} request has been accepted.",
                                                                action = "accepted"
                                                            )
                                                            snackbarHostState.showSnackbar("Appointment accepted")
                                                            reloadKey++
                                                        } catch (e: Exception) {
                                                            snackbarHostState.showSnackbar(
                                                                ErrorHandler.getDisplayMessage(e, "accept appointment")
                                                            )
                                                        }
                                                    }
                                                }
                                            ) {
                                                Text("Accept", color = DesignTokens.Colors.Success)
                                            }

                                            TextButton(
                                                onClick = {
                                                    scope.launch {
                                                        try {
                                                            updateStatusWithNotification(
                                                                appt = appt,
                                                                newStatus = "cancelled",
                                                                notifyUserId = appt.patientId,
                                                                title = "Appointment request declined",
                                                                body = "Your ${appt.type} request was declined.",
                                                                action = "declined"
                                                            )
                                                            snackbarHostState.showSnackbar("Appointment declined")
                                                            reloadKey++
                                                        } catch (e: Exception) {
                                                            snackbarHostState.showSnackbar(
                                                                ErrorHandler.getDisplayMessage(e, "decline appointment")
                                                            )
                                                        }
                                                    }
                                                }
                                            ) {
                                                Text("Decline", color = DesignTokens.Colors.Error)
                                            }
                                        }
                                    }

                                    if (appt.status == "pending" && userRole == "patient" && appt.requestedByRole == "patient") {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        TextButton(
                                            onClick = {
                                                scope.launch {
                                                    try {
                                                        updateStatusWithNotification(
                                                            appt = appt,
                                                            newStatus = "cancelled",
                                                            notifyUserId = appt.doctorId,
                                                            title = "Appointment request cancelled",
                                                            body = "Patient cancelled the ${appt.type} request.",
                                                            action = "cancelled"
                                                        )
                                                        snackbarHostState.showSnackbar("Request cancelled")
                                                        reloadKey++
                                                    } catch (e: Exception) {
                                                        snackbarHostState.showSnackbar(
                                                            ErrorHandler.getDisplayMessage(e, "cancel request")
                                                        )
                                                    }
                                                }
                                            }
                                        ) {
                                            Text("Cancel Request", color = DesignTokens.Colors.Error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
