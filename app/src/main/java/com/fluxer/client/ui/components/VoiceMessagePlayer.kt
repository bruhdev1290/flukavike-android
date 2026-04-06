package com.fluxer.client.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.fluxer.client.ui.theme.*

@Composable
fun VoiceMessagePlayer(
    durationSeconds: Int,
    isPlaying: Boolean = false,
    progress: Float = 0f,
    onPlayPause: () -> Unit = {},
    onSeek: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(PhantomRed.copy(alpha = 0.3f), VelvetLight)
                )
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play/Pause button
        Surface(
            onClick = onPlayPause,
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = PhantomRed
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Waveform visualization
        VoiceWaveform(
            isPlaying = isPlaying,
            modifier = Modifier.weight(1f)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Duration
        Text(
            text = formatDuration((durationSeconds * (1 - progress)).toInt()),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun VoiceWaveform(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    
    Row(
        modifier = modifier.height(30.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(20) { index ->
            val animation by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 500 + (index * 50),
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )
            
            val heightFraction = if (isPlaying) animation else 0.3f + (index % 3) * 0.2f
            
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(heightFraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (isPlaying) PhantomRed.copy(alpha = 0.8f) else TextMuted.copy(alpha = 0.5f)
                    )
            )
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}
