package com.srcardiocare.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.srcardiocare.data.firebase.FirebaseService
import com.srcardiocare.ui.theme.DesignTokens
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenRoute: (route: String, params: Map<String, String>) -> Unit
) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<Pair<String, Map<String, Any?>>>>(emptyList()) }

    LaunchedEffect(Unit) {
        val uid = FirebaseService.currentUID ?: return@LaunchedEffect
        FirebaseService.observeNotifications(uid).collect { items = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val unread = items.count { (it.second["isRead"] as? Boolean) != true }
                    if (unread > 0) {
                        IconButton(onClick = {
                            scope.launch {
                                val uid = FirebaseService.currentUID ?: return@launch
                                runCatching { FirebaseService.markAllNotificationsRead(uid) }
                            }
                        }) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Mark all read")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "You're all caught up.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(DesignTokens.Spacing.MD),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.SM)
        ) {
            items(items, key = { it.first }) { (id, data) ->
                NotificationRow(
                    data = data,
                    onTap = {
                        scope.launch { runCatching { FirebaseService.markNotificationRead(id) } }
                        val route = data["route"] as? String ?: ""
                        @Suppress("UNCHECKED_CAST")
                        val params = (data["params"] as? Map<String, String>).orEmpty()
                        if (route.isNotBlank()) onOpenRoute(route, params)
                    }
                )
            }
        }
    }
}

@Composable
private fun NotificationRow(
    data: Map<String, Any?>,
    onTap: () -> Unit
) {
    val isRead = (data["isRead"] as? Boolean) == true
    val type = (data["type"] as? String).orEmpty()
    val (icon, tint) = iconFor(type)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(DesignTokens.Radius.LG),
        colors = CardDefaults.cardColors(
            containerColor = if (isRead) MaterialTheme.colorScheme.surface
            else DesignTokens.Colors.Primary.copy(alpha = 0.06f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.LG),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.MD)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = data["title"] as? String ?: "Notification",
                    fontWeight = if (isRead) FontWeight.Medium else FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val body = data["body"] as? String
                if (!body.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val timeLabel = formatTime(data["createdAt"])
                if (timeLabel.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            if (!isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(DesignTokens.Colors.Primary)
                )
            }
        }
    }
}

private fun iconFor(type: String): Pair<ImageVector, androidx.compose.ui.graphics.Color> = when (type.lowercase()) {
    "exercise", "prescription" -> Icons.Default.FitnessCenter to DesignTokens.Colors.Primary
    "message" -> Icons.Default.ChatBubble to DesignTokens.Colors.Primary
    "appointment", "appointment_update", "appointment_request" -> Icons.Default.CalendarMonth to DesignTokens.Colors.Primary
    "doctor_assigned" -> Icons.Default.Person to DesignTokens.Colors.Primary
    else -> Icons.Default.Notifications to DesignTokens.Colors.Primary
}

private fun formatTime(raw: Any?): String {
    val millis = when (raw) {
        is com.google.firebase.Timestamp -> raw.toDate().time
        is Long -> raw
        else -> return ""
    }
    val now = System.currentTimeMillis()
    val diff = now - millis
    val sec = diff / 1000
    val min = sec / 60
    val hr = min / 60
    val day = hr / 24
    return when {
        sec < 60 -> "Just now"
        min < 60 -> "${min}m ago"
        hr < 24 -> "${hr}h ago"
        day < 7 -> "${day}d ago"
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(millis))
    }
}
