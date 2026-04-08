package com.fluxer.client.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.fluxer.client.data.model.Message
import com.fluxer.client.data.model.User
import com.fluxer.client.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Message bubble component with gaming aesthetic
 */
@Composable
fun MessageBubble(
    message: Message,
    isOwnMessage: Boolean,
    showAvatar: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        if (!isOwnMessage && showAvatar) {
            // Avatar for other users
            UserAvatar(
                user = message.author,
                size = 40.dp,
                modifier = Modifier.padding(end = 12.dp)
            )
        } else if (!isOwnMessage) {
            // Spacer for alignment when avatar is hidden
            Spacer(modifier = Modifier.width(52.dp))
        }
        
        Column(
            horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
        ) {
            // Username and timestamp
            if (!isOwnMessage && showAvatar) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = message.author?.username ?: "Unknown",
                        style = FluxerTextStyles.gamerTag,
                        color = PhantomRed
                    )
                    Text(
                        text = formatTimestamp(message.createdAt),
                        style = FluxerTextStyles.timestamp,
                        color = TextMuted,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    if (message.isEdited) {
                        Text(
                            text = "(edited)",
                            style = FluxerTextStyles.timestamp,
                            color = TextMuted,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
            
            // Message content
            Box(
                modifier = Modifier
                    .widthIn(max = 320.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            color = if (isOwnMessage) PhantomRed.copy(alpha = 0.9f) else VelvetSurface,
                            shape = if (isOwnMessage) {
                                RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
                            } else {
                                RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (isOwnMessage) PhantomRed.copy(alpha = 0.5f) else BorderSubtle.copy(alpha = 0.5f),
                            shape = if (isOwnMessage) {
                                RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
                            } else {
                                RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
                            }
                        )
                        .padding(12.dp)
                ) {
                    if (message.content.isNotBlank()) {
                        Text(
                            text = message.content,
                            style = FluxerTextStyles.messageContent,
                            color = TextPrimary,
                            maxLines = 20,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.animateContentSize()
                        )
                    }

                    // Attachments
                    if (message.attachments.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        message.attachments.forEach { attachment ->
                            ChatAttachment(attachment = attachment)
                        }
                    }
                }
            }
            
            // Timestamp for own messages
            if (isOwnMessage) {
                Text(
                    text = formatTimestamp(message.createdAt),
                    style = FluxerTextStyles.timestamp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
    
    // Message actions menu
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        modifier = Modifier.background(VelvetSurface)
    ) {
        if (isOwnMessage) {
            DropdownMenuItem(
                text = { Text("Delete", color = DndRed) },
                onClick = {
                    onDelete()
                    showMenu = false
                }
            )
        }
        DropdownMenuItem(
            text = { Text("Copy", color = TextPrimary) },
            onClick = { showMenu = false }
        )
    }
}

/**
 * User avatar component with image loading
 */
@Composable
fun UserAvatar(
    user: User?,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    showStatus: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        modifier.size(size).clickable(onClick = onClick)
    } else {
        modifier.size(size)
    }
    Box(modifier = clickableModifier) {
        // Avatar image or placeholder
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(50),
            color = VelvetSurface,
            border = androidx.compose.foundation.BorderStroke(2.dp, BorderSubtle)
        ) {
            if (!user?.avatarUrl.isNullOrBlank()) {
                // Load user avatar image
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user?.avatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = user?.username ?: "User",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user?.username?.take(1)?.uppercase() ?: "?",
                                style = MaterialTheme.typography.titleMedium,
                                color = PhantomRed
                            )
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user?.username?.take(1)?.uppercase() ?: "?",
                                style = MaterialTheme.typography.titleMedium,
                                color = PhantomRed
                            )
                        }
                    }
                )
            } else {
                // Fallback to initials
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = user?.username?.take(1)?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = PhantomRed
                    )
                }
            }
        }
        
        // Status indicator
        if (showStatus) {
            val statusColor = when (user?.status) {
                com.fluxer.client.data.model.UserStatus.ONLINE -> OnlineGreen
                com.fluxer.client.data.model.UserStatus.AWAY -> AwayYellow
                com.fluxer.client.data.model.UserStatus.DND -> DndRed
                else -> OfflineGray
            }
            
            Box(
                modifier = Modifier
                    .size(size * 0.3f)
                    .align(Alignment.BottomEnd)
                    .background(VelvetBlack, RoundedCornerShape(50))
                    .padding(2.dp)
                    .background(statusColor, RoundedCornerShape(50))
            )
        }
    }
}

/**
 * Date separator for messages
 */
@Composable
fun DateSeparator(date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Divider(
            modifier = Modifier.weight(1f),
            color = BorderSubtle,
            thickness = 1.dp
        )
        Text(
            text = date.uppercase(),
            style = FluxerTextStyles.timestamp,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Divider(
            modifier = Modifier.weight(1f),
            color = BorderSubtle,
            thickness = 1.dp
        )
    }
}

private fun formatTimestamp(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val formatter = DateTimeFormatter.ofPattern("h:mm a")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        isoString
    }
}
