package com.srcardiocare.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.srcardiocare.ui.theme.DesignTokens
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val iconTint: androidx.compose.ui.graphics.Color = DesignTokens.Colors.Primary,
    val title: String,
    val subtitle: String
)

private val PAGES = listOf(
    OnboardingPage(
        icon = Icons.Filled.Favorite,
        title = "Welcome to SR-Cardiocare",
        subtitle = "Your personal digital physiotherapy companion — designed to support your recovery every step of the way."
    ),
    OnboardingPage(
        icon = Icons.Filled.FitnessCenter,
        title = "Your Exercise Plan",
        subtitle = "Your doctor assigns personalised exercises tailored to your recovery. Complete each session at your own pace, any time of day."
    ),
    OnboardingPage(
        icon = Icons.Filled.BarChart,
        title = "Track Your Progress",
        subtitle = "Log every session, review your history, and watch your recovery advance with clear analytics and progress charts."
    ),
    OnboardingPage(
        icon = Icons.Filled.Chat,
        title = "Stay Connected",
        subtitle = "Message your care team directly and receive real-time notifications about new exercises, feedback, and updates."
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == PAGES.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        // Top bar with Skip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.Base, vertical = DesignTokens.Spacing.SM)
        ) {
            if (!isLastPage) {
                TextButton(
                    onClick = onFinish,
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text(
                        "Skip",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Pages
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            PageContent(page = PAGES[page])
        }

        // Dot indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = DesignTokens.Spacing.LG),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PAGES.indices.forEach { index ->
                val selected = pagerState.currentPage == index
                val width by animateDpAsState(
                    targetValue = if (selected) 28.dp else 8.dp,
                    animationSpec = tween(250),
                    label = "dot_$index"
                )
                val color by animateColorAsState(
                    targetValue = if (selected) DesignTokens.Colors.Primary else DesignTokens.Colors.NeutralLight,
                    animationSpec = tween(250),
                    label = "dot_color_$index"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }

        // CTA button
        Button(
            onClick = {
                if (isLastPage) {
                    onFinish()
                } else {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DesignTokens.Spacing.XL)
                .padding(bottom = DesignTokens.Spacing.XXL)
                .height(52.dp),
            shape = RoundedCornerShape(DesignTokens.Radius.Button),
            colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.Primary)
        ) {
            Text(
                text = if (isLastPage) "Get Started" else "Next",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun PageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = DesignTokens.Spacing.XXL),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon badge
        Box(
            modifier = Modifier
                .size(128.dp)
                .clip(CircleShape)
                .background(DesignTokens.Colors.Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = DesignTokens.Colors.Primary,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(DesignTokens.Spacing.XXL))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

        Text(
            text = page.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
