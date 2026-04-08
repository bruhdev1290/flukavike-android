package com.fluxer.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fluxer.client.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    onBack: () -> Unit,
    onNotificationClick: (NotificationItem) -> Unit = {}
) {
    // Sample notifications - in real app this would come from a database
    val notifications = remember { generateSampleNotifications() }
    var selectedFilter by remember { mutableStateOf(NotificationFilter.ALL) }
    
    val filteredNotifications = when (selectedFilter) {
        NotificationFilter.ALL -> notifications
        NotificationFilter.MESSAGES -> notifications.filter { it.type == NotificationType.MESSAGE }
        NotificationFilter.MENTIONS -> notifications.filter { it.type == NotificationType.MENTION }
        NotificationFilter.CALLS -> notifications.filter { it.type == NotificationType.CALL }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.titleLarge,
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
                    IconButton(onClick = { /* Mark all as read */ }) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Mark all read",
                            tint = TextSecondary
                        )
                    }
                    IconButton(onClick = { /* Clear all */ }) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear all",
                            tint = PhantomRed
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
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NotificationFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PhantomRed,
                            selectedLabelColor = TextPrimary,
                            containerColor = VelvetSurface,
                            labelColor = TextSecondary
                        )
                    )
                }
            }
            
            // Notifications list
            if (filteredNotifications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No notifications",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredNotifications,
                        key = { it.id }
                    ) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = { onNotificationClick(notification) },
                            onDismiss = { /* Dismiss notification */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationItem,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val icon = when (notification.type) {
        NotificationType.MESSAGE -> Icons.AutoMirrored.Filled.Chat
        NotificationType.MENTION -> Icons.Default.AlternateEmail
        NotificationType.CALL -> Icons.Default.Call
        NotificationType.CALL_MISSED -> Icons.Default.PhoneMissed
        NotificationType.SYSTEM -> Icons.Default.Info
    }
    
    val iconColor = when (notification.type) {
        NotificationType.MESSAGE -> InfoCyan
        NotificationType.MENTION -> WarningOrange
        NotificationType.CALL -> OnlineGreen
        NotificationType.CALL_MISSED -> DndRed
        NotificationType.SYSTEM -> TextSecondary
    }
    
    val cardModifier = if (!notification.isRead) {
        Modifier.border(1.dp, PhantomRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
    } else Modifier
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(cardModifier),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) VelvetSurface else VelvetMid
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        fontWeight = if (!notification.isRead) FontWeight.SemiBold else FontWeight.Normal
                    )
                    
                    if (!notification.isRead) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(PhantomRed, CircleShape)
                        )
                    }
                }
                
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = formatTimestamp(notification.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
            
            // Actions
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Data classes
data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val channelId: String? = null,
    val senderId: String? = null,
    val timestamp: Long,
    val isRead: Boolean = false
)

enum class NotificationType {
    MESSAGE, MENTION, CALL, CALL_MISSED, SYSTEM
}

enum class NotificationFilter(val displayName: String) {
    ALL("All"),
    MESSAGES("Messages"),
    MENTIONS("Mentions"),
    CALLS("Calls")
}

// Helper functions
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

// Sample data generator
private fun generateSampleNotifications(): List<NotificationItem> {
    val now = System.currentTimeMillis()
    return listOf(
        NotificationItem(
            id = "1",
            type = NotificationType.MESSAGE,
            title = "John Doe",
            message = "Hey! Are you coming to the meeting later?",
            channelId = "dm_123",
            timestamp = now - 120_000,
            isRead = false
        ),
        NotificationItem(
            id = "2",
            type = NotificationType.MENTION,
            title = "Fluxer Developers",
            message = "@You mentioned in #general: Check out this new feature!",
            channelId = "channel_456",
            timestamp = now - 3_600_000,
            isRead = false
        ),
        NotificationItem(
            id = "3",
            type = NotificationType.CALL_MISSED,
            title = "Jane Smith",
            message = "Missed voice call",
            timestamp = now - 7_200_000,
            isRead = true
        ),
        NotificationItem(
            id = "4",
            type = NotificationType.MESSAGE,
            title = "Team Chat",
            message = "Alice: I've uploaded the new designs to Figma",
            channelId = "channel_789",
            timestamp = now - 86_400_000,
            isRead = true
        ),
        NotificationItem(
            id = "5",
            type = NotificationType.SYSTEM,
            title = "Fluxer",
            message = "Your password was changed successfully",
            timestamp = now - 172_800_000,
            isRead = true
        )
    )
}
