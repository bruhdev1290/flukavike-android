package com.fluxer.client.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fluxer.client.data.model.Server
import com.fluxer.client.ui.theme.*

/**
 * Server sidebar with sharp gaming aesthetic
 */
@Composable
fun ServerSidebar(
    servers: List<Server>,
    selectedServerId: String?,
    onServerSelected: (Server) -> Unit,
    onAddServer: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val iconSize = if (isCompact) 22.dp else 28.dp
    val serverIconSize = if (isCompact) 40.dp else 48.dp
    
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(VelvetDark)
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
            onClick = { /* Navigate to DMs */ },
            modifier = Modifier.padding(bottom = if (isCompact) 6.dp else 8.dp),
            size = serverIconSize
        )
        
        // Divider
        Box(
            modifier = Modifier
                .width(if (isCompact) 24.dp else 32.dp)
                .height(2.dp)
                .background(BorderSubtle)
                .padding(vertical = if (isCompact) 6.dp else 8.dp)
        )
        
        // Server list
        servers.forEach { server ->
            ServerIcon(
                server = server,
                isSelected = server.id == selectedServerId,
                hasNotification = false, // TODO: Check unread messages
                onClick = { onServerSelected(server) },
                modifier = Modifier.padding(vertical = if (isCompact) 3.dp else 4.dp),
                size = serverIconSize
            )
        }
        
        // Add server button
        AddServerButton(
            onClick = onAddServer,
            modifier = Modifier.padding(top = if (isCompact) 6.dp else 8.dp),
            size = serverIconSize
        )
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
    val cornerRadius = if (isSelected) (size * 0.33f) else (size / 2)
    val indicatorOffset = -(size * 0.75f)
    val indicatorHeight = size * 0.83f
    
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                if (isSelected) PhantomRed else VelvetSurface,
                RoundedCornerShape(cornerRadius)
            )
            .border(
                width = if (isSelected) 0.dp else 2.dp,
                color = if (isSelected) PhantomRed else BorderSubtle,
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
        } else {
            Text(
                text = server?.name?.take(1)?.uppercase() ?: "?",
                style = if (size >= 48.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                color = if (isSelected) TextPrimary else TextSecondary
            )
        }
    }
    
    // Selection indicator pill
    if (isSelected) {
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(4.dp)
                .height(indicatorHeight)
                .background(PhantomRed, RoundedCornerShape(0.dp, 4.dp, 4.dp, 0.dp))
        )
    }
    
    // Notification dot
    if (hasNotification && !isSelected) {
        Box(
            modifier = Modifier
                .offset((-size/12), (-size/12))
                .size(size / 4)
                .background(AlertYellow, RoundedCornerShape(50))
        )
    }
}

@Composable
private fun AddServerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val iconSize = size / 2
    
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(50.dp))
            .background(Color.Transparent)
            .border(2.dp, SuccessGreen, RoundedCornerShape(50.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Server",
            tint = SuccessGreen,
            modifier = Modifier.size(iconSize)
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
    modifier: Modifier = Modifier
) {
    ChannelListContent(
        channels = channels,
        selectedChannelId = selectedChannelId,
        onChannelSelected = onChannelSelected,
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(VelvetMid)
            .padding(vertical = 16.dp)
    ) {
        // Server header
        Text(
            text = "TEXT CHANNELS",
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Channel items
        channels.filter { it.type == com.fluxer.client.data.model.ChannelType.TEXT }
            .forEach { channel ->
                ChannelItem(
                    channel = channel,
                    isSelected = channel.id == selectedChannelId,
                    onClick = { onChannelSelected(channel) }
                )
            }
    }
}

@Composable
private fun ChannelItem(
    channel: com.fluxer.client.data.model.Channel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> SelectedItem
        else -> Color.Transparent
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#",
            style = FluxerTextStyles.channelName,
            color = if (isSelected) PhantomRed else TextMuted,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = channel.name,
            style = FluxerTextStyles.channelName,
            color = if (isSelected) TextPrimary else TextSecondary,
            maxLines = 1
        )
    }
}
