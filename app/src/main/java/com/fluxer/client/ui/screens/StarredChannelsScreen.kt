package com.fluxer.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fluxer.client.data.model.Channel
import com.fluxer.client.data.model.ChannelType
import com.fluxer.client.data.model.Server
import com.fluxer.client.ui.theme.*
import com.fluxer.client.ui.viewmodel.StarredChannelsViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarredChannelsScreen(
    onBack: () -> Unit,
    onChannelSelected: (Channel, Server) -> Unit,
    viewModel: StarredChannelsViewModel = hiltViewModel()
) {
    val starredChannels by viewModel.starredChannels.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadStarredChannels()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Starred",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Edit button to reorder/remove
                    IconButton(onClick = { /* TODO: Enter edit mode */ }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
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
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PhantomRed)
                }
            } else if (starredChannels.isEmpty()) {
                EmptyStarredState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Group by server for organization
                    val grouped = starredChannels.groupBy { it.server }
                    
                    grouped.forEach { (server, channels) ->
                        item {
                            ServerHeader(server = server)
                        }
                        
                        items(channels) { starredChannel ->
                            StarredChannelItem(
                                channel = starredChannel.channel,
                                server = starredChannel.server,
                                lastMessage = starredChannel.lastMessage,
                                timestamp = starredChannel.lastMessageTime,
                                unreadCount = starredChannel.unreadCount,
                                onClick = { onChannelSelected(starredChannel.channel, starredChannel.server) },
                                onUnstar = { viewModel.unstarChannel(starredChannel.channel.id) }
                            )
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyStarredState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = TextMuted.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No Starred Channels",
            style = MaterialTheme.typography.titleMedium,
            color = TextMuted,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Star channels you use frequently to access them quickly",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ServerHeader(server: Server) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(24.dp),
            shape = RoundedCornerShape(6.dp),
            color = VelvetSurface
        ) {
            if (server.iconUrl != null) {
                AsyncImage(
                    model = server.iconUrl,
                    contentDescription = server.name,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = server.name.take(1).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = PhantomRed
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = server.name,
            style = MaterialTheme.typography.labelLarge,
            color = TextMuted,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StarredChannelItem(
    channel: Channel,
    server: Server,
    lastMessage: String?,
    timestamp: Long?,
    unreadCount: Int,
    onClick: () -> Unit,
    onUnstar: () -> Unit
) {
    var showUnstar by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (unreadCount > 0) VelvetSurface else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel icon
            Icon(
                imageVector = if (channel.type == ChannelType.VOICE) 
                    Icons.Default.VolumeUp else Icons.Default.Tag,
                contentDescription = null,
                tint = if (unreadCount > 0) TextPrimary else TextMuted,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Channel info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (unreadCount > 0) TextPrimary else TextSecondary,
                    fontWeight = if (unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (lastMessage != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = lastMessage.take(40) + if (lastMessage.length > 40) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        timestamp?.let {
                            Text(
                                text = formatTimestamp(it),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (unreadCount > 0) PhantomRed else TextMuted,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
            
            // Unstar button (shown on hover/long press)
            if (showUnstar) {
                IconButton(
                    onClick = onUnstar,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Unstar",
                        tint = AlertYellow,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                // Star indicator
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Starred",
                    tint = AlertYellow.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
            }
            
            // Unread badge
            if (unreadCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Badge(
                    containerColor = PhantomRed
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
}

private fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val now = Instant.now()
    val diffMinutes = ChronoUnit.MINUTES.between(instant, now)
    val diffHours = ChronoUnit.HOURS.between(instant, now)
    val diffDays = ChronoUnit.DAYS.between(instant, now)
    
    return when {
        diffMinutes < 60 -> "${diffMinutes}m"
        diffHours < 24 -> "${diffHours}h"
        diffDays < 7 -> "${diffDays}d"
        else -> {
            val formatter = DateTimeFormatter.ofPattern("MM/dd")
                .withZone(ZoneId.systemDefault())
            formatter.format(instant)
        }
    }
}

// Data class for starred channel with metadata
data class StarredChannel(
    val channel: Channel,
    val server: Server,
    val lastMessage: String? = null,
    val lastMessageTime: Long? = null,
    val unreadCount: Int = 0
)
