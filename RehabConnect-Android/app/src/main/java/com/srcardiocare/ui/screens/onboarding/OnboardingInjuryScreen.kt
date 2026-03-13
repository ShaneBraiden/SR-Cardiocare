// OnboardingInjuryScreen.kt — Multi-select body part grid
package com.srcardiocare.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.srcardiocare.ui.theme.DesignTokens

private data class BodyPart(val name: String, val emoji: String, val subtitle: String)

private val bodyParts = listOf(
    BodyPart("Knee", "🦵", "ACL, Meniscus"),
    BodyPart("Shoulder", "💪", "Rotator Cuff"),
    BodyPart("Lower Back", "🔙", "Lumbar, Disc"),
    BodyPart("Neck", "🧣", "Cervical"),
    BodyPart("Ankle", "🦶", "Sprain, Fracture"),
    BodyPart("Hip", "🏃", "Replacement, Bursitis"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingInjuryScreen(onNext: () -> Unit, onBack: () -> Unit) {
    var selectedParts by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Step 2 of 3", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
                text = "Where does it hurt?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))
            Text(
                text = "Select all areas that apply. This helps us tailor your recovery plan.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.MD),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.MD),
                modifier = Modifier.weight(1f)
            ) {
                items(bodyParts) { part ->
                    val selected = part.name in selectedParts
                    OutlinedCard(
                        onClick = {
                            selectedParts = if (selected) selectedParts - part.name else selectedParts + part.name
                        },
                        shape = RoundedCornerShape(DesignTokens.Radius.LG),
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) DesignTokens.Colors.Primary else DesignTokens.Colors.NeutralLight
                        ),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (selected) DesignTokens.Colors.PrimaryLight.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().padding(DesignTokens.Spacing.MD)) {
                            Column {
                                Text(part.emoji, style = MaterialTheme.typography.headlineMedium)
                                Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))
                                Text(part.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                Text(part.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (selected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = DesignTokens.Colors.Primary,
                                    modifier = Modifier.align(Alignment.TopEnd)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.Primary),
                enabled = selectedParts.isNotEmpty()
            ) {
                Text("Next", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))
        }
    }
}
