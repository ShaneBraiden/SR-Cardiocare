package com.srcardiocare.ui.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.srcardiocare.data.firebase.FirebaseService
import com.srcardiocare.data.model.*
import com.srcardiocare.ui.theme.DesignTokens
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientHistoryScreen(
    patientId: String,
    onBack: () -> Unit
) {
    var historyExercises by remember { mutableStateOf<List<HistoryExerciseItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    suspend fun loadData() {
        if (patientId.isBlank()) return
        try {
            val rawAssignments = FirebaseService.fetchAssignments(patientId)
            val historyList = mutableListOf<HistoryExerciseItem>()

            for ((id, data) in rawAssignments) {
                // Same parse logic manually here
                val assignment = Assignment(
                    id = id,
                    patientId = data["patientId"] as? String ?: "",
                    doctorId = data["doctorId"] as? String ?: "",
                    exerciseId = data["exerciseId"] as? String ?: "",
                    exerciseName = data["exerciseName"] as? String ?: "Exercise",
                    exerciseVideoUrl = data["exerciseVideoUrl"] as? String,
                    exerciseThumbnailUrl = data["exerciseThumbnailUrl"] as? String,
                    exerciseCategory = data["exerciseCategory"] as? String,
                    exerciseDifficulty = data["exerciseDifficulty"] as? String,
                    startDate = data["startDate"] as? String ?: LocalDate.now().toString(),
                    endDate = data["endDate"] as? String ?: LocalDate.now().plusDays(7).toString(),
                    dailyFrequency = ((data["dailyFrequency"] as? Number)?.toInt() ?: 3).coerceIn(1, 3),
                    sets = (data["sets"] as? Number)?.toInt() ?: 3,
                    reps = (data["reps"] as? Number)?.toInt() ?: 10,
                    instructions = data["instructions"] as? String,
                    completionThreshold = (data["completionThreshold"] as? Number)?.toFloat() ?: 0.8f,
                    isActive = data["isActive"] as? Boolean ?: true,
                    createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.toString()
                )

                val startDate = LocalDate.parse(assignment.startDate)
                val endDate = LocalDate.parse(assignment.endDate)

                if (today.isBefore(startDate)) continue

                val allSessions = FirebaseService.fetchAllSessionsForAssignment(assignment.id)
                val groupedByDate = allSessions.groupBy { it.second["sessionDate"] as? String ?: "" }

                val limitDate = if (today.isAfter(endDate)) endDate else today.minusDays(1)
                var iterDate = startDate
                while (!iterDate.isAfter(limitDate)) {
                    val dateStr = iterDate.toString()
                    val daysSessions = groupedByDate[dateStr] ?: emptyList()
                    val completedSessions = daysSessions.count { it.second["status"] == "COMPLETED" }
                    val totalPossible = assignment.dailyFrequency
                    
                    val completionRate = if (totalPossible > 0) {
                        completedSessions.toFloat() / totalPossible
                    } else 0f
                    
                    val historyStatus = when {
                        completionRate >= assignment.completionThreshold -> AssignmentHistoryStatus.FULLY_COMPLETED
                        completedSessions > 0 -> AssignmentHistoryStatus.PARTIALLY_COMPLETED
                        else -> AssignmentHistoryStatus.MISSED
                    }
                    
                    historyList.add(
                        HistoryExerciseItem(
                            assignment = assignment,
                            status = historyStatus,
                            completionRate = completionRate,
                            sessionsCompleted = completedSessions,
                            sessionsPossible = totalPossible,
                            endDate = dateStr
                        )
                    )
                    iterDate = iterDate.plusDays(1)
                }
            }
            historyExercises = historyList.sortedByDescending { it.endDate }
        } catch (_: Exception) { }
        isLoading = false
        isRefreshing = false
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { loadData() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text("Workout History", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch { loadData() }
            },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = DesignTokens.Spacing.MD)
            ) {
                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.XL), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (historyExercises.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.XL),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No history available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(historyExercises, key = { "${it.assignment.id}_${it.endDate}" }) { item ->
                        HistoryCard(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(item: HistoryExerciseItem) {
    val dateLabel = try {
        LocalDate.parse(item.endDate).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    } catch (_: Exception) { item.endDate }

    val statusColor = when (item.status) {
        AssignmentHistoryStatus.FULLY_COMPLETED -> DesignTokens.Colors.Success
        AssignmentHistoryStatus.PARTIALLY_COMPLETED -> DesignTokens.Colors.Warning
        AssignmentHistoryStatus.MISSED -> DesignTokens.Colors.Error
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.XS),
        shape = RoundedCornerShape(DesignTokens.Radius.LG),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when (item.status) {
                        AssignmentHistoryStatus.FULLY_COMPLETED -> Icons.Default.Check
                        AssignmentHistoryStatus.PARTIALLY_COMPLETED -> Icons.Default.WatchLater
                        AssignmentHistoryStatus.MISSED -> Icons.Default.Close
                    },
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(DesignTokens.Spacing.MD))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.assignment.exerciseName,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(DesignTokens.Spacing.SM))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    when (item.status) {
                        AssignmentHistoryStatus.FULLY_COMPLETED -> "Completed"
                        AssignmentHistoryStatus.PARTIALLY_COMPLETED -> "${item.sessionsCompleted}/${item.sessionsPossible} Done"
                        AssignmentHistoryStatus.MISSED -> "Missed"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )
                Text(
                    "${(item.completionRate * 100).toInt()}% Target",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
