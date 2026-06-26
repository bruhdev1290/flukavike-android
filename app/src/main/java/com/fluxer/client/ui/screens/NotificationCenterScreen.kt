package com.fluxer.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PhoneMissed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fluxer.client.data.local.model.NotificationFeedEntity
import com.fluxer.client.ui.components.FluxerEmptyState
import com.fluxer.client.ui.components.FluxerIconButton
import com.fluxer.client.ui.components.FluxerLoadingState
import com.fluxer.client.ui.components.FluxerPageScaffold
import com.fluxer.client.ui.theme.DndRed
import com.fluxer.client.ui.theme.InfoCyan
import com.fluxer.client.ui.theme.OnlineGreen
import com.fluxer.client.ui.theme.PhantomRed
import com.fluxer.client.ui.theme.TextMuted
import com.fluxer.client.ui.theme.TextPrimary
import com.fluxer.client.ui.theme.TextSecondary
import com.fluxer.client.ui.theme.VelvetBlack
import com.fluxer.client.ui.theme.VelvetDark
import com.fluxer.client.ui.theme.VelvetMid
import com.fluxer.client.ui.theme.VelvetSurface
import com.fluxer.client.ui.theme.WarningOrange
import com.fluxer.client.ui.viewmodel.NotificationCenterViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    onBack: () -> Unit,
    onNotificationClick: (NotificationItem) -> Unit = {},
    viewModel: NotificationCenterViewModel = hiltViewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var selectedFilter by remember { mutableStateOf(NotificationFilter.ALL) }

    LaunchedEffect(Unit) {
        viewModel.loadNotifications()
    }

    val notificationItems = remember(notifications) { notifications.map { it.toNotificationItem() } }
    val filteredNotifications = when (selectedFilter) {
        NotificationFilter.ALL -> notificationItems
        NotificationFilter.MESSAGES -> notificationItems.filter {
            it.type == NotificationType.MESSAGE || it.type == NotificationType.SYSTEM
        }
        NotificationFilter.MENTIONS -> notificationItems.filter { it.type == NotificationType.MENTION }
        NotificationFilter.CALLS -> notificationItems.filter {
            it.type == NotificationType.CALL || it.type == NotificationType.CALL_MISSED
        }
    }

    FluxerPageScaffold(
        title = "Notifications",
        subtitle = "${filteredNotifications.size} items",
        onBack = onBack,
        headerActions = {
            FluxerIconButton(
                icon = Icons.Default.DoneAll,
                contentDescription = "Mark all read",
                onClick = viewModel::markAllRead
            )
            FluxerIconButton(
                icon = Icons.Default.DeleteSweep,
                contentDescription = "Clear all",
                onClick = viewModel::clearAll,
                tint = PhantomRed
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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
                            selectedContainerColor = VelvetSurface,
                            selectedLabelColor = TextPrimary,
                            containerColor = VelvetDark,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            when {
                isLoading -> FluxerLoadingState("Loading activity")

                error != null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = error ?: "Failed to load notifications",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DndRed
                    )
                }

                filteredNotifications.isEmpty() -> FluxerEmptyState(
                    title = "No notifications yet",
                    body = "Mentions, messages, and call activity will appear here.",
                    icon = Icons.Default.NotificationsNone
                )

                else -> LazyColumn(
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
                            onClick = {
                                viewModel.markRead(notification.id)
                                onNotificationClick(notification)
                            },
                            onDismiss = { viewModel.markRead(notification.id) }
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
        Modifier.border(1.dp, PhantomRed.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(cardModifier),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) VelvetDark else VelvetMid
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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

private fun NotificationFeedEntity.toNotificationItem(): NotificationItem {
    val normalized = type.trim().lowercase(Locale.US)
    val screenType = when (normalized) {
        "mention" -> NotificationType.MENTION
        "call" -> NotificationType.CALL
        "call_missed" -> NotificationType.CALL_MISSED
        "direct_message", "dm", "message" -> NotificationType.MESSAGE
        else -> NotificationType.SYSTEM
    }
    return NotificationItem(
        id = id,
        type = screenType,
        title = title,
        message = body,
        channelId = channelId,
        senderId = null,
        timestamp = createdAt,
        isRead = read
    )
}

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
