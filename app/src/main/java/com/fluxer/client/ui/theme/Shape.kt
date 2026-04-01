package com.fluxer.client.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI

/**
 * Fluxer Shapes - Sharp, angular gaming aesthetic
 * Mix of sharp corners and minimal rounding for that edgy look
 */

val FluxerShapes = Shapes(
    // Small elements: buttons, chips, badges - sharp corners
    small = RoundedCornerShape(2.dp),
    // Medium elements: cards, dialogs, text fields - slight rounding
    medium = RoundedCornerShape(4.dp),
    // Large elements: bottom sheets, expanded cards - minimal rounding
    large = RoundedCornerShape(8.dp)
)

// Extended shapes for custom UI
object FluxerExtendedShapes {
    // Completely sharp for buttons
    val sharpButton = RoundedCornerShape(0.dp)
    
    // Slight rounding for input fields
    val inputField = RoundedCornerShape(4.dp)
    
    // Message bubbles
    val messageSent = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
    val messageReceived = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
    
    // Avatar
    val avatar = RoundedCornerShape(50) // Circle
    
    // Panel corners
    val panel = RoundedCornerShape(8.dp, 0.dp, 0.dp, 8.dp)
}

/**
 * Custom polygon shape for sharp, angular elements
 * Inspired by Persona 5 UI elements
 */
class PolygonShape(
    private val sides: Int = 6,
    private val cornerRadius: Dp = 0.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(
            path = createPolygonPath(size, sides, cornerRadius.value)
        )
    }
}

/**
 * Diagonal clip shape for edgy panels
 */
class DiagonalShape(
    private val clipAngle: Float = 15f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(0f, size.height * 0.15f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height * 0.85f)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Jagged/Slash shape for buttons
 */
class SlashShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cutSize = size.width * 0.08f
        val path = Path().apply {
            moveTo(cutSize, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width - cutSize, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

private fun createPolygonPath(size: Size, sides: Int, cornerRadius: Float): Path {
    val path = Path()
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val radius = kotlin.math.min(centerX, centerY)
    val angleStep = (2.0 * PI) / sides.toDouble()
    
    for (i in 0 until sides) {
        val angle = i * angleStep - PI / 2.0
        val x = centerX.toDouble() + radius.toDouble() * kotlin.math.cos(angle)
        val y = centerY.toDouble() + radius.toDouble() * kotlin.math.sin(angle)
        
        if (i == 0) {
            path.moveTo(x.toFloat(), y.toFloat())
        } else {
            path.lineTo(x.toFloat(), y.toFloat())
        }
    }
    path.close()
    return path
}
