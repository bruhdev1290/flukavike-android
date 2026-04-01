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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(72.dp)
            .fillMaxHeight()
            .background(VelvetDark)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Home/DM button
        ServerIcon(
            icon = { 
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Direct Messages",
                    modifier = Modifier.size(28.dp),
                    tint = if (selectedServerId == null) TextPrimary else TextSecondary
                )
            },
            isSelected = selectedServerId == null,
            hasNotification = false,
            onClick = { /* Navigate to DMs */ },
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Divider
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(2.dp)
                .background(BorderSubtle)
                .padding(vertical = 8.dp)
        )
        
        // Server list
        servers.forEach { server ->
            ServerIcon(
                server = server,
                isSelected = server.id == selectedServerId,
                hasNotification = false, // TODO: Check unread messages
                onClick = { onServerSelected(server) },
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        
        // Add server button
        AddServerButton(
            onClick = onAddServer,
            modifier = Modifier.padding(top = 8.dp)
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
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .size(48.dp)
            .scale(scale)
            .clip(RoundedCornerShape(if (isSelected) 16.dp else 50.dp))
            .background(
                if (isSelected) PhantomRed else VelvetSurface,
                RoundedCornerShape(if (isSelected) 16.dp else 50.dp)
            )
            .border(
                width = if (isSelected) 0.dp else 2.dp,
                color = if (isSelected) PhantomRed else BorderSubtle,
                shape = RoundedCornerShape(if (isSelected) 16.dp else 50.dp)
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
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) TextPrimary else TextSecondary
            )
        }
    }
    
    // Selection indicator pill
    if (isSelected) {
        Box(
            modifier = Modifier
                .offset(x = (-36).dp)
                .width(4.dp)
                .height(40.dp)
                .background(PhantomRed, RoundedCornerShape(0.dp, 4.dp, 4.dp, 0.dp))
        )
    }
    
    // Notification dot
    if (hasNotification && !isSelected) {
        Box(
            modifier = Modifier
                .offset((-4).dp, (-4).dp)
                .size(12.dp)
                .background(AlertYellow, RoundedCornerShape(50))
        )
    }
}

@Composable
private fun AddServerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .size(48.dp)
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
            modifier = Modifier.size(24.dp)
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
    Column(
        modifier = modifier
            .width(240.dp)
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
