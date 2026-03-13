// OnboardingWelcomeScreen.kt — First onboarding screen
package com.srcardiocare.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.srcardiocare.ui.theme.DesignTokens

@Composable
fun OnboardingWelcomeScreen(onNext: () -> Unit, onSkip: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DesignTokens.Spacing.XL),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Hero placeholder
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(DesignTokens.Colors.PrimaryLight),
                contentAlignment = Alignment.Center
            ) {
                Text("💪", style = MaterialTheme.typography.displayLarge)
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XXL))

            Text(
                text = "Your Recovery\nStarts Here",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            Text(
                text = "Personalised cardiac rehabilitation programs designed with your cardiologist, tracked in real time.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XXL))

            // Page dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .size(if (i == 0) 24.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(if (i == 0) DesignTokens.Colors.Primary else DesignTokens.Colors.NeutralLight)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.Primary)
            ) {
                Text("Next", fontWeight = FontWeight.SemiBold)
            }

            TextButton(onClick = onSkip) {
                Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))
        }
    }
}
