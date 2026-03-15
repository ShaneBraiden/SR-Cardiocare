// ScheduleScreen.kt — Day picker + appointment list with add appointment
package com.srcardiocare.ui.screens.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.srcardiocare.data.firebase.FirebaseService
import com.srcardiocare.ui.theme.DesignTokens
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private data class DayItem(val dayName: String, val date: Int, val fullDate: LocalDate, val hasEvent: Boolean = false)
private data class ApptItem(
    val time: String,
    val name: String,
    val type: String,
    val color: androidx.compose.ui.graphics.Color,
    val apptDate: LocalDate? = null
)

// Generate current week dynamically
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(onBack: () -> Unit) {
    var selectedDay by remember { mutableIntStateOf(LocalDate.now().dayOfWeek.value - 1) }
    val days = remember { currentWeekDays() }
    var appointments by remember { mutableStateOf<List<ApptItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Add appointment state
    var showAddDialog by remember { mutableStateOf(false) }
    var apptType by remember { mutableStateOf("") }
    var apptHour by remember { mutableStateOf("10") }
    var apptMinute by remember { mutableStateOf("00") }
    var apptNotes by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var userRole by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load appointments
    LaunchedEffect(Unit) {
        try {
            val uid = FirebaseService.currentUID ?: return@LaunchedEffect
            val userData = FirebaseService.fetchUser(uid)
            val role = userData["role"] as? String ?: "patient"
            userRole = role
            val rawAppts = FirebaseService.fetchAppointments(uid, role)
            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
            appointments = rawAppts.map { (_, data) ->
                val dateTimeRaw = data["dateTime"]
                var displayTime = ""
                var apptDate: LocalDate? = null

                when (dateTimeRaw) {
                    is String -> {
                        try {
                            val instant = Instant.parse(dateTimeRaw)
                            val zdt = instant.atZone(ZoneId.systemDefault())
                            displayTime = zdt.format(timeFormatter)
                            apptDate = zdt.toLocalDate()
                        } catch (_: Exception) {
                            displayTime = dateTimeRaw
                        }
                    }
                    is com.google.firebase.Timestamp -> {
                        val instant = dateTimeRaw.toDate().toInstant()
                        val zdt = instant.atZone(ZoneId.systemDefault())
                        displayTime = zdt.format(timeFormatter)
                        apptDate = zdt.toLocalDate()
                    }
                }

                val type = data["type"] as? String ?: "Appointment"
                val status = data["status"] as? String ?: ""
                val notes = data["notes"] as? String ?: ""
                val color = when (status) {
                    "confirmed" -> DesignTokens.Colors.Success
                    "completed" -> DesignTokens.Colors.Success
                    "cancelled" -> DesignTokens.Colors.Error
                    else -> DesignTokens.Colors.Warning
                }
                ApptItem(
                    time = displayTime,
                    name = type.replaceFirstChar { it.uppercase() },
                    type = if (notes.isNotBlank()) notes else status.replaceFirstChar { it.uppercase() },
                    color = color,
                    apptDate = apptDate
                )
            }
        } catch (_: Exception) { }
        isLoading = false
    }

    // Filter appointments by selected day
    val selectedDate = days.getOrNull(selectedDay)?.fullDate
    val filteredAppts = if (selectedDate != null) {
        appointments.filter { it.apptDate == selectedDate }
    } else appointments

    // Add Appointment Dialog
    if (showAddDialog) {
        val appointmentTypes = listOf("Consultation", "Follow-up", "Physical Therapy", "Assessment", "Other")
        var selectedType by remember { mutableIntStateOf(0) }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showAddDialog = false },
            title = { Text("New Appointment", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Type", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        appointmentTypes.take(3).forEachIndexed { index, label ->
                            SegmentedButton(
                                selected = selectedType == index,
                                onClick = { selectedType = index; apptType = label },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = DesignTokens.Colors.Primary.copy(alpha = 0.15f),
                                    activeContentColor = DesignTokens.Colors.Primary
                                )
                            ) { Text(label, style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        appointmentTypes.drop(3).forEachIndexed { index, label ->
                            FilterChip(
                                selected = selectedType == index + 3,
                                onClick = { selectedType = index + 3; apptType = label },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DesignTokens.Colors.Primary.copy(alpha = 0.15f),
                                    selectedLabelColor = DesignTokens.Colors.Primary
                                )
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
                            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) apptHour = it },
                            label = { Text("HH") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(DesignTokens.Radius.Base),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
                        )
                        Text(":", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = apptMinute,
                            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) apptMinute = it },
                            label = { Text("MM") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(DesignTokens.Radius.Base),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DesignTokens.Colors.Primary)
                        )
                    }

                    OutlinedTextField(
                        value = apptNotes,
                        onValueChange = { apptNotes = it },
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
                        val type = if (apptType.isBlank()) appointmentTypes[selectedType] else apptType
                        val hour = apptHour.toIntOrNull() ?: 10
                        val minute = apptMinute.toIntOrNull() ?: 0
                        val apptDate = days.getOrNull(selectedDay)?.fullDate ?: LocalDate.now()
                        val dateTime = ZonedDateTime.of(apptDate, LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)), ZoneId.systemDefault())

                        isSaving = true
                        scope.launch {
                            try {
                                val uid = FirebaseService.currentUID ?: throw Exception("Not signed in")
                                val data = mutableMapOf<String, Any>(
                                    "type" to type,
                                    "dateTime" to dateTime.toInstant().toString(),
                                    "status" to "confirmed",
                                    "notes" to apptNotes
                                )
                                // Set both IDs based on role
                                if (userRole == "doctor" || userRole == "admin") {
                                    data["doctorId"] = uid
                                } else {
                                    data["patientId"] = uid
                                }
                                FirebaseService.createAppointment(data)

                                // Add to local list
                                val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
                                appointments = appointments + ApptItem(
                                    time = dateTime.format(timeFormatter),
                                    name = type,
                                    type = if (apptNotes.isNotBlank()) apptNotes else "Confirmed",
                                    color = DesignTokens.Colors.Success,
                                    apptDate = apptDate
                                )
                                showAddDialog = false
                                apptType = ""
                                apptNotes = ""
                                apptHour = "10"
                                apptMinute = "00"
                                snackbarHostState.showSnackbar("✅ Appointment created")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("❌ Failed: ${e.message}")
                            }
                            isSaving = false
                        }
                    },
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DesignTokens.Colors.Primary, strokeWidth = 2.dp)
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
                Icon(Icons.Default.Add, contentDescription = "New Appointment", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Day picker
            LazyRow(
                contentPadding = PaddingValues(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.MD),
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
                                color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
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

            // Selected date header
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

            // Appointments
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DesignTokens.Colors.Primary)
                }
            } else if (filteredAppts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📅", style = MaterialTheme.typography.displaySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No appointments for this day", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tap + to schedule one", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(DesignTokens.Spacing.MD),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Time column
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
                                // Color indicator
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(appt.color)
                                )
                                Spacer(modifier = Modifier.width(DesignTokens.Spacing.MD))
                                // Details
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(appt.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(appt.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                // Status dot
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(appt.color)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
