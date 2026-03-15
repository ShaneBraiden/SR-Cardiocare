// PatientHomeScreen.kt — Patient dashboard with progress ring and exercises
package com.srcardiocare.ui.screens.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
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
import java.time.LocalDateTime

private enum class ExStatus { COMPLETED, ACTIVE, PENDING }
private data class ExItem(val name: String, val detail: String, val status: ExStatus)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientHomeScreen(
    onExerciseTap: () -> Unit,
    onScheduleTap: () -> Unit,
    onAnalyticsTap: () -> Unit,
    onNotificationsTap: () -> Unit,
    onProfile: () -> Unit = {}
) {
    var exercises by remember { mutableStateOf<List<ExItem>>(emptyList()) }
    var userName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var completedCount by remember { mutableIntStateOf(0) }
    var totalCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        try {
            val uid = FirebaseService.currentUID ?: return@LaunchedEffect
            val userData = FirebaseService.fetchUser(uid)
            userName = userData["firstName"] as? String ?: ""

            // Update last seen for online tracking
            FirebaseService.updateLastSeen()

            val plans = FirebaseService.fetchPlans(uid)
            val activePlan = plans.firstOrNull { (it.second["isActive"] as? Boolean) == true }
            if (activePlan != null) {
                val planExercises = activePlan.second["exercises"] as? List<*> ?: emptyList<Any>()
                totalCount = planExercises.size

                val workouts = FirebaseService.fetchWorkouts(uid)
                val latestWorkout = workouts.firstOrNull()
                completedCount = (latestWorkout?.second?.get("exercisesCompleted") as? Number)?.toInt() ?: 0

                exercises = planExercises.mapIndexed { index, ex ->
                    val exMap = ex as? Map<*, *> ?: return@mapIndexed null
                    val name = exMap["name"] as? String ?: "Exercise ${index + 1}"
                    val sets = (exMap["customSets"] as? Number)?.toInt() ?: 0
                    val reps = (exMap["customReps"] as? Number)?.toInt() ?: 0
                    val status = when {
                        index < completedCount -> ExStatus.COMPLETED
                        index == completedCount -> ExStatus.ACTIVE
                        else -> ExStatus.PENDING
                    }
                    ExItem(name, "$sets Sets • $reps Reps", status)
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
                    IconButton(onClick = onNotificationsTap) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onBackground)
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
                            Text("Today's Progress", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$completedCount of $totalCount exercises completed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            items(exercises) { exercise ->
                ExerciseRow(item = exercise, onClick = { if (exercise.status != ExStatus.COMPLETED) onExerciseTap() })
            }
        }
    }
}

@Composable
private fun ExerciseRow(item: ExItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.XS)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(DesignTokens.Radius.LG),
        colors = CardDefaults.cardColors(
            containerColor = if (item.status == ExStatus.ACTIVE) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
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
                            ExStatus.PENDING -> DesignTokens.Colors.NeutralLight
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when (item.status) {
                    ExStatus.COMPLETED -> Text("✓", color = DesignTokens.Colors.Success, fontWeight = FontWeight.Bold)
                    ExStatus.ACTIVE -> Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DesignTokens.Colors.Primary, modifier = Modifier.size(20.dp))
                    ExStatus.PENDING -> Text("·", color = DesignTokens.Colors.NeutralDark)
                }
            }
            Spacer(modifier = Modifier.width(DesignTokens.Spacing.MD))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Medium,
                    color = if (item.status == ExStatus.COMPLETED) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (item.status == ExStatus.COMPLETED) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(item.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
