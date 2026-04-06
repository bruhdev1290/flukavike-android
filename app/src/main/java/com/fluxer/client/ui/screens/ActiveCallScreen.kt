package com.fluxer.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fluxer.client.data.model.CallType
import com.fluxer.client.ui.theme.*
import com.fluxer.client.ui.viewmodel.CallViewModel

@Composable
fun ActiveCallScreen(
    callId: String,
    onEndCall: () -> Unit,
    viewModel: CallViewModel = hiltViewModel()
) {
    val call by viewModel.call.collectAsState()
    val callDuration by viewModel.callDuration.collectAsState()
    val participants by viewModel.participants.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val isVideoOn by viewModel.isVideoOn.collectAsState()
    
    LaunchedEffect(callId) {
        viewModel.joinCall(callId)
    }
    
    DisposableEffect(Unit) {
        onDispose {
            viewModel.endCall()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VelvetBlack)
    ) {
        if (call?.type == CallType.VIDEO && isVideoOn) {
            // Video grid would go here
            VideoGrid(participants = participants)
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            
            // Call type indicator
            Text(
                text = if (participants.size > 2) "GROUP CALL (${participants.size})" 
                       else if (call?.type == CallType.VIDEO) "VIDEO CALL" 
                       else "VOICE CALL",
                style = MaterialTheme.typography.labelLarge,
                color = PhantomRed,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Main participant (or grid for group calls)
            if (participants.size <= 2) {
                val mainParticipant = participants.firstOrNull { it.user.id != call?.initiatorId } 
                    ?: participants.firstOrNull()
                
                mainParticipant?.let { participant ->
                    Surface(
                        modifier = Modifier.size(140.dp),
                        shape = CircleShape,
                        color = VelvetSurface,
                        border = androidx.compose.foundation.BorderStroke(4.dp, OnlineGreen)
                    ) {
                        if (participant.user.avatarUrl != null) {
                            AsyncImage(
                                model = participant.user.avatarUrl,
                                contentDescription = participant.user.username,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = participant.user.username.take(1).uppercase(),
                                    style = MaterialTheme.typography.displayMedium,
                                    color = PhantomRed
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = participant.user.displayName ?: participant.user.username,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Call duration
            Text(
                text = formatDuration(callDuration),
                style = MaterialTheme.typography.titleLarge,
                color = TextSecondary,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Call controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Mute
                CallControlButton(
                    icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label = if (isMuted) "Unmute" else "Mute",
                    isActive = !isMuted,
                    onClick = { viewModel.toggleMute() }
                )
                
                // Video (only for video calls)
                if (call?.type == CallType.VIDEO) {
                    CallControlButton(
                        icon = if (isVideoOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        label = if (isVideoOn) "Video Off" else "Video On",
                        isActive = isVideoOn,
                        onClick = { viewModel.toggleVideo() }
                    )
                }
                
                // Speaker
                CallControlButton(
                    icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                    label = if (isSpeakerOn) "Speaker" else "Earpiece",
                    isActive = isSpeakerOn,
                    onClick = { viewModel.toggleSpeaker() }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // End call button
            Surface(
                onClick = {
                    viewModel.endCall()
                    onEndCall()
                },
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = DndRed
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
}

@Composable
private fun VideoGrid(participants: List<com.fluxer.client.data.model.CallParticipant>) {
    // Placeholder for video grid
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VelvetDark)
    ) {
        Text(
            text = "Video Grid",
            modifier = Modifier.align(Alignment.Center),
            color = TextMuted
        )
    }
}

@Composable
private fun CallControlButton(
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

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}
