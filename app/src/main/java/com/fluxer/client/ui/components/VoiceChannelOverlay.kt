package com.fluxer.client.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
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
fun VoiceChannelOverlay(
    channelName: String,
    participants: List<VoiceParticipant>,
    currentUserState: VoiceState?,
    onMuteToggle: () -> Unit,
    onDeafenToggle: () -> Unit,
    onVideoToggle: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(VelvetSurface)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = OnlineGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = channelName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = VelvetLight
                ) {
                    Text(
                        text = "${participants.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            
            IconButton(onClick = onDisconnect) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Disconnect",
                    tint = DndRed,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Participants Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 80.dp),
            modifier = Modifier.heightIn(max = 200.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(participants) { participant ->
                VoiceParticipantItem(
                    participant = participant,
                    isCurrentUser = participant.user.id == currentUserState?.userId
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Controls
        VoiceControls(
            isMuted = currentUserState?.selfMute ?: false,
            isDeafened = currentUserState?.selfDeaf ?: false,
            isVideoOn = currentUserState?.selfVideo ?: false,
            onMuteToggle = onMuteToggle,
            onDeafenToggle = onDeafenToggle,
            onVideoToggle = onVideoToggle
        )
    }
}

@Composable
private fun VoiceParticipantItem(
    participant: VoiceParticipant,
    isCurrentUser: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box {
            // Avatar
            Surface(
                modifier = Modifier
                    .size(64.dp)
                    .border(
                        width = if (participant.voiceState.speaking) 3.dp else 0.dp,
                        color = if (participant.voiceState.speaking) OnlineGreen else Color.Transparent,
                        shape = CircleShape
                    ),
                shape = CircleShape,
                color = VelvetLight
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
                            style = MaterialTheme.typography.titleLarge,
                            color = PhantomRed
                        )
                    }
                }
            }
            
            // Speaking indicator
            if (participant.voiceState.speaking) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                        .background(VelvetBlack, CircleShape)
                        .padding(2.dp)
                        .background(OnlineGreen, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Speaking",
                        tint = VelvetBlack,
                        modifier = Modifier.size(8.dp)
                    )
                }
            }
            
            // Muted indicator
            if (participant.voiceState.selfMute || participant.voiceState.mute) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                        .background(DndRed, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = "Muted",
                        tint = TextPrimary,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = if (isCurrentUser) "You" else (participant.user.displayName ?: participant.user.username),
            style = MaterialTheme.typography.bodySmall,
            color = if (participant.voiceState.speaking) OnlineGreen else TextSecondary,
            maxLines = 1,
            fontWeight = if (participant.voiceState.speaking) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun VoiceControls(
    isMuted: Boolean,
    isDeafened: Boolean,
    isVideoOn: Boolean,
    onMuteToggle: () -> Unit,
    onDeafenToggle: () -> Unit,
    onVideoToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Mute Button
        VoiceControlButton(
            icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
            label = if (isMuted) "Unmute" else "Mute",
            isActive = !isMuted,
            activeColor = PhantomRed,
            onClick = onMuteToggle
        )
        
        // Deafen Button
        VoiceControlButton(
            icon = if (isDeafened) Icons.Default.HeadsetOff else Icons.Default.Headset,
            label = if (isDeafened) "Undeafen" else "Deafen",
            isActive = !isDeafened,
            activeColor = PhantomRed,
            onClick = onDeafenToggle
        )
        
        // Video Button
        VoiceControlButton(
            icon = if (isVideoOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
            label = if (isVideoOn) "Stop Video" else "Start Video",
            isActive = isVideoOn,
            activeColor = OnlineGreen,
            onClick = onVideoToggle
        )
    }
}

@Composable
private fun VoiceControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = if (isActive) VelvetLight else activeColor.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) TextPrimary else activeColor,
                    modifier = Modifier.size(28.dp)
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

@Composable
fun VoiceChannelListItem(
    channel: Channel,
    participantCount: Int,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = null,
            tint = if (isConnected) OnlineGreen else TextMuted,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = channel.name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isConnected) OnlineGreen else TextSecondary,
            modifier = Modifier.weight(1f)
        )
        
        if (participantCount > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = participantCount.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}


