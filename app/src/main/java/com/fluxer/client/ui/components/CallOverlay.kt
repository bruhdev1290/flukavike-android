package com.fluxer.client.ui.components

import android.telecom.Call
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fluxer.client.data.model.*
import com.fluxer.client.ui.theme.*

@Composable
fun IncomingCallScreen(
    callData: IncomingCallData,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VelvetBlack)
            .padding(32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section - Caller info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 64.dp)
            ) {
                Text(
                    text = if (callData.type == CallType.VIDEO) "VIDEO CALL" else "INCOMING CALL",
                    style = MaterialTheme.typography.labelLarge,
                    color = PhantomRed,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Caller Avatar
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = VelvetSurface,
                    border = androidx.compose.foundation.BorderStroke(4.dp, PhantomRed.copy(alpha = 0.5f))
                ) {
                    if (callData.caller.avatarUrl != null) {
                        AsyncImage(
                            model = callData.caller.avatarUrl,
                            contentDescription = callData.caller.username,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = callData.caller.username.take(1).uppercase(),
                                style = MaterialTheme.typography.displayMedium,
                                color = PhantomRed
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Caller name
                Text(
                    text = callData.caller.displayName ?: callData.caller.username,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                
                // Username
                Text(
                    text = "@${callData.caller.username}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted
                )
                
                // Channel name if applicable
                callData.channelName?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "via $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PhantomRed
                    )
                }
            }
            
            // Call controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Decline Button
                CallControlButton(
                    icon = Icons.Default.CallEnd,
                    label = "Decline",
                    backgroundColor = DndRed,
                    onClick = onDecline
                )
                
                // Accept Button
                CallControlButton(
                    icon = if (callData.type == CallType.VIDEO) Icons.Default.Videocam else Icons.Default.Call,
                    label = "Accept",
                    backgroundColor = SuccessGreen,
                    onClick = onAccept
                )
            }
        }
    }
}

@Composable
fun ActiveCallScreen(
    call: com.fluxer.client.data.model.Call,
    participants: List<CallParticipant>,
    callDuration: Long,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isVideoOn: Boolean,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onVideoToggle: () -> Unit,
    onEndCall: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VelvetBlack)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Call status
        Text(
            text = if (participants.size > 2) "GROUP CALL (${participants.size})" else "VOICE CALL",
            style = MaterialTheme.typography.labelLarge,
            color = PhantomRed,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 48.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Participants display
        if (participants.size == 2) {
            // 1-on-1 call - show large avatar
            val otherParticipant = participants.find { it.user.id != call.initiatorId } ?: participants.first()
            
            Surface(
                modifier = Modifier.size(140.dp),
                shape = CircleShape,
                color = VelvetSurface,
                border = androidx.compose.foundation.BorderStroke(4.dp, OnlineGreen)
            ) {
                if (otherParticipant.user.avatarUrl != null) {
                    AsyncImage(
                        model = otherParticipant.user.avatarUrl,
                        contentDescription = otherParticipant.user.username,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = otherParticipant.user.username.take(1).uppercase(),
                            style = MaterialTheme.typography.displayMedium,
                            color = PhantomRed
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = otherParticipant.user.displayName ?: otherParticipant.user.username,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        } else {
            // Group call - show participant grid
            // TODO: Implement grid for multiple participants
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Call duration
        Text(
            text = formatCallDuration(callDuration),
            style = MaterialTheme.typography.titleLarge,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Call controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Mute
            CallControlButtonSmall(
                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label = if (isMuted) "Unmute" else "Mute",
                isActive = !isMuted,
                onClick = onMuteToggle
            )
            
            // Speaker
            CallControlButtonSmall(
                icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                label = if (isSpeakerOn) "Speaker" else "Earpiece",
                isActive = isSpeakerOn,
                onClick = onSpeakerToggle
            )
            
            // Video
            CallControlButtonSmall(
                icon = if (isVideoOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                label = if (isVideoOn) "Video Off" else "Video On",
                isActive = isVideoOn,
                onClick = onVideoToggle
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // End call button
        Surface(
            onClick = onEndCall,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape),
            color = DndRed,
            shape = CircleShape
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = TextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    backgroundColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = backgroundColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = TextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
    }
}

@Composable
private fun CallControlButtonSmall(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = if (isActive) VelvetSurface else PhantomRed.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) TextPrimary else PhantomRed,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

private fun formatCallDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}
