@file:OptIn(ExperimentalFoundationApi::class)

package com.fluxer.client.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fluxer.client.data.model.Channel
import com.fluxer.client.data.model.User
import com.fluxer.client.data.model.UserStatus
import com.fluxer.client.data.model.displayName
import com.fluxer.client.ui.components.FluxerEmptyState
import com.fluxer.client.ui.components.FluxerIconButton
import com.fluxer.client.ui.components.FluxerLoadingState
import com.fluxer.client.ui.components.FluxerPageScaffold
import com.fluxer.client.ui.components.FluxerPanel
import com.fluxer.client.ui.components.FluxerSectionTitle
import com.fluxer.client.ui.theme.*
import com.fluxer.client.ui.viewmodel.MessagesViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MessagesScreen(
    onBack: () -> Unit,
    onChannelSelected: (Channel) -> Unit,
    onViewProfile: (String) -> Unit = {},
    onStartCall: (String) -> Unit = {},
    viewModel: MessagesViewModel = hiltViewModel()
) {
    val dmChannels by viewModel.dmChannels.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val unreadCountsByChannel by viewModel.unreadCountsByChannel.collectAsState()
    
    // State for context menu
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var showMuteOptions by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadDMChannels()
    }
    
    FluxerPageScaffold(
        title = "Messages",
        subtitle = "${dmChannels.size} conversations",
        onBack = onBack,
        headerActions = {
            FluxerIconButton(
                icon = Icons.Default.Search,
                contentDescription = "Search",
                onClick = { }
            )
            FluxerIconButton(
                icon = Icons.Default.PersonAdd,
                contentDescription = "Add friends",
                onClick = { }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: New DM */ },
                containerColor = PhantomRed,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "New Message",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                FluxerLoadingState("Loading conversations")
            } else if (dmChannels.isEmpty()) {
                FluxerEmptyState(
                    title = "No direct messages yet",
                    body = "When you start a conversation, it will show up here.",
                    icon = Icons.AutoMirrored.Filled.Send
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    item {
                        FluxerSectionTitle(title = "Quick access")
                        PersonalNotesItem(onClick = { /* TODO */ })
                        Spacer(modifier = Modifier.height(12.dp))
                        FluxerSectionTitle(title = "Recent conversations")
                    }
                    
                    items(dmChannels) { dmChannel ->
                        DMChannelItemDiscord(
                            channel = dmChannel,
                            unreadCount = unreadCountsByChannel[dmChannel.id] ?: 0,
                            onClick = { onChannelSelected(dmChannel) },
                            onLongClick = {
                                selectedChannel = dmChannel
                                showContextMenu = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Context Menu Bottom Sheet
    if (showContextMenu && selectedChannel != null) {
        DMContextMenu(
            channel = selectedChannel!!,
            onDismiss = { 
                showContextMenu = false
                selectedChannel = null
            },
            onViewProfile = {
                onViewProfile(selectedChannel!!.id)
                showContextMenu = false
            },
            onStartCall = {
                onStartCall(selectedChannel!!.id)
                showContextMenu = false
            },
            onAddNote = { showContextMenu = false },
            onCloseDM = { 
                viewModel.closeDM(selectedChannel!!.id)
                showContextMenu = false
            },
            onInviteToCommunity = { showContextMenu = false },
            onAddFriend = { showContextMenu = false },
            onBlock = { showContextMenu = false },
            onMuteDM = {
                showContextMenu = false
                showMuteOptions = true
            }
        )
    }
    
    // Mute Options Bottom Sheet
    if (showMuteOptions && selectedChannel != null) {
        MuteOptionsSheet(
            onDismiss = { 
                showMuteOptions = false
                selectedChannel = null
            },
            onMuteSelected = { duration ->
                viewModel.muteDM(selectedChannel!!.id, duration)
                showMuteOptions = false
                selectedChannel = null
            }
        )
    }
}

@Composable
private fun PersonalNotesItem(onClick: () -> Unit) {
    FluxerPanel(
        modifier = Modifier.fillMaxWidth(),
        tonal = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(14.dp),
                color = VelvetMid
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Personal Notes",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Draft thoughts and save things for later",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun DMChannelItemDiscord(
    channel: Channel,
    unreadCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val recipient = remember(channel.id, channel.recipients) {
        channel.recipients.firstOrNull() ?: generateMockRecipient(channel)
    }
    val lastMessage = remember(channel.id, channel.topic, unreadCount) {
        generateMockLastMessage(channel, unreadCount)
    }
    val isSystemUser = remember { recipient.username == "Fluxer" }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (unreadCount > 0) VelvetDark else Color.Transparent, RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = VelvetSurface
            ) {
                if (recipient.avatarUrl != null) {
                    AsyncImage(
                        model = recipient.avatarUrl,
                        contentDescription = recipient.username,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = recipient.username.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
            }
            }
            
            if (!isSystemUser) {
                val statusColor = when (recipient.status) {
                    UserStatus.ONLINE -> OnlineGreen
                    UserStatus.AWAY -> AwayYellow
                    UserStatus.DND -> DndRed
                    UserStatus.OFFLINE -> OfflineGray
                }
                
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomEnd)
                        .background(VelvetBlack, CircleShape)
                        .padding(2.dp)
                        .background(statusColor, CircleShape)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = recipient.displayName ?: recipient.username,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = if (unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (isSystemUser) {
                        Spacer(modifier = Modifier.width(8.dp))
                        SystemBadge()
                    }
                }
                
                if (unreadCount > 0) {
                    Text(
                        text = "$unreadCount new",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val messageColor = if (unreadCount > 0) TextSecondary else TextMuted
                
                Text(
                    text = lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = messageColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                if (unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(containerColor = PhantomRed.copy(alpha = 0.18f), contentColor = PhantomRed) {
                        Text(
                            text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = PhantomRed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemBadge() {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = InfoCyan.copy(alpha = 0.2f)
    ) {
        Text(
            text = "SYSTEM",
            style = MaterialTheme.typography.labelSmall,
            color = InfoCyan,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DMContextMenu(
    channel: Channel,
    onDismiss: () -> Unit,
    onViewProfile: () -> Unit,
    onStartCall: () -> Unit,
    onAddNote: () -> Unit,
    onCloseDM: () -> Unit,
    onInviteToCommunity: () -> Unit,
    onAddFriend: () -> Unit,
    onBlock: () -> Unit,
    onMuteDM: () -> Unit
) {
    val recipient = remember(channel.id, channel.recipients) {
        channel.recipients.firstOrNull() ?: generateMockRecipient(channel)
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VelvetDark,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(TextMuted.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with avatar and name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = VelvetSurface
                ) {
                    if (recipient.avatarUrl != null) {
                        AsyncImage(
                            model = recipient.avatarUrl,
                            contentDescription = recipient.username,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = recipient.username.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineSmall,
                                color = PhantomRed
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = recipient.displayName ?: recipient.username,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Divider(color = BorderSubtle, thickness = 0.5.dp)
            
            // Pin DM section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = VelvetSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                ContextMenuItem(
                    icon = Icons.Default.PushPin,
                    text = "Pin DM",
                    onClick = { /* TODO */ }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Main actions section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = VelvetSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    ContextMenuItem(
                        icon = Icons.Default.Person,
                        text = "View Profile",
                        onClick = onViewProfile
                    )
                    ContextMenuItem(
                        icon = Icons.Default.Call,
                        text = "Start Voice Call",
                        onClick = onStartCall
                    )
                    ContextMenuItem(
                        icon = Icons.Default.Edit,
                        text = "Add Note",
                        onClick = onAddNote
                    )
                    ContextMenuItem(
                        icon = Icons.Default.Close,
                        text = "Close DM",
                        isDestructive = true,
                        onClick = onCloseDM
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Invite/Add Friend section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = VelvetSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    ContextMenuItem(
                        icon = null,
                        text = "Invite to Community",
                        showArrow = true,
                        onClick = onInviteToCommunity
                    )
                    ContextMenuItem(
                        icon = Icons.Default.PersonAdd,
                        text = "Add Friend",
                        onClick = onAddFriend
                    )
                    ContextMenuItem(
                        icon = Icons.Default.Block,
                        text = "Block",
                        isDestructive = true,
                        onClick = onBlock
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mute DM section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = VelvetSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                ContextMenuItem(
                    icon = null,
                    text = "Mute DM",
                    showArrow = true,
                    onClick = onMuteDM
                )
            }
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector?,
    text: String,
    isDestructive: Boolean = false,
    showArrow: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) DndRed else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        } else {
            Spacer(modifier = Modifier.width(40.dp))
        }
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) DndRed else TextPrimary,
            modifier = Modifier.weight(1f)
        )
        
        if (showArrow) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MuteOptionsSheet(
    onDismiss: () -> Unit,
    onMuteSelected: (MuteDuration) -> Unit
) {
    val options = listOf(
        MuteDuration.MINUTES_15 to "For 15 minutes",
        MuteDuration.MINUTES_30 to "For 30 minutes",
        MuteDuration.HOURS_1 to "For 1 hour",
        MuteDuration.HOURS_3 to "For 3 hours",
        MuteDuration.HOURS_4 to "For 4 hours",
        MuteDuration.HOURS_8 to "For 8 hours",
        MuteDuration.UNTIL_MORNING to "Until 8:00 AM",
        MuteDuration.ALWAYS to "Always"
    )
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VelvetDark,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(TextMuted.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with back button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                
                Text(
                    text = "Mute DM",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            Divider(color = BorderSubtle, thickness = 0.5.dp)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mute options
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = VelvetSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    options.forEachIndexed { index, (duration, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMuteSelected(duration) }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        if (index < options.size - 1) {
                            Divider(
                                color = BorderSubtle.copy(alpha = 0.5f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class MuteDuration {
    MINUTES_15,
    MINUTES_30,
    HOURS_1,
    HOURS_3,
    HOURS_4,
    HOURS_8,
    UNTIL_MORNING,
    ALWAYS
}

// Fallbacks while richer DM metadata is still being hydrated from the native cache.
private fun generateMockRecipient(channel: Channel): User {
    val name = channel.name.ifBlank { "Unknown user" }
    return User(
        id = channel.id,
        username = channel.displayName().ifBlank { name }.lowercase().replace(" ", "_"),
        displayName = channel.displayName().ifBlank { name },
        avatarUrl = null,
        status = UserStatus.OFFLINE
    )
}

private fun generateMockLastMessage(channel: Channel, unreadCount: Int): String {
    return channel.topic?.takeIf { it.isNotBlank() }
        ?: if (unreadCount > 0) "New activity in this conversation" else "Open conversation"
}

private fun formatDiscordTimestamp(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val now = Instant.now()
    val diffMinutes = ChronoUnit.MINUTES.between(instant, now)
    val diffHours = ChronoUnit.HOURS.between(instant, now)
    val diffDays = ChronoUnit.DAYS.between(instant, now)
    val diffWeeks = diffDays / 7
    
    return when {
        diffMinutes < 60 -> "${diffMinutes}m"
        diffHours < 24 -> "${diffHours}h"
        diffDays < 7 -> "${diffDays}d"
        diffWeeks < 4 -> "${diffWeeks}w"
        diffDays < 365 -> "${diffDays / 30}mo"
        else -> "${diffDays / 365}y"
    }
}
