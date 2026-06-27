@file:OptIn(ExperimentalLayoutApi::class)

package com.fluxer.client.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.fluxer.client.data.model.Message
import com.fluxer.client.data.model.Reaction
import com.fluxer.client.data.model.User
import com.fluxer.client.ui.theme.*
import com.fluxer.client.util.CdnUrlBuilder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Message bubble component aligned with the premium Fluxer redesign.
 */
@Composable
fun MessageBubble(
    message: Message,
    isOwnMessage: Boolean,
    showAvatar: Boolean,
    cdnBaseUrl: String? = null,
    onDelete: () -> Unit,
    onReply: () -> Unit,
    onAddReaction: (String) -> Unit,
    onPin: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onAvatarClick: ((String) -> Unit)? = null,
    onViewAttachment: ((com.fluxer.client.data.model.Attachment) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { showMenu = true }
                    )
                },
            horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
        ) {
            if (!isOwnMessage && showAvatar) {
                // Avatar for other users
                UserAvatar(
                    user = message.author,
                    size = 40.dp,
                    cdnBaseUrl = cdnBaseUrl,
                    modifier = Modifier.padding(end = 12.dp),
                    onClick = if (onAvatarClick != null && message.author?.id != null)
                        { { onAvatarClick(message.author.id) } } else null
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
                            color = TextSecondary
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
                                color = if (isOwnMessage) PhantomRed.copy(alpha = 0.2f) else VelvetDark,
                                shape = if (isOwnMessage) FluxerExtendedShapes.messageSent else FluxerExtendedShapes.messageReceived
                            )
                            .border(
                                width = 1.dp,
                                color = if (isOwnMessage) PhantomRed.copy(alpha = 0.35f) else BorderSubtle.copy(alpha = 0.8f),
                                shape = if (isOwnMessage) FluxerExtendedShapes.messageSent else FluxerExtendedShapes.messageReceived
                            )
                            .padding(13.dp)
                    ) {
                        // Reply preview
                        message.replyTo?.let { replyMessage ->
                            ReplyPreview(
                                message = replyMessage,
                                cdnBaseUrl = cdnBaseUrl,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
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
                                ChatAttachment(
                                    attachment = attachment,
                                    onViewAttachment = onViewAttachment
                                )
                            }
                        }
                    }
                }
                
                // Reactions row
                if (message.reactions.isNotEmpty()) {
                    ReactionsRow(
                        reactions = message.reactions,
                        onToggleReaction = onAddReaction,
                        onOpenPicker = { showEmojiPicker = true },
                        modifier = Modifier.padding(top = 4.dp)
                    )
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
            // Reply option
            DropdownMenuItem(
                text = { Text("Reply", color = TextPrimary) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Reply,
                        contentDescription = null,
                        tint = TextPrimary
                    )
                },
                onClick = {
                    onReply()
                    showMenu = false
                }
            )
            
            // Add Reaction option
            DropdownMenuItem(
                text = { Text("Add Reaction", color = TextPrimary) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.EmojiEmotions,
                        contentDescription = null,
                        tint = PhantomRed
                    )
                },
                onClick = {
                    showEmojiPicker = true
                    showMenu = false
                }
            )
            
            if (isOwnMessage) {
                onEdit?.let { editFn ->
                    DropdownMenuItem(
                        text = { Text("Edit", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary) },
                        onClick = { editFn(); showMenu = false }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Delete", color = DndRed) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = DndRed
                        )
                    },
                    onClick = {
                        onDelete()
                        showMenu = false
                    }
                )
            }
            
            onPin?.let { pinFn ->
                DropdownMenuItem(
                    text = { Text("Pin Message", color = TextPrimary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            tint = TextPrimary
                        )
                    },
                    onClick = { pinFn(); showMenu = false }
                )
            }

            DropdownMenuItem(
                text = { Text("Copy", color = TextPrimary) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = TextPrimary
                    )
                },
                onClick = { showMenu = false }
            )
        }
        
        // Emoji Picker Dialog
        if (showEmojiPicker) {
            EmojiPickerDialog(
                onEmojiSelected = { emoji ->
                    onAddReaction(emoji)
                    showEmojiPicker = false
                },
                onDismiss = { showEmojiPicker = false }
            )
        }
    }
}

