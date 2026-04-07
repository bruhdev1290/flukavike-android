@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.fluxer.client.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fluxer.client.data.model.Message
import com.fluxer.client.data.model.Reaction
import com.fluxer.client.data.model.User
import com.fluxer.client.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun EnhancedMessageBubble(
    message: Message,
    isOwnMessage: Boolean,
    showAvatar: Boolean,
    onDelete: () -> Unit,
    onReply: (Message) -> Unit = {},
    onReact: (String) -> Unit = {},
    onReplyClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showReactionPicker by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        // Reply reference
        message.replyToId?.let { replyId ->
            ReplyReference(
                replyId = replyId,
                isOwnMessage = isOwnMessage,
                onClick = { onReplyClick(replyId) }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
        ) {
            if (!isOwnMessage && showAvatar) {
                UserAvatar(
                    user = message.author,
                    size = 40.dp,
                    modifier = Modifier.padding(end = 12.dp)
                )
            } else if (!isOwnMessage) {
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
                    modifier = Modifier.widthIn(max = 320.dp)
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
                            .animateContentSize()
                    ) {
                        if (message.content.isNotBlank()) {
                            Text(
                                text = message.content,
                                style = FluxerTextStyles.messageContent,
                                color = TextPrimary,
                                maxLines = 20,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Voice message
                        // TODO: Check for voice attachment type
                        // VoiceMessagePlayer(durationSeconds = 23)

                        // Attachments
                        if (message.attachments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            message.attachments.forEach { attachment ->
                                ChatAttachment(attachment = attachment)
                            }
                        }
                    }
                    
                    // Hover actions
                    Row(
                        modifier = Modifier
                            .align(if (isOwnMessage) Alignment.BottomStart else Alignment.BottomEnd)
                            .offset(
                                x = if (isOwnMessage) (-8).dp else 8.dp,
                                y = 16.dp
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(VelvetLight)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { showReactionPicker = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddReaction,
                                contentDescription = "React",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { onReply(message) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Reply,
                                contentDescription = "Reply",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                
                // Reactions row
                if (message.reactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    ReactionsRow(
                        reactions = message.reactions,
                        onReactionClick = { emoji -> onReact(emoji) }
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
    }
    
    // Message actions menu
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false },
        modifier = Modifier.background(VelvetSurface)
    ) {
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, null, tint = TextSecondary) },
            text = { Text("Reply", color = TextPrimary) },
            onClick = {
                onReply(message)
                showMenu = false
            }
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.AddReaction, null, tint = TextSecondary) },
            text = { Text("Add Reaction", color = TextPrimary) },
            onClick = {
                showReactionPicker = true
                showMenu = false
            }
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = TextSecondary) },
            text = { Text("Copy Text", color = TextPrimary) },
            onClick = { showMenu = false }
        )
        if (isOwnMessage) {
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.Edit, null, tint = TextSecondary) },
                text = { Text("Edit", color = TextPrimary) },
                onClick = { showMenu = false }
            )
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = DndRed) },
                text = { Text("Delete", color = DndRed) },
                onClick = {
                    onDelete()
                    showMenu = false
                }
            )
        }
    }
    
    // Reaction picker
    if (showReactionPicker) {
        ReactionPickerDialog(
            onDismiss = { showReactionPicker = false },
            onReactionSelected = { emoji ->
                onReact(emoji)
                showReactionPicker = false
            }
        )
    }
}

@Composable
private fun ReplyReference(
    replyId: String,
    isOwnMessage: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(if (isOwnMessage) 0.8f else 0.9f)
            .padding(start = if (isOwnMessage) 0.dp else 52.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        // Reply indicator line
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .background(PhantomRed.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column {
            Text(
                text = "Replying to message",
                style = MaterialTheme.typography.labelSmall,
                color = PhantomRed
            )
            Text(
                text = "Click to view",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReactionsRow(
    reactions: List<Reaction>,
    onReactionClick: (String) -> Unit
) {
    Row(
        modifier = Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        reactions.forEach { reaction ->
            ReactionChip(
                reaction = reaction,
                onClick = { onReactionClick(reaction.emoji.name) }
            )
        }
    }
}

@Composable
private fun ReactionChip(
    reaction: Reaction,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.height(28.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (reaction.userReacted) PhantomRed.copy(alpha = 0.2f) else VelvetLight
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = reaction.emoji.name,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = reaction.count.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = if (reaction.userReacted) PhantomRed else TextSecondary,
                fontWeight = if (reaction.userReacted) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ReactionPickerDialog(
    onDismiss: () -> Unit,
    onReactionSelected: (String) -> Unit
) {
    val commonReactions = listOf("👍", "❤️", "😂", "😮", "😢", "🎉", "🔥", "👏", "🤔", "😡")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VelvetSurface,
        titleContentColor = TextPrimary,
        title = { Text("Add Reaction") },
        text = {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                commonReactions.forEach { emoji ->
                    Surface(
                        onClick = { onReactionSelected(emoji) },
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = VelvetLight
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

@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement
    ) {
        content()
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
