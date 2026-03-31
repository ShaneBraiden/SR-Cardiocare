package com.srcardiocare.ui.screens.notifications

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import com.srcardiocare.core.security.ErrorHandler
import com.srcardiocare.data.firebase.FirebaseService
import com.srcardiocare.ui.theme.DesignTokens
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val type: String,
    val isRead: Boolean,
    val createdAtText: String
)

private fun typeColor(type: String): Color {
    return when (type.lowercase()) {
        "appointment", "appointment_request", "appointment_update" -> DesignTokens.Colors.Warning
        "message" -> DesignTokens.Colors.ChartSecondaryTeal
        "plan", "workout" -> DesignTokens.Colors.Primary
        "achievement" -> DesignTokens.Colors.Success
        else -> DesignTokens.Colors.Slate500
    }
}

private fun formatCreatedAt(raw: Any?): String {
    val formatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
    return when (raw) {
        is Timestamp -> raw.toDate().toInstant().atZone(ZoneId.systemDefault()).format(formatter)
        is String -> {
            try {
                Instant.parse(raw).atZone(ZoneId.systemDefault()).format(formatter)
            } catch (_: Exception) {
                raw
            }
        }
        else -> "Just now"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    suspend fun loadNotifications() {
        try {
            val uid = FirebaseService.currentUID
            if (uid == null) {
                loadError = "Not signed in"
                notifications = emptyList()
                isLoading = false
                return
            }

            val raw = FirebaseService.fetchNotifications(uid)
            notifications = raw.map { (id, data) ->
                NotificationItem(
                    id = id,
                    title = data["title"] as? String ?: "Notification",
                    body = data["body"] as? String ?: "",
                    type = data["type"] as? String ?: "general",
                    isRead = data["isRead"] as? Boolean ?: false,
                    createdAtText = formatCreatedAt(data["createdAt"])
                )
            }
            loadError = null
        } catch (e: Exception) {
            loadError = ErrorHandler.getDisplayMessage(e, "load notifications")
        }
        isLoading = false
    }

    LaunchedEffect(Unit) {
        loadNotifications()
    }

    val unreadCount = notifications.count { !it.isRead }

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
                    if (unreadCount > 0) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    val uid = FirebaseService.currentUID
                                    if (uid == null) {
                                        snackbarHostState.showSnackbar("Not signed in")
                                        return@launch
                                    }
                                    try {
                                        FirebaseService.markAllNotificationsRead(uid)
                                        notifications = notifications.map { it.copy(isRead = true) }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(
                                            ErrorHandler.getDisplayMessage(e, "mark all as read")
                                        )
                                    }
                                }
                            }
                        ) {
                            Text("Mark all")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DesignTokens.Colors.Primary)
                }
            }

            loadError != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Could not load notifications", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            loadError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = {
                            isLoading = true
                            scope.launch { loadNotifications() }
                        }) {
                            Text("Retry")
                        }
                    }
                }
            }

            notifications.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No notifications yet", fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "You will see appointment and activity updates here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.MD),
                    verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.SM)
                ) {
                    item {
                        Text(
                            if (unreadCount > 0) "$unreadCount unread" else "All caught up",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }

                    items(notifications) { notification ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!notification.isRead) {
                                        scope.launch {
                                            try {
                                                FirebaseService.markNotificationRead(notification.id)
                                                notifications = notifications.map {
                                                    if (it.id == notification.id) it.copy(isRead = true) else it
                                                }
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar(
                                                    ErrorHandler.getDisplayMessage(e, "mark notification")
                                                )
                                            }
                                        }
                                    }
                                },
                            shape = RoundedCornerShape(DesignTokens.Radius.LG),
                            colors = CardDefaults.cardColors(
                                containerColor = if (notification.isRead) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(DesignTokens.Spacing.MD)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(typeColor(notification.type), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.size(8.dp))
                                        Text(
                                            notification.title,
                                            fontWeight = if (notification.isRead) FontWeight.Medium else FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Text(
                                        notification.createdAtText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    notification.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (!notification.isRead) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Tap to mark as read",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DesignTokens.Colors.Primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
