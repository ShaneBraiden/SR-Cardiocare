// WorkoutPlayerScreen.kt — Video player with aspect ratio detection, fullscreen, and exercise controls
package com.srcardiocare.ui.screens.workout

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.srcardiocare.ui.theme.DesignTokens
import kotlinx.coroutines.launch

enum class VideoOrientation {
    LANDSCAPE,  // Width > Height (16:9, etc.)
    PORTRAIT,   // Height > Width (9:16, etc.)
    SQUARE      // Width == Height (1:1)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPlayerScreen(
    exerciseName: String,
    videoUrl: String?,
    sets: Int,
    reps: Int,
    instructions: String?,
    planId: String,
    totalCount: Int,
    isLastExercise: Boolean,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentSet by remember { mutableIntStateOf(1) }
    val totalSets = if (sets > 0) sets else 3
    val detailText = "$totalSets Sets • $reps Reps"
    val instructionsText = instructions?.ifBlank { null }
        ?: "Follow the movement slowly and keep proper form throughout each repetition."
    val playerHtml = remember(videoUrl) { buildPlayerHtml(videoUrl) }
    val isYoutube = remember(videoUrl) { !extractYoutubeVideoId(videoUrl.orEmpty()).isNullOrBlank() }

    // Video loading state
    var isVideoLoading by remember { mutableStateOf(true) }
    // Fullscreen state
    var isFullscreen by remember { mutableStateOf(false) }
    // Video orientation detection
    var videoOrientation by remember { mutableStateOf(VideoOrientation.LANDSCAPE) }
    var videoAspectRatio by remember { mutableFloatStateOf(16f / 9f) }

    val exoPlayer = remember(videoUrl) {
        if (videoUrl.isNullOrBlank() || isYoutube) {
            isVideoLoading = false
            null
        } else {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(videoUrl))
                prepare()
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                            isVideoLoading = false
                        }
                    }
                    
                    override fun onVideoSizeChanged(videoSize: VideoSize) {
                        if (videoSize.width > 0 && videoSize.height > 0) {
                            videoAspectRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                            videoOrientation = when {
                                videoSize.width > videoSize.height -> VideoOrientation.LANDSCAPE
                                videoSize.height > videoSize.width -> VideoOrientation.PORTRAIT
                                else -> VideoOrientation.SQUARE
                            }
                        }
                    }
                })
            }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer?.release() }
    }

    // Handle fullscreen orientation - only rotate for landscape videos
    val activity = context as? Activity
    DisposableEffect(isFullscreen, videoOrientation) {
        if (isFullscreen) {
            // Only force landscape if the video is landscape oriented
            when (videoOrientation) {
                VideoOrientation.LANDSCAPE -> {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
                VideoOrientation.PORTRAIT -> {
                    // Keep portrait for portrait videos
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                VideoOrientation.SQUARE -> {
                    // Let user decide for square videos
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Handle back from fullscreen
    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    if (isFullscreen) {
        // Fullscreen video mode
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (exoPlayer != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = true
                            player = exoPlayer
                        }
                    },
                    update = { it.player = exoPlayer },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (isYoutube) {
                YouTubeWebPlayer(
                    html = playerHtml,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Exit fullscreen button
            IconButton(
                onClick = { isFullscreen = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.FullscreenExit,
                    contentDescription = "Exit Fullscreen",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        return
    }

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
            // Video area with dynamic aspect ratio and fullscreen button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        when (videoOrientation) {
                            VideoOrientation.LANDSCAPE -> Modifier.aspectRatio(videoAspectRatio.coerceIn(1.2f, 2.5f))
                            VideoOrientation.PORTRAIT -> Modifier.aspectRatio(videoAspectRatio.coerceIn(0.4f, 0.9f))
                            VideoOrientation.SQUARE -> Modifier.aspectRatio(1f)
                        }
                    )
            ) {
                if (videoUrl.isNullOrBlank()) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                loadDataWithBaseURL(null, playerHtml, "text/html", "utf-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (isYoutube) {
                    YouTubeWebPlayer(
                        html = playerHtml,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = true
                                player = exoPlayer
                            }
                        },
                        update = { it.player = exoPlayer },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Loading overlay
                if (isVideoLoading && !videoUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Loading video…",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Fullscreen button - only show for landscape videos or always for non-portrait
                if (!videoUrl.isNullOrBlank() && videoOrientation != VideoOrientation.PORTRAIT) {
                    IconButton(
                        onClick = { isFullscreen = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Fullscreen,
                            contentDescription = "Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = DesignTokens.Colors.NeutralLight)

            // Exercise details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.XL)
            ) {
                Text(exerciseName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(4.dp))
                Text(detailText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
                Text("Set $currentSet of $totalSets", fontWeight = FontWeight.SemiBold, color = DesignTokens.Colors.Primary)

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

                Text(
                    text = instructionsText,
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
                        scope.launch {
                            val uid = com.srcardiocare.data.firebase.FirebaseService.currentUID
                            if (uid != null && planId.isNotBlank()) {
                                try {
                                    com.srcardiocare.data.firebase.FirebaseService.incrementExerciseProgress(uid, planId, totalCount)
                                } catch (_: Exception) {}
                            }
                            onFinish()
                        }
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YouTubeWebPlayer(html: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context: Context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webViewClient = WebViewClient()
                loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)
            }
        },
        update = { it.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null) },
        modifier = modifier
    )
}

private fun buildPlayerHtml(videoUrl: String?): String {
    if (videoUrl.isNullOrBlank()) {
        return """
            <html><body style="margin:0;padding:0;display:flex;align-items:center;justify-content:center;background:#000;color:#fff;font-family:sans-serif;">
              <div>No video available for this exercise.</div>
            </body></html>
        """.trimIndent()
    }

    val videoId = extractYoutubeVideoId(videoUrl)
    return if (videoId != null) {
        """
            <html><body style="margin:0;padding:0;background:#000;">
              <iframe width="100%" height="100%"
                src="https://www.youtube.com/embed/$videoId?playsinline=1&rel=0"
                frameborder="0" allowfullscreen
                allow="autoplay; encrypted-media; picture-in-picture">
              </iframe>
            </body></html>
        """.trimIndent()
    } else {
        val escapedUrl = videoUrl.replace("&", "&amp;")
        """
            <html><body style="margin:0;padding:0;background:#000;">
              <video width="100%" height="100%" controls playsinline>
                <source src="$escapedUrl" type="video/mp4" />
                Your browser does not support video playback.
              </video>
            </body></html>
        """.trimIndent()
    }
}

private fun extractYoutubeVideoId(url: String): String? {
    return runCatching {
        val uri = Uri.parse(url)
        when {
            uri.host?.contains("youtu.be") == true -> uri.lastPathSegment
            uri.host?.contains("youtube.com") == true && uri.path?.startsWith("/embed/") == true -> {
                uri.pathSegments.getOrNull(1)
            }
            uri.host?.contains("youtube.com") == true -> uri.getQueryParameter("v")
            else -> null
        }
    }.getOrNull()?.takeIf { it.isNotBlank() }
}
