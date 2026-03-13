// ScheduleScreen.kt — Day picker + appointment list
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
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private data class DayItem(val dayName: String, val date: Int, val hasEvent: Boolean = false)
private data class ApptItem(val time: String, val name: String, val type: String, val color: androidx.compose.ui.graphics.Color)

// Generate current week dynamically
private fun currentWeekDays(): List<DayItem> {
    val today = LocalDate.now()
    val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
    return (0L..6L).map { offset ->
        val d = startOfWeek.plusDays(offset)
        DayItem(
            dayName = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            date = d.dayOfMonth
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

    LaunchedEffect(Unit) {
        try {
            val uid = FirebaseService.currentUID ?: return@LaunchedEffect
            val userData = FirebaseService.fetchUser(uid)
            val role = userData["role"] as? String ?: "patient"
            val rawAppts = FirebaseService.fetchAppointments(uid, role)
            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
            appointments = rawAppts.map { (_, data) ->
                val dateTimeStr = data["dateTime"] as? String ?: ""
                val displayTime = try {
                    val instant = Instant.parse(dateTimeStr)
                    instant.atZone(ZoneId.systemDefault()).format(timeFormatter)
                } catch (_: Exception) { dateTimeStr }
                val type = data["type"] as? String ?: ""
                val status = data["status"] as? String ?: ""
                val color = when (status) {
                    "confirmed" -> DesignTokens.Colors.Success
                    "completed" -> DesignTokens.Colors.Success
                    "cancelled" -> DesignTokens.Colors.Error
                    else -> DesignTokens.Colors.Warning
                }
                ApptItem(displayTime, type, status.replaceFirstChar { it.uppercase() }, color)
            }
        } catch (_: Exception) { }
        isLoading = false
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
                    TextButton(onClick = {}) {
                        Text("Today", color = DesignTokens.Colors.Primary, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Add appointment */ },
                containerColor = DesignTokens.Colors.Primary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Appointment", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
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
                            modifier = Modifier.padding(vertical = DesignTokens.Spacing.MD),
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
                            if (day.hasEvent) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (selected) MaterialTheme.colorScheme.onPrimary else DesignTokens.Colors.Primary)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = DesignTokens.Colors.NeutralLight)

            // Appointments
            LazyColumn(
                contentPadding = PaddingValues(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.MD),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.SM)
            ) {
                items(appointments) { appt ->
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
                            Text(
                                appt.time,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(72.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(appt.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text(appt.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
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
