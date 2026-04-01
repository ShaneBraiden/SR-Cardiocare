// AssignmentWorkoutScreen.kt — Workout player with set-by-set completion and session tracking
package com.srcardiocare.ui.screens.workout

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.srcardiocare.data.firebase.FirebaseService
import com.srcardiocare.data.model.Assignment
import com.srcardiocare.ui.theme.DesignTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class WorkoutPhase {
    READY,          // Initial state, ready to start
    WATCHING,       // Video is playing
    SET_COMPLETE,   // Just finished a set, showing popup
    ALL_SETS_DONE,  // All sets done, ready to complete
    COMPLETING      // Submitting completion
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentWorkoutScreen(
    assignment: Assignment,
    sessionNumber: Int,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Session state
    var sessionId by remember { mutableStateOf<String?>(null) }
    var currentSet by remember { mutableIntStateOf(1) }
    var phase by remember { mutableStateOf(WorkoutPhase.READY) }
    var videoWatchedSeconds by remember { mutableIntStateOf(0) }
    var setStartTime by remember { mutableStateOf<Long?>(null) }
    
    // UI state
    var isVideoLoading by remember { mutableStateOf(true) }
    var isFullscreen by remember { mutableStateOf(false) }
    var videoOrientation by remember { mutableStateOf(VideoOrientation.LANDSCAPE) }
    var videoAspectRatio by remember { mutableFloatStateOf(16f / 9f) }
    var showAbandonDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val totalSets = assignment.sets
    val videoUrl = assignment.exerciseVideoUrl
    val isYoutube = remember(videoUrl) { 
        !extractYoutubeVideoId(videoUrl.orEmpty()).isNullOrBlank() 
    }
    val playerHtml = remember(videoUrl) { buildPlayerHtml(videoUrl) }

    // Initialize session on first load
    LaunchedEffect(Unit) {
        try {
            val today = LocalDate.now().toString()
            val id = FirebaseService.startSession(
                assignmentId = assignment.id,
                sessionDate = today,
                sessionNumber = sessionNumber,
                totalSets = totalSets
            )
            sessionId = id
        } catch (e: Exception) {
            errorMessage = "Failed to start session: ${e.message}"
        }
    }

    // Track video playback time
    LaunchedEffect(phase) {
        if (phase == WorkoutPhase.WATCHING) {
            setStartTime = System.currentTimeMillis()
            while (phase == WorkoutPhase.WATCHING) {
                delay(1000)
                videoWatchedSeconds++
            }
        }
    }

    // ExoPlayer setup
    val exoPlayer = remember(videoUrl) {
        if (videoUrl.isNullOrBlank() || isYoutube) {
            isVideoLoading = false
            null
        } else {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(videoUrl))
                prepare()
                playWhenReady = false // Don't auto-play
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            isVideoLoading = false
                        }
                        // When video ends, mark set as complete
                        if (playbackState == Player.STATE_ENDED && phase == WorkoutPhase.WATCHING) {
                            phase = WorkoutPhase.SET_COMPLETE
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

    // Handle fullscreen orientation
    val activity = context as? Activity
    DisposableEffect(isFullscreen, videoOrientation) {
        if (isFullscreen) {
            when (videoOrientation) {
                VideoOrientation.LANDSCAPE -> {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
                VideoOrientation.PORTRAIT -> {
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                VideoOrientation.SQUARE -> {
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

    // Handle back button
    BackHandler(enabled = isFullscreen || phase != WorkoutPhase.READY) {
        when {
            isFullscreen -> isFullscreen = false
            phase != WorkoutPhase.READY -> showAbandonDialog = true
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SET COMPLETION POPUP
    // ═══════════════════════════════════════════════════════════════════════════
    if (phase == WorkoutPhase.SET_COMPLETE) {
        SetCompletePopup(
            currentSet = currentSet,
            totalSets = totalSets,
            onContinue = {
                scope.launch {
                    // Log the completed set
                    sessionId?.let { id ->
                        try {
                            FirebaseService.logSetCompletion(
                                sessionId = id,
                                setNumber = currentSet,
                                videoWatchedSeconds = videoWatchedSeconds,
                                repsCompleted = assignment.reps
                            )
                        } catch (_: Exception) {}
                    }
                    
                    // Reset for next set
                    videoWatchedSeconds = 0
                    
                    if (currentSet < totalSets) {
                        currentSet++
                        phase = WorkoutPhase.READY
                        exoPlayer?.seekTo(0)
                        exoPlayer?.pause()
                    } else {
                        phase = WorkoutPhase.ALL_SETS_DONE
                    }
                }
            },
            onFinishLater = {
                scope.launch {
                    // Log partial completion and abandon
                    sessionId?.let { id ->
                        try {
                            FirebaseService.logSetCompletion(
                                sessionId = id,
                                setNumber = currentSet,
                                videoWatchedSeconds = videoWatchedSeconds,
                                repsCompleted = assignment.reps
                            )
                            FirebaseService.abandonSession(id)
                        } catch (_: Exception) {}
                    }
                    onBack()
                }
            }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ALL SETS DONE POPUP
    // ═══════════════════════════════════════════════════════════════════════════
    if (phase == WorkoutPhase.ALL_SETS_DONE) {
        AllSetsDonePopup(
            exerciseName = assignment.exerciseName,
            sessionNumber = sessionNumber,
            onComplete = {
                phase = WorkoutPhase.COMPLETING
                scope.launch {
                    sessionId?.let { id ->
                        try {
                            FirebaseService.completeSession(id)
                        } catch (_: Exception) {}
                    }
                    onComplete()
                }
            }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ABANDON SESSION DIALOG
    // ═══════════════════════════════════════════════════════════════════════════
    if (showAbandonDialog) {
        AlertDialog(
            onDismissRequest = { showAbandonDialog = false },
            title = { Text("Leave Workout?") },
            text = { 
                Text("Your progress for this session will be saved as incomplete. You can continue later today.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            sessionId?.let { id ->
                                try {
                                    FirebaseService.abandonSession(id)
                                } catch (_: Exception) {}
                            }
                            onBack()
                        }
                    }
                ) {
                    Text("Leave", color = DesignTokens.Colors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbandonDialog = false }) {
                    Text("Continue Workout")
                }
            }
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FULLSCREEN MODE
    // ═══════════════════════════════════════════════════════════════════════════
    if (isFullscreen) {
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

    // ═══════════════════════════════════════════════════════════════════════════
    // MAIN UI
    // ═══════════════════════════════════════════════════════════════════════════
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Session $sessionNumber", fontWeight = FontWeight.Bold)
                        Text(
                            "Set $currentSet of $totalSets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (phase == WorkoutPhase.READY) onBack() else showAbandonDialog = true 
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Progress indicator
            SetProgressIndicator(
                currentSet = currentSet,
                totalSets = totalSets,
                modifier = Modifier.padding(horizontal = DesignTokens.Spacing.XL)
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            // Video player area
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
                    // No video placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.VideocamOff,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No video available",
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
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
                        CircularProgressIndicator(color = Color.White)
                    }
                }

                // Fullscreen button shown for all video orientations
                if (!videoUrl.isNullOrBlank()) {
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
                Text(
                    assignment.exerciseName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    "${assignment.sets} Sets • ${assignment.reps} Reps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.LG))

                // Current set highlight
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = DesignTokens.Colors.Primary.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(DesignTokens.Radius.Card)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(DesignTokens.Spacing.MD),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DesignTokens.Colors.Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$currentSet",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.width(DesignTokens.Spacing.MD))
                        Column {
                            Text(
                                "Set $currentSet of $totalSets",
                                fontWeight = FontWeight.SemiBold,
                                color = DesignTokens.Colors.Primary
                            )
                            Text(
                                "${assignment.reps} repetitions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Instructions
                if (!assignment.instructions.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.LG))
                    Text(
                        "Instructions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.XS))
                    Text(
                        assignment.instructions,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action button
            Button(
                onClick = {
                    when (phase) {
                        WorkoutPhase.READY -> {
                            phase = WorkoutPhase.WATCHING
                            exoPlayer?.play()
                        }
                        WorkoutPhase.WATCHING -> {
                            // Manual finish set (if they don't wait for video to end)
                            exoPlayer?.pause()
                            phase = WorkoutPhase.SET_COMPLETE
                        }
                        else -> {}
                    }
                },
                enabled = phase == WorkoutPhase.READY || phase == WorkoutPhase.WATCHING,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.MD)
                    .height(56.dp),
                shape = RoundedCornerShape(DesignTokens.Radius.Button),
                colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.Primary)
            ) {
                when (phase) {
                    WorkoutPhase.READY -> {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Set $currentSet", fontWeight = FontWeight.SemiBold)
                    }
                    WorkoutPhase.WATCHING -> {
                        Text("Finish Set", fontWeight = FontWeight.SemiBold)
                    }
                    WorkoutPhase.COMPLETING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }
                    else -> {
                        Text("Please wait...", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SET PROGRESS INDICATOR
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SetProgressIndicator(
    currentSet: Int,
    totalSets: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(totalSets) { index ->
            val setNumber = index + 1
            val isCompleted = setNumber < currentSet
            val isCurrent = setNumber == currentSet
            
            Box(
                modifier = Modifier
                    .size(if (isCurrent) 36.dp else 28.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> DesignTokens.Colors.Success
                            isCurrent -> DesignTokens.Colors.Primary
                            else -> DesignTokens.Colors.NeutralLight
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        "$setNumber",
                        color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            
            if (index < totalSets - 1) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(3.dp)
                        .align(Alignment.CenterVertically)
                        .background(
                            if (setNumber < currentSet) DesignTokens.Colors.Success
                            else DesignTokens.Colors.NeutralLight
                        )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SET COMPLETE POPUP
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SetCompletePopup(
    currentSet: Int,
    totalSets: Int,
    onContinue: () -> Unit,
    onFinishLater: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.MD),
            shape = RoundedCornerShape(DesignTokens.Radius.XXL),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(DesignTokens.Spacing.XL),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(DesignTokens.Colors.Success.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = DesignTokens.Colors.Success,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.LG))

                Text(
                    "Set $currentSet Complete!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))

                if (currentSet < totalSets) {
                    Text(
                        "Great work! Ready for set ${currentSet + 1}?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        "Amazing! You've completed all $totalSets sets!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

                // Continue button
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(DesignTokens.Radius.Button),
                    colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.Primary)
                ) {
                    Text(
                        if (currentSet < totalSets) "Continue to Set ${currentSet + 1}" else "Complete Workout",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Finish later option (only if more sets remain)
                if (currentSet < totalSets) {
                    Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))
                    TextButton(onClick = onFinishLater) {
                        Text(
                            "Finish Later",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ALL SETS DONE POPUP
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AllSetsDonePopup(
    exerciseName: String,
    sessionNumber: Int,
    onComplete: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.MD),
            shape = RoundedCornerShape(DesignTokens.Radius.XXL),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(DesignTokens.Spacing.XL),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Celebration icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(DesignTokens.Colors.Success.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(40.dp), tint = DesignTokens.Colors.Success)
                }

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.LG))

                Text(
                    "Workout Complete!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = DesignTokens.Colors.Success
                )

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))

                Text(
                    exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.XS))

                Text(
                    "Session $sessionNumber completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))

                // Complete button
                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(DesignTokens.Radius.Button),
                    colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.Success)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HELPER COMPONENTS
// ═══════════════════════════════════════════════════════════════════════════════

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
              <div>No video available</div>
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
    }.getOrNull()
}
