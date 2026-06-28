package com.fluxer.client.ui.screens

import android.app.Activity
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fluxer.client.data.model.VoiceParticipant
import com.fluxer.client.ui.theme.*
import com.fluxer.client.ui.viewmodel.VoiceChannelViewModel
import com.fluxer.client.util.CdnUrlBuilder
import io.livekit.android.renderer.SurfaceViewRenderer
import io.livekit.android.room.participant.RemoteParticipant
import io.livekit.android.room.track.VideoTrack

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
    val isCameraEnabled by viewModel.isCameraEnabled.collectAsState()
    val isScreenSharing by viewModel.isScreenSharing.collectAsState()
    val localVideoTrack by viewModel.localVideoTrack.collectAsState()

    val context = LocalContext.current

    // Screen share permission launcher
    val screenShareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let { viewModel.startScreenShare(it) }
        }
    }

    LaunchedEffect(channelId) {
        viewModel.joinChannel(channelId)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.leaveChannel() }
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
                                text = when {
                                    isConnected -> "${livekitParticipants.size + 1} participants • Live"
                                    isConnecting -> "Connecting..."
                                    else -> "Disconnected"
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PhantomRed)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Connecting to voice...", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                    }
                }
            } else {
                // Local camera preview strip (shown when camera is on)
                if (isCameraEnabled && localVideoTrack != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(VelvetDark)
                    ) {
                        VideoTrackView(
                            videoTrack = localVideoTrack!!,
                            mirror = true,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            "You (Camera)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(bottomStart = 12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Participants grid
                val allParticipants = combineParticipants(livekitParticipants, participants, speakingParticipants)
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(allParticipants, key = { it.id }) { participant ->
                        when (participant) {
                            is ParticipantItem.Local -> LocalParticipantCard(
                                isMuted = isMuted,
                                isDeafened = isDeafened,
                                isSpeaking = speakingParticipants.isEmpty(),
                                isCameraOn = isCameraEnabled
                            )
                            is ParticipantItem.Remote -> {
                                val remoteVideo = viewModel.getRemoteVideoTrack(participant.remoteParticipant)
                                RemoteParticipantCard(
                                    participant = participant.remoteParticipant,
                                    serverInfo = participant.serverInfo,
                                    isSpeaking = speakingParticipants.contains(participant.remoteParticipant.sid.value),
                                    videoTrack = remoteVideo
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                VoiceChannelControls(
                    isMuted = isMuted,
                    isDeafened = isDeafened,
                    isCameraEnabled = isCameraEnabled,
                    isScreenSharing = isScreenSharing,
                    onMuteToggle = { viewModel.toggleMute() },
                    onDeafenToggle = { viewModel.toggleDeafen() },
                    onCameraToggle = { viewModel.toggleCamera() },
                    onScreenShareToggle = {
                        if (isScreenSharing) {
                            viewModel.stopScreenShare()
                        } else {
                            val mgr = context.getSystemService(android.content.Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                            screenShareLauncher.launch(mgr.createScreenCaptureIntent())
                        }
                    },
                    onDisconnect = {
                        viewModel.leaveChannel()
                        onBack()
                    }
                )
            }
        }
    }
}

// ── Video renderer ──────────────────────────────────────────────────────────

@Composable
fun VideoTrackView(
    videoTrack: VideoTrack,
    mirror: Boolean = false,
    modifier: Modifier = Modifier
) {
    var renderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    AndroidView(
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                try {
                    init(livekit.org.webrtc.EglBase.create().eglBaseContext, null)
                    setEnableHardwareScaler(true)
                    setMirror(mirror)
                } catch (e: Exception) {
                    // EGL init failure — frames won't render but won't crash
                }
                videoTrack.addRenderer(this)
                renderer = this
            }
        },
        modifier = modifier
    )
    DisposableEffect(videoTrack) {
        onDispose {
            renderer?.let {
                videoTrack.removeRenderer(it)
                try { it.release() } catch (_: Exception) {}
            }
        }
    }
}

// ── Participant data model ───────────────────────────────────────────────────

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
    val items = mutableListOf<ParticipantItem>(ParticipantItem.Local())
    livekitParticipants.forEach { lk ->
        val serverInfo = serverParticipants.find { it.user.id == lk.identity?.value }
        items.add(ParticipantItem.Remote(lk, serverInfo))
    }
    return items
}

// ── Participant cards ────────────────────────────────────────────────────────

@Composable
private fun LocalParticipantCard(
    isMuted: Boolean,
    isDeafened: Boolean,
    isSpeaking: Boolean,
    isCameraOn: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
        Box {
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
                    Text("You", style = MaterialTheme.typography.headlineMedium, color = PhantomRed)
                }
            }
            StatusBadge(isMuted = isMuted || isDeafened, isDeafened = isDeafened, isSpeaking = isSpeaking)
            if (isCameraOn) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd)
                        .background(OnlineGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "You",
            style = MaterialTheme.typography.bodySmall,
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
    isSpeaking: Boolean,
    videoTrack: VideoTrack?
) {
    val userName = serverInfo?.user?.displayName
        ?: serverInfo?.user?.username
        ?: participant.identity?.value
        ?: "Unknown"
    val avatarUrl = CdnUrlBuilder.avatarUrlOrDefault(
        cdnBase = null,
        staticCdnBase = null,
        userId = serverInfo?.user?.id ?: "",
        hash = serverInfo?.user?.avatarUrl
    )
    val isMuted = !participant.isMicrophoneEnabled()
    val hasCam = participant.isCameraEnabled()

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
        Box {
            if (hasCam && videoTrack != null) {
                // Show live video feed
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(
                            width = if (isSpeaking) 3.dp else 0.dp,
                            color = if (isSpeaking) OnlineGreen else Color.Transparent,
                            shape = CircleShape
                        )
                        .background(VelvetDark)
                ) {
                    VideoTrackView(videoTrack = videoTrack, modifier = Modifier.fillMaxSize())
                }
            } else {
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
                        AsyncImage(model = avatarUrl, contentDescription = userName, modifier = Modifier.fillMaxSize())
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(userName.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium, color = PhantomRed)
                        }
                    }
                }
            }
            StatusBadge(isMuted = isMuted, isDeafened = false, isSpeaking = isSpeaking)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            userName,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSpeaking) OnlineGreen else TextPrimary,
            fontWeight = if (isSpeaking) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun BoxScope.StatusBadge(isMuted: Boolean, isDeafened: Boolean, isSpeaking: Boolean) {
    when {
        isMuted || isDeafened -> Box(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.BottomEnd)
                .background(DndRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDeafened) Icons.Default.HeadsetOff else Icons.Default.MicOff,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
        isSpeaking -> Box(
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.BottomEnd)
                .background(VelvetBlack, CircleShape)
                .padding(3.dp)
                .background(OnlineGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Mic, contentDescription = null, tint = VelvetBlack, modifier = Modifier.size(12.dp))
        }
    }
}

