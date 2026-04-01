// InAppPopup.kt — Center modal popup component for in-app notifications
package com.srcardiocare.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.srcardiocare.ui.theme.DesignTokens

enum class PopupType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

data class PopupAction(
    val text: String,
    val onClick: () -> Unit,
    val isPrimary: Boolean = false
)

/**
 * In-app center modal popup for displaying notifications and alerts
 */
@Composable
fun InAppPopup(
    visible: Boolean,
    onDismiss: () -> Unit,
    type: PopupType = PopupType.INFO,
    title: String,
    message: String,
    primaryAction: PopupAction? = null,
    secondaryAction: PopupAction? = null,
    dismissOnTapOutside: Boolean = true,
    showCloseButton: Boolean = true
) {
    if (!visible) return
    
    Dialog(
        onDismissRequest = { if (dismissOnTapOutside) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = dismissOnTapOutside,
            usePlatformDefaultWidth = false
        )
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.9f, animationSpec = tween(200)),
            exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.9f, animationSpec = tween(150))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { if (dismissOnTapOutside) onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .widthIn(max = 340.dp)
                        .padding(horizontal = DesignTokens.Spacing.XL)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* Block clicks */ },
                    shape = RoundedCornerShape(DesignTokens.Radius.XXL),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(DesignTokens.Spacing.XL),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Close button
                        if (showCloseButton) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
                        }
                        
                        // Icon
                        PopupIcon(type = type)
                        
                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.LG))
                        
                        // Title
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))
                        
                        // Message
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.XL))
                        
                        // Actions
                        if (primaryAction != null || secondaryAction != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.MD)
                            ) {
                                secondaryAction?.let { action ->
                                    OutlinedButton(
                                        onClick = {
                                            action.onClick()
                                            onDismiss()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(DesignTokens.Radius.Button),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = DesignTokens.Colors.Primary
                                        )
                                    ) {
                                        Text(action.text, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                
                                primaryAction?.let { action ->
                                    Button(
                                        onClick = {
                                            action.onClick()
                                            onDismiss()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(DesignTokens.Radius.Button),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = getTypeColor(type)
                                        )
                                    ) {
                                        Text(action.text, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
                    }
                }
            }
        }
    }
}

@Composable
private fun PopupIcon(type: PopupType) {
    val (icon, color) = when (type) {
        PopupType.SUCCESS -> Icons.Default.Check to DesignTokens.Colors.Success
        PopupType.ERROR -> Icons.Default.Close to DesignTokens.Colors.Error
        PopupType.WARNING -> Icons.Default.Warning to DesignTokens.Colors.Warning
        PopupType.INFO -> Icons.Default.Info to DesignTokens.Colors.Primary
    }
    
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
    }
}

private fun getTypeColor(type: PopupType): Color {
    return when (type) {
        PopupType.SUCCESS -> DesignTokens.Colors.Success
        PopupType.ERROR -> DesignTokens.Colors.Error
        PopupType.WARNING -> DesignTokens.Colors.Warning
        PopupType.INFO -> DesignTokens.Colors.Primary
    }
}

/**
 * Simple popup controller for managing popup state
 */
class PopupController {
    var isVisible by mutableStateOf(false)
        private set
    
    var type by mutableStateOf(PopupType.INFO)
        private set
    
    var title by mutableStateOf("")
        private set
    
    var message by mutableStateOf("")
        private set
    
    var primaryAction: PopupAction? by mutableStateOf(null)
        private set
    
    var secondaryAction: PopupAction? by mutableStateOf(null)
        private set
    
    fun show(
        type: PopupType = PopupType.INFO,
        title: String,
        message: String,
        primaryAction: PopupAction? = null,
        secondaryAction: PopupAction? = null
    ) {
        this.type = type
        this.title = title
        this.message = message
        this.primaryAction = primaryAction
        this.secondaryAction = secondaryAction
        isVisible = true
    }
    
    fun showSuccess(title: String, message: String, onOk: () -> Unit = {}) {
        show(
            type = PopupType.SUCCESS,
            title = title,
            message = message,
            primaryAction = PopupAction("OK", onOk, true)
        )
    }
    
    fun showError(title: String, message: String, onOk: () -> Unit = {}) {
        show(
            type = PopupType.ERROR,
            title = title,
            message = message,
            primaryAction = PopupAction("OK", onOk, true)
        )
    }
    
    fun showWarning(
        title: String,
        message: String,
        onConfirm: () -> Unit,
        onCancel: () -> Unit = {}
    ) {
        show(
            type = PopupType.WARNING,
            title = title,
            message = message,
            primaryAction = PopupAction("Confirm", onConfirm, true),
            secondaryAction = PopupAction("Cancel", onCancel)
        )
    }
    
    fun showInfo(title: String, message: String, onOk: () -> Unit = {}) {
        show(
            type = PopupType.INFO,
            title = title,
            message = message,
            primaryAction = PopupAction("Got it", onOk, true)
        )
    }
    
    fun dismiss() {
        isVisible = false
    }
}

@Composable
fun rememberPopupController(): PopupController {
    return remember { PopupController() }
}
