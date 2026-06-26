package com.fluxer.client.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fluxer.client.ui.theme.*

/**
 * Premium button styling shared across the redesigned app.
 */
@Composable
fun FluxerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ButtonVariant = ButtonVariant.Primary,
    size: ButtonSize = ButtonSize.Medium
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    val colors = when (variant) {
        ButtonVariant.Primary -> ButtonColors(PhantomRed, TextPrimary, PhantomRed)
        ButtonVariant.Secondary -> ButtonColors(
            container = VelvetMid,
            content = TextPrimary,
            border = BorderSubtle
        )
        ButtonVariant.Ghost -> ButtonColors(
            container = Color.Transparent,
            content = TextSecondary,
            border = Color.Transparent
        )
        ButtonVariant.Danger -> ButtonColors(
            container = DndRed,
            content = TextPrimary,
            border = DndRed
        )
    }
    
    val (height, fontSize, horizontalPadding) = when (size) {
        ButtonSize.Small -> Triple(36.dp, MaterialTheme.typography.labelMedium, 16.dp)
        ButtonSize.Medium -> Triple(48.dp, MaterialTheme.typography.labelLarge, 24.dp)
        ButtonSize.Large -> Triple(56.dp, MaterialTheme.typography.titleSmall, 32.dp)
    }
    
    val alpha = if (enabled) 1f else 0.5f
    
    Surface(
        modifier = modifier
            .height(height)
            .scale(scale),
        color = colors.container.copy(alpha = alpha),
        border = if (variant != ButtonVariant.Ghost) BorderStroke(1.dp, colors.border.copy(alpha = alpha)) else null,
        shadowElevation = if (variant == ButtonVariant.Primary && !isPressed) 4.dp else 0.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                )
                .padding(horizontal = horizontalPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = fontSize.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                ),
                color = colors.content.copy(alpha = alpha),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Legacy decorative button preserved for places that still opt into it.
 */
@Composable
fun SlashButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Box(
        modifier = modifier
            .height(48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .drawBehind {
                val cutSize = size.width * 0.05f
                
                // Draw shadow
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(cutSize + 4, 4f)
                        lineTo(size.width + 4, 4f)
                        lineTo(size.width - cutSize + 4, size.height + 4)
                        lineTo(4f, size.height + 4)
                        close()
                    },
                    color = ShadowHeavy
                )
                
                // Draw button
                drawPath(
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(cutSize, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width - cutSize, size.height)
                        lineTo(0f, size.height)
                        close()
                    },
                    color = if (isPressed) PhantomRedDark else PhantomRed
                )
            }
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                letterSpacing = 1.5.sp
            ),
            color = TextPrimary
        )
    }
}

/**
 * Loading button aligned with the calmer premium system.
 */
@Composable
fun LoadingButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val alpha = when {
        isLoading -> 1f
        !enabled -> 0.5f
        else -> 1f
    }
    Box(
        modifier = modifier
            .height(48.dp)
            .background(PhantomRed.copy(alpha = alpha), RoundedCornerShape(12.dp))
            .clickable(enabled = enabled && !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = TextPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                ),
                color = TextPrimary.copy(alpha = alpha)
            )
        }
    }
}

enum class ButtonVariant {
    Primary, Secondary, Ghost, Danger
}

enum class ButtonSize {
    Small, Medium, Large
}

private data class ButtonColors(
    val container: Color,
    val content: Color,
    val border: Color
)