/**
 * Reply preview for quoted messages - Discord-style inline reply
 */
@Composable
private fun ReplyPreview(
    message: Message,
    cdnBaseUrl: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // L-shaped reply indicator line
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(16.dp)
        ) {
            // Vertical line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(16.dp)
                    .background(TextMuted.copy(alpha = 0.6f), RoundedCornerShape(1.dp))
                    .align(Alignment.TopStart)
            )
            // Horizontal line
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(2.dp)
                    .background(TextMuted.copy(alpha = 0.6f), RoundedCornerShape(1.dp))
                    .align(Alignment.CenterStart)
            )
        }
        
        // Reply info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp)
        ) {
            // Author avatar (small)
            val replyAvatarUrl = CdnUrlBuilder.avatarUrl(cdnBaseUrl, message.author?.id ?: "", message.author?.avatarUrl)
            if (replyAvatarUrl != null) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(replyAvatarUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(50))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(VelvetSurface, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = message.author?.username?.take(1)?.uppercase() ?: "?",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(6.dp))
            
            // Author name
            Text(
                text = message.author?.username ?: "Unknown",
                style = MaterialTheme.typography.labelMedium,
                color = PhantomRed,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.width(6.dp))
            
            // Message preview
            Text(
                text = message.content.take(40) + if (message.content.length > 40) "..." else "",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Reactions row displaying emoji reactions
 */
@Composable
private fun ReactionsRow(
    reactions: List<Reaction>,
    onToggleReaction: (String) -> Unit,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        reactions.forEach { reaction ->
            ReactionChip(reaction = reaction, onToggle = { onToggleReaction(reaction.emoji.name) })
        }
        Surface(
            modifier = Modifier.size(28.dp).clickable { onOpenPicker() },
            shape = RoundedCornerShape(12.dp),
            color = VelvetSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AddReaction, contentDescription = "Add reaction", tint = TextMuted, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/**
 * Individual reaction chip
 */
@Composable
private fun ReactionChip(reaction: Reaction, onToggle: () -> Unit = {}) {
    val backgroundColor = if (reaction.userReacted) PhantomRed.copy(alpha = 0.3f) else VelvetSurface
    val borderColor = if (reaction.userReacted) PhantomRed else BorderSubtle

    Surface(
        modifier = Modifier.height(28.dp).clickable { onToggle() },
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = reaction.emoji.name,
                style = MaterialTheme.typography.bodyMedium
            )
            if (reaction.count > 1) {
                Text(
                    text = reaction.count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (reaction.userReacted) TextPrimary else TextSecondary
                )
            }
        }
    }
}

/**
 * Simple emoji picker dialog
 */
@Composable
private fun EmojiPickerDialog(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val commonEmojis = listOf(
        "👍", "👎", "😂", "❤️", "🔥", "👏", "😢", "😮",
        "🎉", "🤔", "👌", "😍", "🙏", "💯", "🤣", "😭"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VelvetMid,
        title = {
            Text(
                text = "Add Reaction",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                commonEmojis.forEach { emoji ->
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { onEmojiSelected(emoji) },
                        shape = RoundedCornerShape(8.dp),
                        color = VelvetSurface
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = emoji,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

/**
 * User avatar component with image loading
 */
@Composable
fun UserAvatar(
    user: User?,
    size: androidx.compose.ui.unit.Dp,
    cdnBaseUrl: String? = null,
    modifier: Modifier = Modifier,
    showStatus: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val clickableModifier = when {
        onClick != null && onLongClick != null ->
            modifier.size(size).pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongClick() })
            }
        onClick != null -> modifier.size(size).clickable(onClick = onClick)
        else -> modifier.size(size)
    }
    Box(modifier = clickableModifier) {
        // Avatar image or placeholder
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(50),
            color = VelvetSurface,
            border = androidx.compose.foundation.BorderStroke(2.dp, BorderSubtle)
        ) {
            val resolvedAvatarUrl = CdnUrlBuilder.avatarUrl(cdnBaseUrl, user?.id ?: "", user?.avatarUrl)
            if (resolvedAvatarUrl != null) {
                // Load user avatar image
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(resolvedAvatarUrl)
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
        HorizontalDivider(
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
        HorizontalDivider(
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
