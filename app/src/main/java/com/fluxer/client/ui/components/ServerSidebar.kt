package com.fluxer.client.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fluxer.client.data.model.displayName
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.fluxer.client.data.model.Server
import com.fluxer.client.ui.theme.*

/**
 * Server sidebar styled for the calmer Fluxer shell.
 */
@Composable
fun ServerSidebar(
    servers: List<Server>,
    selectedServerId: String?,
    onServerSelected: (Server) -> Unit,
    onHomeSelected: () -> Unit,
    unreadCountsByGuild: Map<String, Int> = emptyMap(),
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val iconSize = if (isCompact) 22.dp else 28.dp
    val serverIconSize = if (isCompact) 40.dp else 48.dp
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(VelvetBlack)
            .border(1.dp, BorderSubtle.copy(alpha = 0.45f))
            .padding(start = if (isCompact) 6.dp else 8.dp)
            .padding(vertical = if (isCompact) 8.dp else 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Home/DM button
        ServerIcon(
            icon = { 
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Direct Messages",
                    modifier = Modifier.size(iconSize),
                    tint = if (selectedServerId == null) TextPrimary else TextSecondary
                )
            },
            isSelected = selectedServerId == null,
            hasNotification = false,
            onClick = onHomeSelected,
            modifier = Modifier.padding(bottom = if (isCompact) 6.dp else 8.dp),
            size = serverIconSize
        )
        
        // Divider
        Box(
            modifier = Modifier
                .width(if (isCompact) 24.dp else 32.dp)
                .height(1.dp)
                .background(BorderSubtle.copy(alpha = 0.8f))
                .padding(vertical = if (isCompact) 6.dp else 8.dp)
        )
        
        // Server list
        servers.forEach { server ->
            ServerIcon(
                server = server,
                isSelected = server.id == selectedServerId,
                hasNotification = (unreadCountsByGuild[server.id] ?: 0) > 0,
                onClick = { onServerSelected(server) },
                modifier = Modifier.padding(vertical = if (isCompact) 3.dp else 4.dp),
                size = serverIconSize
            )
        }
    }
}

@Composable
private fun ServerIcon(
    server: Server? = null,
    icon: @Composable (() -> Unit)? = null,
    isSelected: Boolean,
    hasNotification: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    val interactionSource = remember { MutableInteractionSource() }
    val cornerRadius = if (isSelected) 16.dp else (size / 2)
    
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                if (isSelected) SelectedItem else VelvetDark,
                RoundedCornerShape(cornerRadius)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) PhantomRed.copy(alpha = 0.5f) else BorderSubtle,
                shape = RoundedCornerShape(cornerRadius)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            icon()
        } else if (!server?.iconUrl.isNullOrBlank()) {
            // Load server icon image
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(server?.iconUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = server?.name ?: "Server",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = server?.name?.take(1)?.uppercase() ?: "?",
                            style = if (size >= 48.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                            color = if (isSelected) TextPrimary else TextSecondary
                        )
                    }
                },
                error = {
                    Text(
                        text = server?.name?.take(1)?.uppercase() ?: "?",
                        style = if (size >= 48.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                        color = if (isSelected) TextPrimary else TextSecondary
                    )
                }
            )
        } else {
            Text(
                text = server?.name?.take(1)?.uppercase() ?: "?",
                style = if (size >= 48.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                color = if (isSelected) TextPrimary else TextSecondary
            )
        }
    }
    
    if (hasNotification) {
        Box(
            modifier = Modifier
                .offset((size / 3), -(size / 3))
                .size(if (isSelected) 8.dp else 10.dp)
                .background(if (isSelected) AlertYellow else PhantomRed, RoundedCornerShape(50))
        )
    }
}

/**
 * Channel list for selected server
 */
@Composable
fun ChannelList(
    channels: List<com.fluxer.client.data.model.Channel>,
    selectedChannelId: String?,
    onChannelSelected: (com.fluxer.client.data.model.Channel) -> Unit,
    onNavigateToVoiceChannel: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    ChannelListContent(
        channels = channels,
        selectedChannelId = selectedChannelId,
        onChannelSelected = onChannelSelected,
        onNavigateToVoiceChannel = onNavigateToVoiceChannel,
        modifier = modifier.width(240.dp)
    )
}

/**
 * Channel list content - reusable for both persistent and drawer layouts
 */
@Composable
fun ChannelListContent(
    channels: List<com.fluxer.client.data.model.Channel>,
    selectedChannelId: String?,
    onChannelSelected: (com.fluxer.client.data.model.Channel) -> Unit,
    onNavigateToVoiceChannel: (String) -> Unit = {},
    unreadCountsByChannel: Map<String, Int> = emptyMap(),
    favoriteChannelIds: Set<String> = emptySet(),
    modifier: Modifier = Modifier
) {
    val textChannels = channels.filter { it.type == com.fluxer.client.data.model.ChannelType.TEXT }
    val voiceChannels = channels.filter { it.type == com.fluxer.client.data.model.ChannelType.VOICE }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(VelvetDark)
            .border(1.dp, BorderSubtle.copy(alpha = 0.45f))
            .padding(vertical = 16.dp)
    ) {
        // Text Channels Section
        if (textChannels.isNotEmpty()) {
            Text(
                text = "Text channels",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            textChannels.forEach { channel ->
                ChannelItem(
                    channel = channel,
                    isSelected = channel.id == selectedChannelId,
                    unreadCount = unreadCountsByChannel[channel.id] ?: 0,
                    isFavorite = favoriteChannelIds.contains(channel.id),
                    onClick = { onChannelSelected(channel) }
                )
            }
        }

        // Voice Channels Section
        if (voiceChannels.isNotEmpty()) {
            if (textChannels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = "Voice channels",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            voiceChannels.forEach { channel ->
                ChannelItem(
                    channel = channel,
                    isSelected = false,
                    unreadCount = unreadCountsByChannel[channel.id] ?: 0,
                    isFavorite = favoriteChannelIds.contains(channel.id),
                    onClick = { onNavigateToVoiceChannel(channel.id) }
                )
            }
        }
    }
}

@Composable
private fun ChannelItem(
    channel: com.fluxer.client.data.model.Channel,
    isSelected: Boolean,
    unreadCount: Int,
    isFavorite: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> SelectedItem
        else -> Color.Transparent
    }

    val isVoice = channel.type == com.fluxer.client.data.model.ChannelType.VOICE

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .background(backgroundColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isVoice) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = if (isSelected) PhantomRed else TextMuted,
                modifier = Modifier.padding(end = 8.dp).size(18.dp)
            )
        } else {
            Text(
                text = "#",
                style = FluxerTextStyles.channelName,
                color = if (isSelected) TextPrimary else TextMuted,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Text(
            text = channel.displayName(),
            style = FluxerTextStyles.channelName,
            color = if (isSelected) TextPrimary else TextSecondary,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        if (isFavorite) {
            Text(
                text = "★",
                color = AlertYellow,
                style = FluxerTextStyles.channelName,
                modifier = Modifier.padding(start = 6.dp)
            )
        }

        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isSelected) PhantomRed else VelvetSurface)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary
                )
            }
        }
    }
}
