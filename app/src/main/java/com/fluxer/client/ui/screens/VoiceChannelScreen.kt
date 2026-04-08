package com.fluxer.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fluxer.client.data.model.VoiceParticipant
import com.fluxer.client.ui.theme.*
import com.fluxer.client.ui.viewmodel.VoiceChannelViewModel
import io.livekit.android.room.participant.RemoteParticipant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceChannelScreen(
    channelId: String,
    onBack: () -> Unit,
    viewModel: VoiceChannelViewModel = hiltViewModel()
) {
    val participants by viewModel.participants.collectAsState()
    val livekitParticipants by viewModel.livekitParticipants.collectAsState()
    val speakingParticipants by viewModel.speakingParticipants.collectAsState()
    val channelInfo by viewModel.channelInfo.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isDeafened by viewModel.isDeafened.collectAsState()
    
    LaunchedEffect(channelId) {
        viewModel.joinChannel(channelId)
    }
    
    DisposableEffect(Unit) {
        onDispose {
            viewModel.leaveChannel()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = channelInfo?.name ?: "Voice Channel",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Connection status dot
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (isConnected) OnlineGreen else WarningOrange,
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isConnected) {
                                    "${livekitParticipants.size + 1} participants • Live"
                                } else if (isConnecting) {
                                    "Connecting..."
                                } else {
                                    "Disconnected"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.leaveChannel()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VelvetDark,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = VelvetBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (isConnecting) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PhantomRed)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Connecting to voice...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                // Participants Grid - Combine LiveKit and server participants
                val allParticipants = combineParticipants(
                    livekitParticipants = livekitParticipants,
                    serverParticipants = participants,
                    speakingParticipants = speakingParticipants
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(allParticipants, key = { it.id }) { participant ->
                        when (participant) {
                            is ParticipantItem.Local -> {
                                LocalParticipantCard(
                                    isMuted = isMuted,
                                    isDeafened = isDeafened,
                                    isSpeaking = speakingParticipants.isEmpty()
                                )
                            }
                            is ParticipantItem.Remote -> {
                                RemoteParticipantCard(
                                    participant = participant.remoteParticipant,
                                    serverInfo = participant.serverInfo,
                                    isSpeaking = speakingParticipants.contains(participant.remoteParticipant.sid.value)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Voice Controls
                VoiceChannelControls(
                    isMuted = isMuted,
                    isDeafened = isDeafened,
                    onMuteToggle = { viewModel.toggleMute() },
                    onDeafenToggle = { viewModel.toggleDeafen() },
                    onDisconnect = {
                        viewModel.leaveChannel()
                        onBack()
                    }
                )
            }
        }
    }
}

// Sealed class to represent different participant types
private sealed class ParticipantItem(val id: String) {
    class Local : ParticipantItem("local")
    class Remote(
        val remoteParticipant: RemoteParticipant,
        val serverInfo: VoiceParticipant?
    ) : ParticipantItem(remoteParticipant.sid.value)
}

private fun combineParticipants(
    livekitParticipants: List<RemoteParticipant>,
    serverParticipants: List<VoiceParticipant>,
    speakingParticipants: Set<String>
): List<ParticipantItem> {
    val items = mutableListOf<ParticipantItem>()
    
    // Add local participant first
    items.add(ParticipantItem.Local())
    
    // Add remote participants
    livekitParticipants.forEach { livekitParticipant ->
        val serverInfo = serverParticipants.find { 
            it.user.id == livekitParticipant.identity?.value 
        }
        items.add(ParticipantItem.Remote(livekitParticipant, serverInfo))
    }
    
    return items
}

@Composable
private fun LocalParticipantCard(
    isMuted: Boolean,
    isDeafened: Boolean,
    isSpeaking: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Box {
            // Avatar
            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .border(
                        width = if (isSpeaking) 3.dp else 0.dp,
                        color = if (isSpeaking) OnlineGreen else Color.Transparent,
                        shape = CircleShape
                    ),
                shape = CircleShape,
                color = PhantomRed.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "You",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PhantomRed
                    )
                }
            }
            
            // Status indicators
            when {
                isMuted || isDeafened -> {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.BottomEnd)
                            .background(DndRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDeafened) Icons.Default.HeadsetOff else Icons.Default.MicOff,
                            contentDescription = if (isDeafened) "Deafened" else "Muted",
                            tint = TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                isSpeaking -> {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.BottomEnd)
                            .background(VelvetBlack, CircleShape)
                            .padding(3.dp)
                            .background(OnlineGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Speaking",
                            tint = VelvetBlack,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "You",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSpeaking) OnlineGreen else TextPrimary,
            fontWeight = if (isSpeaking) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun RemoteParticipantCard(
    participant: RemoteParticipant,
    serverInfo: VoiceParticipant?,
    isSpeaking: Boolean
) {
    val userName = serverInfo?.user?.displayName 
        ?: serverInfo?.user?.username 
        ?: participant.identity?.value
        ?: "Unknown"
    val avatarUrl = serverInfo?.user?.avatarUrl
    val isMuted = !participant.isMicrophoneEnabled()
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Box {
            // Avatar
            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .border(
                        width = if (isSpeaking) 3.dp else 0.dp,
                        color = if (isSpeaking) OnlineGreen else Color.Transparent,
                        shape = CircleShape
                    ),
                shape = CircleShape,
                color = VelvetSurface
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = userName,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = userName.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = PhantomRed
                        )
                    }
                }
            }
            
            // Status indicators
            when {
                isMuted -> {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.BottomEnd)
                            .background(DndRed, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MicOff,
                            contentDescription = "Muted",
                            tint = TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                isSpeaking -> {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.BottomEnd)
                            .background(VelvetBlack, CircleShape)
                            .padding(3.dp)
                            .background(OnlineGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Speaking",
                            tint = VelvetBlack,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = userName,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSpeaking) OnlineGreen else TextPrimary,
            fontWeight = if (isSpeaking) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun VoiceChannelControls(
    isMuted: Boolean,
    isDeafened: Boolean,
    onMuteToggle: () -> Unit,
    onDeafenToggle: () -> Unit,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mute Button
        VoiceControlButton(
            icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
            label = if (isMuted) "Unmute" else "Mute",
            isActive = !isMuted,
            activeColor = PhantomRed,
            onClick = onMuteToggle
        )
        
        // Disconnect Button
        Surface(
            onClick = onDisconnect,
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = DndRed
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Disconnect",
                    tint = TextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        // Deafen Button
        VoiceControlButton(
            icon = if (isDeafened) Icons.Default.HeadsetOff else Icons.Default.Headset,
            label = if (isDeafened) "Undeafen" else "Deafen",
            isActive = !isDeafened,
            activeColor = PhantomRed,
            onClick = onDeafenToggle
        )
    }
}

@Composable
private fun VoiceControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = if (isActive) VelvetSurface else activeColor.copy(alpha = 0.2f)
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
