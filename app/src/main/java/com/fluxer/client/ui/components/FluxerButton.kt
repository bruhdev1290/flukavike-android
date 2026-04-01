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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fluxer.client.ui.theme.*

/**
 * Sharp, angular button with Persona 5-inspired styling
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
        ButtonVariant.Primary -> ButtonColors(
            container = PhantomRed,
            content = TextPrimary,
            border = PhantomRed
        )
        ButtonVariant.Secondary -> ButtonColors(
            container = Color.Transparent,
            content = PhantomRed,
            border = PhantomRed
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
            .scale(scale)
            .clip(RectangleShape),
        color = colors.container.copy(alpha = alpha),
        border = if (variant != ButtonVariant.Ghost) {
            BorderStroke(2.dp, colors.border.copy(alpha = alpha))
        } else null,
        shadowElevation = if (isPressed) 0.dp else 8.dp,
        shape = RectangleShape
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
                text = text.uppercase(),
                style = fontSize.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = colors.content.copy(alpha = alpha),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Button with diagonal slash edges for extra flair
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
 * Loading button with animated effect
 */
@Composable
fun LoadingButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RectangleShape)
            .background(PhantomRed)
            .clickable(enabled = !isLoading, onClick = onClick),
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
                text = text.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ),
                color = TextPrimary
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
