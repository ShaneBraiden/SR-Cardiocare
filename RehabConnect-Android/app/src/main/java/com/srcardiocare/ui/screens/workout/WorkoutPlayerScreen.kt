// WorkoutPlayerScreen.kt — Video player with exercise controls
package com.srcardiocare.ui.screens.workout

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.srcardiocare.ui.theme.DesignTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPlayerScreen(onFinish: () -> Unit, onBack: () -> Unit) {
    var currentSet by remember { mutableIntStateOf(1) }
    val totalSets = 3

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout", fontWeight = FontWeight.Bold) },
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
        ) {
            // YouTube video player
            // TODO: pass actual videoId from exercise data
            val videoId = "" // Extract from exercise.videoUrl
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        loadData(
                            """<html><body style="margin:0;padding:0">
                            <iframe width="100%" height="100%"
                              src="https://www.youtube.com/embed/$videoId?playsinline=1"
                              frameborder="0" allowfullscreen
                              allow="autoplay; encrypted-media">
                            </iframe></body></html>""".trimIndent(),
                            "text/html", "utf-8"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )

            HorizontalDivider(color = DesignTokens.Colors.NeutralLight)

            // Exercise details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.XL)
            ) {
                // TODO: Load exercise name from workout plan API
                Text("", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                // TODO: Load sets/reps/duration from exercise plan
                Text("", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
                Text("Set $currentSet of $totalSets", fontWeight = FontWeight.SemiBold, color = DesignTokens.Colors.Primary)

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

                // TODO: Load instruction text from exercise
                Text(
                    text = "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Finish button
            Button(
                onClick = {
                    if (currentSet < totalSets) {
                        currentSet++
                    } else {
                        onFinish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.MD)
                    .height(52.dp),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.Primary)
            ) {
                Text(
                    if (currentSet < totalSets) "Finish Set" else "Complete Workout",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
        }
    }
}
