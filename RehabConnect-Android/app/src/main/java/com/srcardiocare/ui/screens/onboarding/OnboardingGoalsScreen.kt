// OnboardingGoalsScreen.kt — Single-select goal picker
package com.srcardiocare.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.srcardiocare.ui.theme.DesignTokens

private data class Goal(val title: String, val icon: String)

private val goals = listOf(
    Goal("Reduce Pain", "🎯"),
    Goal("Increase Mobility", "🏃"),
    Goal("Post-Surgery Recovery", "🏥"),
    Goal("Sports Performance", "⚽"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingGoalsScreen(onComplete: () -> Unit, onBack: () -> Unit) {
    var selectedGoal by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Step 3 of 3", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                .padding(horizontal = DesignTokens.Spacing.XL)
        ) {
            Text(
                text = "What's your primary goal?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))
            Text(
                text = "This helps us personalise your rehabilitation programme.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

            goals.forEach { goal ->
                val selected = selectedGoal == goal.title
                OutlinedCard(
                    onClick = { selectedGoal = goal.title },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = DesignTokens.Spacing.MD),
                    shape = RoundedCornerShape(DesignTokens.Radius.LG),
                    border = BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) DesignTokens.Colors.Primary else DesignTokens.Colors.NeutralLight
                    ),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selected) DesignTokens.Colors.PrimaryLight.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(DesignTokens.Spacing.MD),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.MD)
                    ) {
                        // Radio circle
                        RadioButton(
                            selected = selected,
                            onClick = { selectedGoal = goal.title },
                            colors = RadioButtonDefaults.colors(selectedColor = DesignTokens.Colors.Primary)
                        )
                        Text(goal.icon, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = goal.title,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.Primary),
                enabled = selectedGoal != null
            ) {
                Text("Get Started", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))
        }
    }
}
