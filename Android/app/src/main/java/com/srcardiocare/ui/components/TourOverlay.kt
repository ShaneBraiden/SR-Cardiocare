// TourOverlay.kt — In-app guided tour overlay with spotlight + tooltip callouts.
//
// Usage (see PatientHomeScreen for an integration example):
//   1. Define steps: `val steps = listOf(TourStep("exercises", "Exercises", "Tap here…"), …)`
//   2. Remember state: `val tour = rememberTourState(steps, active = shouldShowTour)`
//   3. Tag targets with `Modifier.tourTarget(tour, "exercises")`
//   4. Render overlay at the top of your Box: `TourOverlay(tour, onComplete = …, onSkip = …)`
package com.srcardiocare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.srcardiocare.ui.theme.DesignTokens

data class TourStep(
    val key: String,
    val title: String,
    val body: String
)

class TourState internal constructor(val steps: List<TourStep>) {
    var currentIndex by mutableIntStateOf(0)
        private set
    var isActive by mutableStateOf(false)
        internal set
    internal val targets = mutableStateMapOf<String, Rect>()

    val currentStep: TourStep? get() = steps.getOrNull(currentIndex)
    val currentRect: Rect? get() = currentStep?.let { targets[it.key] }
    val isFirst: Boolean get() = currentIndex == 0
    val isLast: Boolean get() = currentIndex == steps.lastIndex

    fun next() {
        if (!isLast) currentIndex++
    }

    fun previous() {
        if (!isFirst) currentIndex--
    }

    internal fun registerTarget(key: String, rect: Rect) {
        targets[key] = rect
    }

    internal fun reset() {
        currentIndex = 0
    }
}

@Composable
fun rememberTourState(steps: List<TourStep>, active: Boolean): TourState {
    val state = remember(steps) { TourState(steps) }
    LaunchedEffect(active) {
        state.isActive = active
        if (active) state.reset()
    }
    return state
}

/** Measures the element's bounds-in-window so the overlay can draw a spotlight around it. */
fun Modifier.tourTarget(state: TourState, key: String): Modifier =
    this.onGloballyPositioned { coords ->
        state.registerTarget(key, coords.boundsInWindow())
    }

@Composable
fun TourOverlay(
    state: TourState,
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    if (!state.isActive) return
    val step = state.currentStep ?: return
    val rect = state.currentRect
    val density = LocalDensity.current
    val padPx = with(density) { 8.dp.toPx() }
    val cornerPx = with(density) { DesignTokens.Radius.LG.toPx() }

    Popup(
        properties = PopupProperties(focusable = true, dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Swallow all touches so the underlying UI is not interactive during the tour.
                .pointerInput(step.key) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
                // graphicsLayer forces an offscreen layer so BlendMode.Clear can punch a hole.
                .graphicsLayer(alpha = 0.99f)
                .drawWithContent {
                    drawContent()
                    drawRect(color = Color.Black.copy(alpha = 0.72f))
                    if (rect != null && rect.width > 0f && rect.height > 0f) {
                        drawRoundRect(
                            color = Color.Transparent,
                            topLeft = Offset(rect.left - padPx, rect.top - padPx),
                            size = Size(rect.width + padPx * 2, rect.height + padPx * 2),
                            cornerRadius = CornerRadius(cornerPx, cornerPx),
                            blendMode = BlendMode.Clear
                        )
                    }
                }
        ) {
            TooltipCallout(
                rect = rect,
                title = step.title,
                body = step.body,
                stepIndex = state.currentIndex,
                stepCount = state.steps.size,
                isFirst = state.isFirst,
                isLast = state.isLast,
                onPrevious = { state.previous() },
                onNext = {
                    if (state.isLast) onComplete() else state.next()
                },
                onSkip = onSkip
            )
        }
    }
}

@Composable
private fun BoxScope.TooltipCallout(
    rect: Rect?,
    title: String,
    body: String,
    stepIndex: Int,
    stepCount: Int,
    isFirst: Boolean,
    isLast: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val density = LocalDensity.current
    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    // Place the card below the target when the target sits in the upper half; otherwise above.
    // When no rect is known yet (first layout pass) center it on screen.
    val verticalAlignment: Alignment = when {
        rect == null -> Alignment.Center
        rect.center.y < screenHeightPx / 2f -> Alignment.BottomCenter
        else -> Alignment.TopCenter
    }

    Card(
        modifier = Modifier
            .align(verticalAlignment)
            .padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.XL)
            .fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.Radius.LG),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(DesignTokens.Spacing.XL)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${stepIndex + 1} / $stepCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(DesignTokens.Spacing.SM))

            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(DesignTokens.Spacing.LG))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSkip) {
                    Text("Skip Tour", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                if (!isFirst) {
                    OutlinedButton(
                        onClick = onPrevious,
                        shape = RoundedCornerShape(DesignTokens.Radius.Button)
                    ) {
                        Text("Previous")
                    }
                    Spacer(Modifier.width(DesignTokens.Spacing.SM))
                }
                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(DesignTokens.Radius.Button),
                    colors = ButtonDefaults.buttonColors(containerColor = DesignTokens.Colors.Primary)
                ) {
                    Text(
                        text = if (isLast) "Finish" else "Next",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
