// NotificationsScreen.kt — Grouped notification settings with toggles
package com.srcardiocare.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.srcardiocare.ui.theme.DesignTokens

private data class NotifItem(
    val title: String,
    val subtitle: String? = null,
    val emoji: String,
    val color: Color,
    val defaultOn: Boolean = true
)

private data class NotifSection(val title: String, val footer: String? = null, val items: List<NotifItem>)

private val sections = listOf(
    NotifSection(
        title = "REHABILITATION & GOALS",
        footer = "Receive gentle nudges to keep your recovery on track.",
        items = listOf(
            NotifItem("Daily Exercise Alerts", null, "🏋️", DesignTokens.Colors.Primary),
            NotifItem("Missed Session Alerts", null, "⚠️", DesignTokens.Colors.Warning),
        )
    ),
    NotifSection(
        title = "DOCTOR & CLINIC",
        items = listOf(
            NotifItem("New Messages", null, "💬", DesignTokens.Colors.ChartSecondaryTeal),
            NotifItem("Appointment Reminders", null, "📅", DesignTokens.Colors.Slate500),
        )
    ),
    NotifSection(
        title = "PROGRESS & ACHIEVEMENTS",
        items = listOf(
            NotifItem("Weekly Progress Summary", null, "📊", DesignTokens.Colors.Success, defaultOn = false),
            NotifItem("Achievement Badges", null, "🏅", DesignTokens.Colors.Warning),
        )
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val toggleStates = remember {
        mutableStateMapOf<String, Boolean>().apply {
            sections.flatMap { it.items }.forEach { put(it.title, it.defaultOn) }
        }
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DesignTokens.Spacing.XL)
        ) {
            sections.forEach { section ->
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

                Text(
                    section.title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing
                )

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))

                Card(
                    shape = RoundedCornerShape(DesignTokens.Radius.LG),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    section.items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = DesignTokens.Spacing.MD, vertical = DesignTokens.Spacing.MD),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(item.color.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(item.emoji, style = MaterialTheme.typography.bodyMedium)
                            }

                            Spacer(modifier = Modifier.width(DesignTokens.Spacing.MD))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                item.subtitle?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Switch(
                                checked = toggleStates[item.title] ?: item.defaultOn,
                                onCheckedChange = { toggleStates[item.title] = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = DesignTokens.Colors.Primary
                                )
                            )
                        }

                        if (index < section.items.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 56.dp),
                                color = DesignTokens.Colors.NeutralLight
                            )
                        }
                    }
                }

                section.footer?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = DesignTokens.Spacing.SM)
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XXL))

            Text(
                "To change system-level permissions, visit your device Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XXL))
        }
    }
}
