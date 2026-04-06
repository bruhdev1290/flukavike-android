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
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    // State for context menu
    var selectedChannel by remember { mutableStateOf<Channel?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var showMuteOptions by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadDMChannels()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Messages",
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
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextPrimary
                        )
                    }
                    
                    Surface(
                        onClick = { /* TODO */ },
                        shape = RoundedCornerShape(16.dp),
                        color = VelvetSurface,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add Friends",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VelvetDark,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: New DM */ },
                containerColor = PhantomRed,
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
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    item {
                        PersonalNotesItem(onClick = { /* TODO */ })
                    }
                    
                    items(dmChannels) { dmChannel ->
                        DMChannelItemDiscord(
                            channel = dmChannel,
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = VelvetSurface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = "Personal Notes",
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
    
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun DMChannelItemDiscord(
    channel: Channel,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val recipient = remember { generateMockRecipient(channel) }
    val lastMessage = remember { generateMockLastMessage(channel) }
    val isFromMe = remember { (0..1).random() == 0 }
    val timestamp = remember { System.currentTimeMillis() - (0..2592000000).random() }
    val unreadCount = remember { if ((0..5).random() == 0) (1..5).random() else 0 }
    val isSystemUser = remember { recipient.username == "Fluxer" }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
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
                            color = PhantomRed
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
                        style = MaterialTheme.typography.bodyLarge,
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
                
                Text(
                    text = formatDiscordTimestamp(timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (unreadCount > 0) PhantomRed else TextMuted
                )
            }
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val messagePrefix = if (isFromMe) "You: " else ""
                val messageColor = if (unreadCount > 0) TextSecondary else TextMuted
                
                Text(
                    text = messagePrefix + lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = messageColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                if (unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Badge(containerColor = PhantomRed) {
                        Text(
                            text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
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
    val recipient = remember { generateMockRecipient(channel) }
    
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

// Mock data generators
private fun generateMockRecipient(channel: Channel): User {
    val names = listOf(
        "Elias" to "#3b82f6",
        "MON7Y5" to "#22c55e", 
        "Ashwood" to TextSecondary,
        "Salim" to TextSecondary,
        "Ferret" to TextSecondary,
        "Fluxer" to "#a855f7",
        "Hampus" to TextSecondary,
        "meowergirl" to TextSecondary,
        "Mr. Cake Slayer" to TextSecondary
    )
    val (name, _) = names.random()
    return User(
        id = channel.id,
        username = name.lowercase().replace(" ", "_"),
        displayName = name,
        avatarUrl = null,
        status = UserStatus.entries.toTypedArray().random()
    )
}

private fun generateMockLastMessage(channel: Channel): String {
    val messages = listOf(
        "ok gn",
        "don't have it set to automatic yet",
        "ah ok",
        "Kinda want to enforce squash commits for pr's",
        "why not, in dont see any issue here",
        "that was a joke",
        "how would something go wrong?",
        "Ah haha",
        "ok set",
        "3 Members",
        "<https://app.deel.com/people...>",
        "👍"
    )
    return messages.random()
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


