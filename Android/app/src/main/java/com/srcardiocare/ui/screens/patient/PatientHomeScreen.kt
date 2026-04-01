// PatientHomeScreen.kt — Patient dashboard with cards for navigation
package com.srcardiocare.ui.screens.patient

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.srcardiocare.R
import com.srcardiocare.core.NotificationService
import com.srcardiocare.data.firebase.FirebaseService
import com.srcardiocare.ui.components.InAppPopup
import com.srcardiocare.ui.components.PopupAction
import com.srcardiocare.ui.components.PopupType
import com.srcardiocare.ui.components.rememberPopupController
import com.srcardiocare.ui.theme.DesignTokens
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.launch

private const val MAX_WORKOUTS_PER_DAY = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientHomeScreen(
    onExerciseTap: () -> Unit,
    onScheduleTap: () -> Unit,
    onAnalyticsTap: () -> Unit,
    onNotificationsTap: () -> Unit,
    onChatTap: () -> Unit = {},
    onProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val popupController = rememberPopupController()

    var userName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var completedCount by remember { mutableIntStateOf(0) }
    var totalCount by remember { mutableIntStateOf(0) }
    var fullyCompletedToday by remember { mutableIntStateOf(0) }
    var expiryText by remember { mutableStateOf<String?>(null) }
    var unreadNotificationCount by remember { mutableIntStateOf(0) }
    var unreadChatCount by remember { mutableIntStateOf(0) }
    var latestPopupNotificationId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    try {
                        val uid = FirebaseService.currentUID ?: return@launch
                        val userData = FirebaseService.fetchUser(uid)
                        userName = userData["firstName"] as? String ?: ""

                        FirebaseService.updateLastSeen()

                        val plans = FirebaseService.fetchPlans(uid)
                        val activePlan = plans.firstOrNull { (it.second["isActive"] as? Boolean) == true }
                            ?: plans.firstOrNull()
                        if (activePlan != null) {
                            val planData = activePlan.second

                            // Check plan expiry
                            val expiryDateRaw = planData["expiryDate"]
                            val expiryDays = (planData["expiryDays"] as? Number)?.toInt()
                            val planCreatedAt = planData["createdAt"]
                            val today = LocalDate.now()

                            var computedExpiryDate: LocalDate? = null

                            when {
                                expiryDateRaw is String -> {
                                    try {
                                        computedExpiryDate = LocalDate.parse(expiryDateRaw)
                                    } catch (_: Exception) { }
                                }
                                expiryDateRaw is com.google.firebase.Timestamp -> {
                                    computedExpiryDate = expiryDateRaw.toDate().toInstant()
                                        .atZone(ZoneId.systemDefault()).toLocalDate()
                                }
                                expiryDays != null && planCreatedAt is com.google.firebase.Timestamp -> {
                                    val createdDate = planCreatedAt.toDate().toInstant()
                                        .atZone(ZoneId.systemDefault()).toLocalDate()
                                    computedExpiryDate = createdDate.plusDays(expiryDays.toLong())
                                }
                            }

                            expiryText = if (computedExpiryDate != null) {
                                val daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(today, computedExpiryDate).toInt()
                                if (daysRemaining < 0) {
                                    "Plan Expired"
                                } else if (daysRemaining == 0) {
                                    "Expires Today"
                                } else {
                                    "Expires in $daysRemaining days"
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

                            val latestWorkout = todayWorkouts.firstOrNull()
                            completedCount = if (latestWorkout != null) {
                                (latestWorkout.second["exercisesCompleted"] as? Number)?.toInt() ?: 0
                            } else {
                                0
                            }

                            val pendingExercises = (totalCount - completedCount).coerceAtLeast(0)
                            val hourNow = LocalDateTime.now().hour
                            if (pendingExercises > 0 && hourNow >= 18) {
                                val reminderTitle = "Workout reminder"
                                val reminderBody = "You still have $pendingExercises exercise${if (pendingExercises != 1) "s" else ""} pending for today."

                                val created = try {
                                    FirebaseService.ensureDailyWorkoutRiskNotification(
                                        userId = uid,
                                        title = reminderTitle,
                                        body = reminderBody
                                    )
                                } catch (_: Exception) {
                                    false
                                }

                                if (created) {
                                    NotificationService.showWorkoutReminderNotification(
                                        context = context,
                                        workoutName = "Today's workout",
                                        minutesBefore = 0,
                                        notificationId = 3901
                                    )
                                    popupController.show(
                                        type = PopupType.WARNING,
                                        title = reminderTitle,
                                        message = reminderBody,
                                        primaryAction = PopupAction("Open", onExerciseTap, true),
                                        secondaryAction = PopupAction("Later", {})
                                    )
                                }
                            }
                        }
                    } catch (_: Exception) { }
                    isLoading = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        val uid = FirebaseService.currentUID ?: return@LaunchedEffect
        FirebaseService.observeNotifications(uid).collect { raw ->
            val unread = raw.filter { (_, data) -> (data["isRead"] as? Boolean) != true }
            unreadNotificationCount = unread.size
            unreadChatCount = unread.count { (_, data) ->
                (data["type"] as? String)?.lowercase() == "message"
            }

            val newestUnread = unread.firstOrNull() ?: return@collect
            if (newestUnread.first == latestPopupNotificationId) return@collect

            val data = newestUnread.second
            val popupType = when ((data["type"] as? String)?.lowercase()) {
                "workout", "workout_risk" -> PopupType.WARNING
                "feedback", "achievement" -> PopupType.SUCCESS
                "message" -> PopupType.INFO
                "appointment", "appointment_update", "appointment_request" -> PopupType.WARNING
                else -> PopupType.INFO
            }

            popupController.show(
                type = popupType,
                title = data["title"] as? String ?: "New update",
                message = (data["body"] as? String).orEmpty().ifBlank { "You have a new notification." },
                primaryAction = PopupAction("View", onNotificationsTap, true),
                secondaryAction = PopupAction("Later", {})
            )
            latestPopupNotificationId = newestUnread.first
        }
    }

    val hour = LocalDateTime.now().hour
    val greeting = when {
        hour in 5..11 -> "Good Morning"
        hour in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.sr_logo),
                                contentDescription = "SrCardioCare logo",
                                modifier = Modifier.size(38.dp),
                                contentScale = ContentScale.Fit
                            )
                            Column {
                                Text(
                                    "SrCardioCare",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "A digital health platform for Cardiac rehabilitation",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = DesignTokens.Spacing.XL)
            ) {
            // Welcome header
            item {
                Column(modifier = Modifier.padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.MD)) {
                    Text(greeting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(userName.ifBlank { "Loading…" }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                }
            }

            // Progress card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.XL),
                    shape = RoundedCornerShape(DesignTokens.Radius.XL),
                    colors = CardDefaults.cardColors(containerColor = DesignTokens.Colors.Primary)
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
                                color = MaterialTheme.colorScheme.onPrimary,
                                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f),
                                strokeWidth = 8.dp,
                                strokeCap = StrokeCap.Round,
                            )
                            Text("$progressPercent%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                        Column {
                            Text("Today's Progress", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("$completedCount of $totalCount exercises", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))
            }

            // Dashboard Cards section
            item {
                Text(
                    "Dashboard",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.SM)
                )
            }

            // Navigation Cards Grid - Row 1
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.XL),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.MD)
                ) {
                    DashboardCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FitnessCenter,
                        title = "Exercises",
                        subtitle = "$totalCount assigned",
                        badgeCount = if (totalCount - completedCount > 0) totalCount - completedCount else null,
                        onClick = onExerciseTap
                    )
                    DashboardCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CalendarMonth,
                        title = "Schedule",
                        subtitle = "View appointments",
                        onClick = onScheduleTap
                    )
                }
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
            }

            // Navigation Cards Grid - Row 2
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.XL),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.MD)
                ) {
                    DashboardCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Insights,
                        title = "Progress",
                        subtitle = "Track your stats",
                        onClick = onAnalyticsTap
                    )
                    DashboardCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ChatBubble,
                        title = "Messages",
                        subtitle = "Chat with doctor",
                        showDot = unreadChatCount > 0,
                        onClick = onChatTap
                    )
                }
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
            }

            // Navigation Cards Grid - Row 3
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.XL),
                    horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.MD)
                ) {
                    DashboardCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        subtitle = "View alerts",
                        badgeCount = unreadNotificationCount.takeIf { it > 0 },
                        onClick = onNotificationsTap
                    )
                    DashboardCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Person,
                        title = "Profile",
                        subtitle = "Your account",
                        onClick = onProfile
                    )
                }
            }
            }
        }

        InAppPopup(
            visible = popupController.isVisible,
            onDismiss = { popupController.dismiss() },
            type = popupController.type,
            title = popupController.title,
            message = popupController.message,
            primaryAction = popupController.primaryAction,
            secondaryAction = popupController.secondaryAction
        )
    }
}

@Composable
private fun DashboardCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    badgeCount: Int? = null,
    showDot: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(DesignTokens.Radius.LG),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.LG),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DesignTokens.Colors.Primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = DesignTokens.Colors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (badgeCount != null) {
                    Badge(
                        modifier = Modifier.align(Alignment.TopEnd),
                        containerColor = DesignTokens.Colors.Error
                    ) {
                        Text(badgeCount.toString())
                    }
                } else if (showDot) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(DesignTokens.Colors.Error)
                    )
                }
            }
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
