// PatientHomeScreen.kt — Patient dashboard with progress ring, exercises, expiry & daily limit
package com.srcardiocare.ui.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.srcardiocare.data.firebase.FirebaseService
import com.srcardiocare.ui.theme.DesignTokens
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private enum class ExStatus { COMPLETED, ACTIVE, PENDING, EXPIRED, DAILY_LIMIT }
private data class ExItem(
    val name: String,
    val detail: String,
    val status: ExStatus,
    val sets: Int,
    val reps: Int,
    val videoUrl: String?,
    val instructions: String?,
    val statusLabel: String? = null
)

private const val MAX_WORKOUTS_PER_DAY = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientHomeScreen(
    onExerciseTap: (name: String, videoUrl: String?, sets: Int, reps: Int, instructions: String?, planId: String, totalCount: Int, isLastExercise: Boolean) -> Unit,
    onScheduleTap: () -> Unit,
    onAnalyticsTap: () -> Unit,
    onNotificationsTap: () -> Unit,
    onChatTap: () -> Unit = {},
    onProfile: () -> Unit = {}
) {
    var exercises by remember { mutableStateOf<List<ExItem>>(emptyList()) }
    var userName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var completedCount by remember { mutableIntStateOf(0) }
    var totalCount by remember { mutableIntStateOf(0) }
    var fullyCompletedToday by remember { mutableIntStateOf(0) }
    var dailyLimitReached by remember { mutableStateOf(false) }
    var activePlanId by remember { mutableStateOf<String?>(null) }
    var expiryText by remember { mutableStateOf<String?>(null) }
    var sessionLocksEnabled by remember { mutableStateOf(true) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val uid = FirebaseService.currentUID ?: return@LaunchedEffect
            val userData = FirebaseService.fetchUser(uid)
            userName = userData["firstName"] as? String ?: ""

            // Update last seen for online tracking
            FirebaseService.updateLastSeen()

            // Fetch global session locks setting
            sessionLocksEnabled = FirebaseService.fetchSessionLocksEnabled()

            // Check daily workout limit
            val todayCompletions = try {
                FirebaseService.fetchWorkoutCompletionsToday(uid)
            } catch (_: Exception) { 0 }
            dailyLimitReached = todayCompletions >= MAX_WORKOUTS_PER_DAY

            val plans = FirebaseService.fetchPlans(uid)
            val activePlan = plans.firstOrNull { (it.second["isActive"] as? Boolean) == true }
                ?: plans.firstOrNull()
            if (activePlan != null) {
                activePlanId = activePlan.first
                val planData = activePlan.second

                // Check plan expiry
                val expiryDateRaw = planData["expiryDate"]
                val expiryDays = (planData["expiryDays"] as? Number)?.toInt()
                val planCreatedAt = planData["createdAt"]
                val today = LocalDate.now()

                var computedExpiryDate: LocalDate? = null

                val isExpired = when {
                    expiryDateRaw is String -> {
                        try {
                            computedExpiryDate = LocalDate.parse(expiryDateRaw)
                            today.isAfter(computedExpiryDate)
                        } catch (_: Exception) { false }
                    }
                    expiryDateRaw is com.google.firebase.Timestamp -> {
                        computedExpiryDate = expiryDateRaw.toDate().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                        today.isAfter(computedExpiryDate)
                    }
                    expiryDays != null && planCreatedAt is com.google.firebase.Timestamp -> {
                        val createdDate = planCreatedAt.toDate().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                        computedExpiryDate = createdDate.plusDays(expiryDays.toLong())
                        today.isAfter(computedExpiryDate)
                    }
                    else -> false
                }

                expiryText = if (computedExpiryDate != null) {
                    val daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(today, computedExpiryDate).toInt()
                    if (daysRemaining < 0) {
                        "Plan Expired"
                    } else if (daysRemaining == 0) {
                        "Expires Today"
                    } else {
                        "Expires in $daysRemaining days (${computedExpiryDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"))})"
                    }
                } else null

                val planExercises = planData["exercises"] as? List<*> ?: emptyList<Any>()
                totalCount = planExercises.size

                // Workout history tracking
                val workouts = try { FirebaseService.fetchWorkouts(uid) } catch (_: Exception) { emptyList() }
                
                val todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
                val todayWorkouts = workouts.filter { 
                    (it.second["startedAt"] as? com.google.firebase.Timestamp)?.seconds ?: 0L >= todayStart 
                }

                fullyCompletedToday = todayWorkouts.count { 
                    (it.second["exercisesCompleted"] as? Number)?.toInt() == totalCount 
                }
                
                dailyLimitReached = fullyCompletedToday >= MAX_WORKOUTS_PER_DAY
                
                val latestWorkout = todayWorkouts.firstOrNull()
                completedCount = if (latestWorkout != null) {
                    val comp = (latestWorkout.second["exercisesCompleted"] as? Number)?.toInt() ?: 0
                    if (comp < totalCount) comp else 0 // Reset to 0 if fully completed, so a new session can start
                } else {
                    0
                }

                exercises = planExercises.mapIndexed { index, ex ->
                    val exMap = ex as? Map<*, *> ?: return@mapIndexed null
                    val name = (exMap["name"] as? String)
                        ?: (exMap["title"] as? String)
                        ?: "Exercise ${index + 1}"
                    val sets = (exMap["customSets"] as? Number)?.toInt()
                        ?: (exMap["sets"] as? Number)?.toInt()
                        ?: 0
                    val reps = (exMap["customReps"] as? Number)?.toInt()
                        ?: (exMap["reps"] as? Number)?.toInt()
                        ?: 0
                    val videoUrl = exMap["videoUrl"] as? String
                    val instructionsValue = exMap["instructions"]
                    val instructions = when (instructionsValue) {
                        is String -> instructionsValue
                        is List<*> -> instructionsValue.joinToString("\n") { it?.toString().orEmpty() }
                        else -> null
                    }

                    val status = when {
                        isExpired -> ExStatus.EXPIRED
                        dailyLimitReached && index >= completedCount -> ExStatus.DAILY_LIMIT
                        index < completedCount -> ExStatus.COMPLETED
                        index == completedCount -> ExStatus.ACTIVE
                        else -> ExStatus.PENDING
                    }

                    val statusLabel = when (status) {
                        ExStatus.EXPIRED -> "Expired"
                        ExStatus.DAILY_LIMIT -> "Daily limit reached"
                        else -> null
                    }

                    ExItem(
                        name = name,
                        detail = "$sets Sets • $reps Reps",
                        status = status,
                        sets = sets,
                        reps = reps,
                        videoUrl = videoUrl,
                        instructions = instructions,
                        statusLabel = statusLabel
                    )
                }.filterNotNull()
            }
        } catch (_: Exception) { }
        isLoading = false
    }

    val hour = LocalDateTime.now().hour
    val greeting = when {
        hour in 5..11 -> "Good Morning"
        hour in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(selected = true, onClick = {}, label = { Text("Home") }, icon = { Icon(Icons.Default.Home, contentDescription = "Home") })
                NavigationBarItem(selected = false, onClick = onScheduleTap, label = { Text("Schedule") }, icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Schedule") })
                NavigationBarItem(selected = false, onClick = onAnalyticsTap, label = { Text("Progress") }, icon = { Icon(Icons.Default.Insights, contentDescription = "Progress") })
                NavigationBarItem(selected = false, onClick = onProfile, label = { Text("Profile") }, icon = { Icon(Icons.Default.Person, contentDescription = "Profile") })
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = DesignTokens.Spacing.XL)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.MD),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(greeting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(userName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onChatTap) {
                            Icon(Icons.Default.ChatBubble, contentDescription = "Messages", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        IconButton(onClick = onNotificationsTap) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            }

            // Expiry Banner
            expiryText?.let { text ->
                item {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.XS)
                    )
                }
            }

            // Daily limit warning
            if (dailyLimitReached) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.SM),
                        shape = RoundedCornerShape(DesignTokens.Radius.LG),
                        colors = CardDefaults.cardColors(containerColor = DesignTokens.Colors.Warning.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(DesignTokens.Spacing.MD),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.SM)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = DesignTokens.Colors.Warning)
                            Column {
                                Text("Daily Limit Reached", fontWeight = FontWeight.SemiBold, color = DesignTokens.Colors.Warning)
                                Text("You've completed $MAX_WORKOUTS_PER_DAY workouts today. Come back tomorrow!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Progress card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.XL),
                    shape = RoundedCornerShape(DesignTokens.Radius.XL),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(DesignTokens.Spacing.XL),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.XL)
                    ) {
                        val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
                        val progressPercent = (progress * 100).toInt()
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(80.dp),
                                color = DesignTokens.Colors.Primary,
                                trackColor = DesignTokens.Colors.NeutralLight,
                                strokeWidth = 8.dp,
                                strokeCap = StrokeCap.Round,
                            )
                            Text("$progressPercent%", fontWeight = FontWeight.Bold, color = DesignTokens.Colors.Primary)
                        }
                        Column {
                            Text("Session Progress", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("$completedCount of $totalCount exercises completed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Sessions Today: $fullyCompletedToday / $MAX_WORKOUTS_PER_DAY", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = DesignTokens.Colors.Primary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))
            }

            // Section header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.SM),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Daily Exercises", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    TextButton(onClick = {}) { Text("View All", color = DesignTokens.Colors.Primary) }
                }
            }

            // Exercise list
            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.XL), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DesignTokens.Colors.Primary)
                    }
                }
            }
            itemsIndexed(exercises) { index, exercise ->
                ExerciseRow(
                    item = exercise,
                    sessionLocksEnabled = sessionLocksEnabled,
                    onClick = {
                        if (sessionLocksEnabled && (exercise.status == ExStatus.COMPLETED || exercise.status == ExStatus.EXPIRED || exercise.status == ExStatus.DAILY_LIMIT)) {
                            // Can't open — do nothing
                        } else {
                            onExerciseTap(
                                exercise.name,
                                exercise.videoUrl,
                                exercise.sets,
                                exercise.reps,
                                exercise.instructions,
                                activePlanId ?: "",
                                totalCount,
                                index == totalCount - 1
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ExerciseRow(item: ExItem, sessionLocksEnabled: Boolean, onClick: () -> Unit) {
    val isBlocked = sessionLocksEnabled && (item.status == ExStatus.COMPLETED || item.status == ExStatus.EXPIRED || item.status == ExStatus.DAILY_LIMIT)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.XS)
            .clickable(enabled = !isBlocked, onClick = onClick),
        shape = RoundedCornerShape(DesignTokens.Radius.LG),
        colors = CardDefaults.cardColors(
            containerColor = when (item.status) {
                ExStatus.EXPIRED -> MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                ExStatus.DAILY_LIMIT -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                ExStatus.ACTIVE -> MaterialTheme.colorScheme.surface
                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            }
        ),
        elevation = if (item.status == ExStatus.ACTIVE) CardDefaults.cardElevation(defaultElevation = 4.dp) else CardDefaults.cardElevation()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when (item.status) {
                            ExStatus.COMPLETED -> DesignTokens.Colors.Success.copy(alpha = 0.15f)
                            ExStatus.ACTIVE -> DesignTokens.Colors.Primary.copy(alpha = 0.15f)
                            ExStatus.EXPIRED -> DesignTokens.Colors.Error.copy(alpha = 0.15f)
                            ExStatus.DAILY_LIMIT -> DesignTokens.Colors.Warning.copy(alpha = 0.15f)
                            ExStatus.PENDING -> DesignTokens.Colors.NeutralLight
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (item.status) {
                    ExStatus.COMPLETED -> Text("✓", color = DesignTokens.Colors.Success, fontWeight = FontWeight.Bold)
                    ExStatus.ACTIVE -> Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DesignTokens.Colors.Primary, modifier = Modifier.size(20.dp))
                    ExStatus.EXPIRED -> Icon(Icons.Default.Lock, contentDescription = null, tint = DesignTokens.Colors.Error, modifier = Modifier.size(18.dp))
                    ExStatus.DAILY_LIMIT -> Icon(Icons.Default.Lock, contentDescription = null, tint = DesignTokens.Colors.Warning, modifier = Modifier.size(18.dp))
                    ExStatus.PENDING -> Text("·", color = DesignTokens.Colors.NeutralDark)
                }
            }
            Spacer(modifier = Modifier.width(DesignTokens.Spacing.MD))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Medium,
                    color = if (isBlocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (item.status == ExStatus.COMPLETED) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(item.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                item.statusLabel?.let { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (item.status) {
                            ExStatus.EXPIRED -> DesignTokens.Colors.Error
                            ExStatus.DAILY_LIMIT -> DesignTokens.Colors.Warning
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