// ── Controls bar ─────────────────────────────────────────────────────────────

@Composable
private fun VoiceChannelControls(
    isMuted: Boolean,
    isDeafened: Boolean,
    isCameraEnabled: Boolean,
    isScreenSharing: Boolean,
    onMuteToggle: () -> Unit,
    onDeafenToggle: () -> Unit,
    onCameraToggle: () -> Unit,
    onScreenShareToggle: () -> Unit,
    onDisconnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        VoiceControlButton(
            icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
            label = if (isMuted) "Unmute" else "Mute",
            isActive = !isMuted,
            activeColor = PhantomRed,
            onClick = onMuteToggle
        )

        VoiceControlButton(
            icon = if (isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
            label = if (isCameraEnabled) "Cam On" else "Camera",
            isActive = isCameraEnabled,
            activeColor = OnlineGreen,
            onClick = onCameraToggle
        )

        // End call button
        Surface(
            onClick = onDisconnect,
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = DndRed
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CallEnd, contentDescription = "Disconnect", tint = TextPrimary, modifier = Modifier.size(30.dp))
            }
        }

        VoiceControlButton(
            icon = if (isScreenSharing) Icons.Default.StopScreenShare else Icons.Default.ScreenShare,
            label = if (isScreenSharing) "Stop Share" else "Share",
            isActive = isScreenSharing,
            activeColor = OnlineGreen,
            onClick = onScreenShareToggle
        )

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
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = if (isActive) activeColor.copy(alpha = 0.2f) else VelvetSurface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isActive) activeColor else TextMuted,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}
