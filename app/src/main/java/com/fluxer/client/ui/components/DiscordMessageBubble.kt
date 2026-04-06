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
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fluxer.client.data.model.Message
import com.fluxer.client.data.model.Reaction
import com.fluxer.client.data.model.UserStatus
import com.fluxer.client.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Discord-style message bubble with colored usernames, mentions, and modern layout
 */
@Composable
fun DiscordMessageBubble(
    message: Message,
    isOwnMessage: Boolean,
    showAvatar: Boolean,
    isGrouped: Boolean,
    onDelete: () -> Unit,
    onReply: (Message) -> Unit,
    onReact: (String) -> Unit,
    onReplyClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    var showReactionPicker by remember { mutableStateOf(false) }
    
    // Generate a consistent color for this username (like Discord role colors)
    val usernameColor = remember(message.author?.id) {
        generateUsernameColor(message.author?.username ?: "")
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (message.mentions.isNotEmpty()) 
                    PhantomRed.copy(alpha = 0.05f) 
                else 
                    Color.Transparent
            )
            .padding(horizontal = 16.dp, vertical = if (showAvatar) 2.dp else 1.dp)
    ) {
        // Reply reference (Discord style with curved connector)
        message.replyToId?.let { replyId ->
            ReplyReferenceDiscord(
                replyId = replyId,
                replyPreview = message.replyTo?.let { "${it.author?.username}: ${it.content.take(30)}${if (it.content.length > 30) "..." else ""}" } 
                    ?: "Original message was deleted",
                onClick = { onReplyClick(replyId) }
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            // Avatar column
            Box(
                modifier = Modifier.width(56.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                if (showAvatar) {
                    DiscordAvatar(
                        avatarUrl = message.author?.avatarUrl,
                        username = message.author?.username ?: "Unknown",
                        status = message.author?.status ?: UserStatus.OFFLINE
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Header with username and timestamp
                if (showAvatar) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message.author?.username ?: "Unknown",
                            style = MaterialTheme.typography.labelLarge,
                            color = usernameColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = formatDiscordMessageTime(message.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        
                        if (message.isEdited) {
                            Text(
                                text = " (edited)",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
                
                // Message content with mention highlighting
                SelectionContainer {
                    HighlightedMessageText(
                        content = message.content,
                        mentions = message.mentions,
                        modifier = Modifier.padding(top = if (showAvatar) 2.dp else 0.dp)
                    )
                }

                // Attachments
                if (message.attachments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    message.attachments.forEach { attachment ->
                        DiscordAttachment(attachment = attachment)
                    }
                }
                
                // Embeds (for links, etc.)
                if (message.embeds.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    message.embeds.forEach { embed ->
                        DiscordEmbed(embed = embed)
                    }
                }
                
                // Reactions row
                if (message.reactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    DiscordReactionsRow(
                        reactions = message.reactions,
                        onReactionClick = { emoji -> onReact(emoji) }
                    )
                }
            }
        }
    }
    
    // Hover menu for message actions
    if (showMenu) {
        MessageActionMenu(
            onDismiss = { showMenu = false },
            onReply = { 
                onReply(message)
                showMenu = false 
            },
            onReact = {
                showReactionPicker = true
                showMenu = false
            },
            onCopy = { showMenu = false },
            onDelete = if (isOwnMessage) {
                {
                    onDelete()
                    showMenu = false
                }
            } else null
        )
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
private fun HighlightedMessageText(
    content: String,
    mentions: List<com.fluxer.client.data.model.User> = emptyList(),
    modifier: Modifier = Modifier
) {
    // Simple mention highlighting with background color
    // For full implementation, use AnnotatedString with SpanStyles
    val hasMention = mentions.isNotEmpty() || content.contains("@")
    
    Text(
        text = content,
        style = MaterialTheme.typography.bodyLarge,
        color = TextPrimary,
        modifier = modifier
    )
}

@Composable
private fun DiscordAvatar(
    avatarUrl: String?,
    username: String,
    status: UserStatus
) {
    Box {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = VelvetSurface
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = username,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = username.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = PhantomRed
                    )
                }
            }
        }
        
        // Status indicator
        val statusColor = when (status) {
            UserStatus.ONLINE -> OnlineGreen
            UserStatus.AWAY -> AwayYellow
            UserStatus.DND -> DndRed
            UserStatus.OFFLINE -> OfflineGray
        }
        
        Box(
            modifier = Modifier
                .size(12.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = 2.dp)
                .background(VelvetBlack, CircleShape)
                .padding(2.dp)
                .background(statusColor, CircleShape)
        )
    }
}

@Composable
private fun ReplyReferenceDiscord(
    replyId: String,
    replyPreview: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, bottom = 4.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reply icon with text
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Reply,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(6.dp))
        
        Text(
            text = "Reply to message",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )
    }
}

@Composable
private fun DiscordAttachment(
    attachment: com.fluxer.client.data.model.Attachment
) {
    val isImage = attachment.contentType?.startsWith("image/") == true
    
    if (isImage) {
        Column {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .heightIn(max = 300.dp),
                shape = RoundedCornerShape(8.dp),
                color = VelvetSurface
            ) {
                AsyncImage(
                    model = attachment.url,
                    contentDescription = attachment.filename,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            }
            
            // Expiration info
            if (attachment.filename.contains("temp", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Expires in 7 days",
                    style = MaterialTheme.typography.labelSmall,
                    color = InfoCyan
                )
            }
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth(0.8f),
            shape = RoundedCornerShape(8.dp),
            color = VelvetSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = PhantomRed,
                    modifier = Modifier.size(40.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = attachment.filename,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatFileSize(attachment.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                
                IconButton(onClick = { /* TODO: Download */ }) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscordEmbed(
    embed: com.fluxer.client.data.model.Embed
) {
    Surface(
        modifier = Modifier.fillMaxWidth(0.9f),
        shape = RoundedCornerShape(4.dp),
        color = VelvetSurface
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(embed.color?.let { Color(it) } ?: PhantomRed)
            )
            
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                embed.author?.let { author ->
                    Text(
                        text = author.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
                
                embed.title?.let { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = InfoCyan,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                embed.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                
                embed.image?.let { image ->
                    Spacer(modifier = Modifier.height(8.dp))
                    AsyncImage(
                        model = image.url,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                
                embed.footer?.let { footer ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = footer.text,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscordReactionsRow(
    reactions: List<Reaction>,
    onReactionClick: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        reactions.forEach { reaction ->
            DiscordReactionChip(
                reaction = reaction,
                onClick = { onReactionClick(reaction.emoji) }
            )
        }
        
        // Add reaction button
        Surface(
            onClick = { onReactionClick("") },
            modifier = Modifier.size(28.dp),
            shape = RoundedCornerShape(12.dp),
            color = VelvetSurface
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.AddReaction,
                    contentDescription = "Add reaction",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DiscordReactionChip(
    reaction: Reaction,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.height(28.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (reaction.userReacted) PhantomRed.copy(alpha = 0.15f) else VelvetSurface,
        border = if (reaction.userReacted) {
            androidx.compose.foundation.BorderStroke(1.dp, PhantomRed.copy(alpha = 0.3f))
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = reaction.emoji,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = reaction.count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (reaction.userReacted) PhantomRed else TextMuted,
                fontWeight = if (reaction.userReacted) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun MessageActionMenu(
    onDismiss: () -> Unit,
    onReply: () -> Unit,
    onReact: () -> Unit,
    onCopy: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss)
        )
        
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = VelvetSurface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column {
                MessageActionItem(
                    icon = Icons.AutoMirrored.Filled.Reply,
                    text = "Reply",
                    onClick = onReply
                )
                MessageActionItem(
                    icon = Icons.Default.AddReaction,
                    text = "Add Reaction",
                    onClick = onReact
                )
                MessageActionItem(
                    icon = Icons.Default.ContentCopy,
                    text = "Copy Text",
                    onClick = onCopy
                )
                onDelete?.let {
                    MessageActionItem(
                        icon = Icons.Default.Delete,
                        text = "Delete",
                        isDestructive = true,
                        onClick = it
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDestructive) DndRed else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDestructive) DndRed else TextPrimary
        )
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
            Column {
                androidx.compose.foundation.layout.FlowRow(
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "More emoji options coming soon",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
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

// Helper functions

private fun generateUsernameColor(username: String): Color {
    // Discord-like username colors based on hash
    val colors = listOf(
        Color(0xFF1ABC9C), // Teal
        Color(0xFF2ECC71), // Green
        Color(0xFF3498DB), // Blue
        Color(0xFF9B59B6), // Purple
        Color(0xFFE91E63), // Pink
        Color(0xFFF1C40F), // Yellow
        Color(0xFFE67E22), // Orange
        Color(0xFFE74C3C), // Red
        Color(0xFF95A5A6), // Gray
    )
    
    val hash = username.hashCode().absoluteValue
    return colors[hash % colors.size]
}

private fun formatDiscordMessageTime(isoString: String): String {
    return try {
        val instant = Instant.parse(isoString)
        val now = Instant.now()
        val yesterday = now.minus(1, ChronoUnit.DAYS)
        
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
            .withZone(ZoneId.systemDefault())
        
        when {
            instant.truncatedTo(ChronoUnit.DAYS) == now.truncatedTo(ChronoUnit.DAYS) -> {
                "Today at ${timeFormatter.format(instant)}"
            }
            instant.truncatedTo(ChronoUnit.DAYS) == yesterday.truncatedTo(ChronoUnit.DAYS) -> {
                "Yesterday at ${timeFormatter.format(instant)}"
            }
            else -> {
                val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
                    .withZone(ZoneId.systemDefault())
                "${dateFormatter.format(instant)} ${timeFormatter.format(instant)}"
            }
        }
    } catch (e: Exception) {
        isoString
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

private val Int.absoluteValue: Int
    get() = if (this < 0) -this else this
