// AssignmentListScreen.kt — Exercise assignments with active/history sections
package com.srcardiocare.ui.screens.patient

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.srcardiocare.data.firebase.FirebaseService
import com.srcardiocare.data.model.*
import com.srcardiocare.ui.theme.DesignTokens
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentListScreen(
    onExerciseTap: (assignment: Assignment, sessionNumber: Int) -> Unit,
    onBack: () -> Unit
) {
    var activeExercises by remember { mutableStateOf<List<ActiveExerciseItem>>(emptyList()) }
    var historyExercises by remember { mutableStateOf<List<HistoryExerciseItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showHistory by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    suspend fun loadData() {
        try {
            val patientId = FirebaseService.currentUID ?: return

            // Fetch all assignments for patient
            val rawAssignments = FirebaseService.fetchAssignments(patientId)

            // Fetch today's sessions
            val todaySessions = FirebaseService.fetchTodaysSessions(patientId)
            val sessionsByAssignment = todaySessions.groupBy { 
                it.second["assignmentId"] as? String ?: "" 
            }

            val activeList = mutableListOf<ActiveExerciseItem>()
            val historyList = mutableListOf<HistoryExerciseItem>()

            for ((id, data) in rawAssignments) {
                val assignment = parseAssignment(id, data)

                val startDate = LocalDate.parse(assignment.startDate)
                val endDate = LocalDate.parse(assignment.endDate)

                // Skip if not yet started
                if (today.isBefore(startDate)) continue

                // Check if expired (past end_date)
                if (today.isAfter(endDate)) {
                    // Move to history - build history item inline
                    val allSessions = FirebaseService.fetchAllSessionsForAssignment(assignment.id)
                    val completedSessions = allSessions.count { it.second["status"] == "COMPLETED" }
                    
                    val totalDays = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
                    val totalPossible = totalDays * assignment.dailyFrequency
                    
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
                            endDate = assignment.endDate
                        )
                    )
                } else {
                    // Active today
                    val assignmentSessions = sessionsByAssignment[assignment.id] ?: emptyList()
                    val completedSessions = assignmentSessions.count { 
                        it.second["status"] == "COMPLETED" 
                    }
                    val inProgressSession = assignmentSessions.find { 
                        it.second["status"] == "IN_PROGRESS" 
                    }

                    val isDone = completedSessions >= assignment.dailyFrequency
                    val daysUntilExpiry = ChronoUnit.DAYS.between(today, endDate).toInt()
                    val expiryText = when {
                        daysUntilExpiry == 0 -> "Expires today"
                        daysUntilExpiry == 1 -> "Expires tomorrow"
                        daysUntilExpiry <= 7 -> "Expires in $daysUntilExpiry days"
                        else -> "Expires ${endDate.format(dateFormatter)}"
                    }

                    activeList.add(
                        ActiveExerciseItem(
                            assignment = assignment,
                            sessionsToday = completedSessions,
                            dailyTarget = assignment.dailyFrequency,
                            isDoneToday = isDone,
                            canStartSession = !isDone && inProgressSession == null,
                            currentSession = inProgressSession?.let { parseSessionLog(it.first, it.second) },
                            expiryText = expiryText,
                            progressText = "$completedSessions/${assignment.dailyFrequency}"
                        )
                    )
                }
            }

            // Sort: incomplete first, then done (strikethrough at bottom)
            activeExercises = activeList.sortedBy { it.isDoneToday }
            // Sort history: most recent first
            historyExercises = historyList.sortedByDescending { it.endDate }
            errorMessage = null

        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to load assignments"
        }
        isLoading = false
        isRefreshing = false
    }



    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Exercises") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
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
                contentPadding = PaddingValues(bottom = DesignTokens.Spacing.XXXL)
            ) {
                // Loading state
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.XXL),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = DesignTokens.Colors.Primary)
                        }
                    }
                }

                // Error state
                errorMessage?.let { error ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(DesignTokens.Spacing.XL),
                            colors = CardDefaults.cardColors(
                                containerColor = DesignTokens.Colors.Error.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(DesignTokens.Spacing.MD),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = DesignTokens.Colors.Error
                                )
                                Spacer(modifier = Modifier.width(DesignTokens.Spacing.SM))
                                Text(error, color = DesignTokens.Colors.Error)
                            }
                        }
                    }
                }

                // ═══════════════════════════════════════════════════════════════
                // ACTIVE EXERCISES SECTION
                // ═══════════════════════════════════════════════════════════════
                if (!isLoading && activeExercises.isNotEmpty()) {
                    item {
                        Text(
                            "Today's Exercises",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                horizontal = DesignTokens.Spacing.XL,
                                vertical = DesignTokens.Spacing.MD
                            )
                        )
                    }

                    items(activeExercises, key = { it.assignment.id }) { item ->
                        ActiveExerciseCard(
                            item = item,
                            onClick = {
                                if (item.canStartSession) {
                                    onExerciseTap(item.assignment, item.sessionsToday + 1)
                                } else if (item.currentSession != null) {
                                    // Resume in-progress session
                                    onExerciseTap(item.assignment, item.sessionsToday + 1)
                                }
                            }
                        )
                    }
                }

                // Empty state for active
                if (!isLoading && activeExercises.isEmpty() && historyExercises.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(DesignTokens.Spacing.XXL),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = DesignTokens.Colors.NeutralGrey
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
                                Text(
                                    "No exercises assigned",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.Spacing.XS))
                                Text(
                                    "Your doctor will assign exercises for you",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // ═══════════════════════════════════════════════════════════════
                // HISTORY SECTION
                // ═══════════════════════════════════════════════════════════════
                if (!isLoading && historyExercises.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.LG))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = DesignTokens.Spacing.XL))
                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showHistory = !showHistory }
                                .padding(
                                    horizontal = DesignTokens.Spacing.XL,
                                    vertical = DesignTokens.Spacing.SM
                                ),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(DesignTokens.Spacing.SM))
                                Text(
                                    "History (${historyExercises.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                if (showHistory) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (showHistory) "Collapse" else "Expand"
                            )
                        }
                    }

                    if (showHistory) {
                        items(historyExercises, key = { it.assignment.id }) { item ->
                            HistoryExerciseCard(item = item)
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ACTIVE EXERCISE CARD
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ActiveExerciseCard(
    item: ActiveExerciseItem,
    onClick: () -> Unit
) {
    val isBlocked = !item.canStartSession && item.currentSession == null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.XS)
            .clickable(enabled = !isBlocked, onClick = onClick)
            .animateContentSize(),
        shape = RoundedCornerShape(DesignTokens.Radius.Card),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isDoneToday) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = if (!item.isDoneToday && item.canStartSession) {
            CardDefaults.cardElevation(defaultElevation = 4.dp)
        } else {
            CardDefaults.cardElevation()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            item.isDoneToday -> DesignTokens.Colors.Success.copy(alpha = 0.15f)
                            item.currentSession != null -> DesignTokens.Colors.Warning.copy(alpha = 0.15f)
                            else -> DesignTokens.Colors.Primary.copy(alpha = 0.15f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    item.isDoneToday -> Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Done",
                        tint = DesignTokens.Colors.Success,
                        modifier = Modifier.size(24.dp)
                    )
                    item.currentSession != null -> Icon(
                        Icons.Default.Pause,
                        contentDescription = "In Progress",
                        tint = DesignTokens.Colors.Warning,
                        modifier = Modifier.size(24.dp)
                    )
                    else -> Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Start",
                        tint = DesignTokens.Colors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(DesignTokens.Spacing.MD))

            // Exercise info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.assignment.exerciseName,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.isDoneToday) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (item.isDoneToday) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    "${item.assignment.sets} Sets • ${item.assignment.reps} Reps",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Expiry warning
                item.expiryText?.let { expiry ->
                    val expiryColor = when {
                        expiry.contains("today") -> DesignTokens.Colors.Error
                        expiry.contains("tomorrow") -> DesignTokens.Colors.Warning
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        expiry,
                        style = MaterialTheme.typography.labelSmall,
                        color = expiryColor
                    )
                }

                // In-progress indicator
                if (item.currentSession != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = DesignTokens.Colors.Warning,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Session in progress - tap to continue",
                            style = MaterialTheme.typography.labelSmall,
                            color = DesignTokens.Colors.Warning,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Progress badge
            Surface(
                shape = RoundedCornerShape(DesignTokens.Radius.Full),
                color = when {
                    item.isDoneToday -> DesignTokens.Colors.Success.copy(alpha = 0.15f)
                    item.sessionsToday > 0 -> DesignTokens.Colors.Primary.copy(alpha = 0.15f)
                    else -> DesignTokens.Colors.NeutralLight
                }
            ) {
                Text(
                    text = item.progressText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        item.isDoneToday -> DesignTokens.Colors.Success
                        item.sessionsToday > 0 -> DesignTokens.Colors.Primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HISTORY EXERCISE CARD
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HistoryExerciseCard(item: HistoryExerciseItem) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val endDate = LocalDate.parse(item.endDate)

    val (statusColor, statusIcon, statusText) = when (item.status) {
        AssignmentHistoryStatus.FULLY_COMPLETED -> Triple(
            DesignTokens.Colors.Success,
            Icons.Default.CheckCircle,
            "Completed"
        )
        AssignmentHistoryStatus.PARTIALLY_COMPLETED -> Triple(
            DesignTokens.Colors.Warning,
            Icons.Default.Warning,
            "Partial"
        )
        AssignmentHistoryStatus.MISSED -> Triple(
            DesignTokens.Colors.Error,
            Icons.Default.Cancel,
            "Missed"
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.XS),
        shape = RoundedCornerShape(DesignTokens.Radius.Card),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    statusIcon,
                    contentDescription = statusText,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(DesignTokens.Spacing.MD))

            // Exercise info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.assignment.exerciseName,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Ended ${endDate.format(dateFormatter)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Completion stats
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${item.sessionsCompleted}/${item.sessionsPossible}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Text(
                    "${(item.completionRate * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PARSERS
// ═══════════════════════════════════════════════════════════════════════════════

private fun parseAssignment(id: String, data: Map<String, Any?>): Assignment {
    return Assignment(
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
}

private fun parseSessionLog(id: String, data: Map<String, Any?>): SessionLog {
    val setLogsRaw = data["setLogs"] as? List<*> ?: emptyList<Any>()
    val setLogs = setLogsRaw.mapNotNull { raw ->
        val map = raw as? Map<*, *> ?: return@mapNotNull null
        SetLog(
            setNumber = (map["setNumber"] as? Number)?.toInt() ?: 0,
            startedAt = map["startedAt"] as? String,
            completedAt = map["completedAt"] as? String,
            videoWatchedSeconds = (map["videoWatchedSeconds"] as? Number)?.toInt() ?: 0,
            repsCompleted = (map["repsCompleted"] as? Number)?.toInt()
        )
    }

    return SessionLog(
        id = id,
        assignmentId = data["assignmentId"] as? String ?: "",
        patientId = data["patientId"] as? String ?: "",
        sessionDate = data["sessionDate"] as? String ?: LocalDate.now().toString(),
        sessionNumber = (data["sessionNumber"] as? Number)?.toInt() ?: 1,
        startedAt = (data["startedAt"] as? com.google.firebase.Timestamp)?.toDate()?.toString(),
        completedAt = (data["completedAt"] as? com.google.firebase.Timestamp)?.toDate()?.toString(),
        setsCompleted = (data["setsCompleted"] as? Number)?.toInt() ?: 0,
        totalSets = (data["totalSets"] as? Number)?.toInt() ?: 3,
        setLogs = setLogs,
        status = SessionStatus.valueOf(data["status"] as? String ?: "IN_PROGRESS"),
        feedbackId = data["feedbackId"] as? String
    )
}
