package com.fluxer.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.fluxer.client.data.model.Attachment
import com.fluxer.client.data.model.GuildMember
import com.fluxer.client.data.model.InviteInfo
import com.fluxer.client.data.model.Message
import com.fluxer.client.data.model.User
import com.fluxer.client.ui.theme.*
import com.fluxer.client.util.CdnUrlBuilder

// ==================== PROFILE CARD SHEET ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCardSheet(
    user: User,
    onDismiss: () -> Unit,
    onViewFullProfile: (String) -> Unit,
    onSendMessage: ((String) -> Unit)? = null,
    cdnBaseUrl: String? = null
) {
    val displayName = user.displayName?.takeIf { it.isNotBlank() } ?: user.username
    val avatarUrl = CdnUrlBuilder.avatarUrlOrDefault(
        cdnBase = cdnBaseUrl,
        staticCdnBase = null,
        userId = user.id,
        hash = user.avatarUrl,
        size = CdnUrlBuilder.Sizes.AVATAR_PROFILE
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VelvetDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(VelvetSurface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = displayName,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(displayName, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Text("@${user.username}", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onSendMessage != null) {
                    OutlinedButton(
                        onClick = { onSendMessage(user.id); onDismiss() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Message")
                    }
                }
                Button(
                    onClick = { onViewFullProfile(user.id); onDismiss() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PhantomRed)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Profile")
                }
            }
        }
    }
}

// ==================== CUSTOM STATUS SHEET ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomStatusSheet(
    currentStatus: String?,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit
) {
    var text by remember { mutableStateOf(currentStatus ?: "") }

    val presets = listOf(
        "🟢" to "Online",
        "🌙" to "Do Not Disturb",
        "🎮" to "Gaming",
        "🎧" to "Listening to music",
        "💻" to "Coding",
        "☕" to "On a break"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VelvetDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Set Custom Status",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("What's on your mind?", color = TextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PhantomRed,
                    unfocusedBorderColor = BorderSubtle,
                    cursorColor = PhantomRed
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("Quick picks", style = MaterialTheme.typography.labelMedium, color = TextMuted)
            Spacer(Modifier.height(8.dp))

            presets.chunked(2).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (emoji, label) ->
                        Surface(
                            modifier = Modifier.weight(1f).clickable { text = "$emoji $label" },
                            color = VelvetSurface,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "$emoji $label",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { onSave(null) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) { Text("Clear") }
                Button(
                    onClick = { onSave(text.trim().ifBlank { null }) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PhantomRed)
                ) { Text("Save") }
            }
        }
    }
}

// ==================== JOIN SERVER SHEET ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinServerSheet(
    onDismiss: () -> Unit,
    invitePreview: InviteInfo?,
    onPreviewCode: (String) -> Unit,
    onJoin: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VelvetDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Join a Server",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Enter an invite code or full invite link",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                placeholder = { Text("e.g. abc123 or https://…/invite/abc123", color = TextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PhantomRed,
                    unfocusedBorderColor = BorderSubtle,
                    cursorColor = PhantomRed
                ),
                trailingIcon = {
                    IconButton(onClick = {
                        val trimmed = code.trim().substringAfterLast('/').trim()
                        if (trimmed.isNotBlank()) onPreviewCode(trimmed)
                    }) {
                        Icon(Icons.Default.Search, contentDescription = "Preview", tint = TextSecondary)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            invitePreview?.let { preview ->
                Spacer(Modifier.height(16.dp))
                Surface(color = VelvetSurface, shape = RoundedCornerShape(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val previewIconUrl = preview.guild?.let { guild ->
                            CdnUrlBuilder.serverIconUrl(
                                cdnBase = null,
                                guildId = guild.id,
                                hash = guild.iconUrl,
                                size = 96
                            )
                        }
                        if (previewIconUrl != null) {
                            AsyncImage(
                                model = previewIconUrl,
                                contentDescription = preview.guild.name,
                                modifier = Modifier.size(48.dp).clip(CircleShape)
                            )
                        } else {
                            Box(
                                Modifier.size(48.dp).background(PhantomRed.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    preview.guild?.name?.take(1)?.uppercase() ?: "?",
                                    color = PhantomRed,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(preview.guild?.name ?: "Unknown Server", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${preview.memberCount} members · ${preview.onlineCount} online",
                                color = TextMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = OnlineGreen)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    val trimmed = code.trim().substringAfterLast('/').trim()
                    if (trimmed.isNotBlank()) onJoin(trimmed)
                },
                enabled = code.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PhantomRed)
            ) { Text("Join Server") }
        }
    }
}

// ==================== PINNED MESSAGES SHEET ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinnedMessagesSheet(
    pinnedMessages: List<Message>,
    onDismiss: () -> Unit,
    onUnpin: (String) -> Unit,
    onJumpTo: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VelvetDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Pinned Messages",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            if (pinnedMessages.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text("No pinned messages yet", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 480.dp)) {
                    items(pinnedMessages, key = { it.id }) { msg ->
                        PinnedMessageItem(
                            message = msg,
                            onUnpin = { onUnpin(msg.id) },
                            onJumpTo = { onJumpTo(msg.id) }
                        )
                        Divider(color = BorderSubtle.copy(alpha = 0.4f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PinnedMessageItem(message: Message, onUnpin: () -> Unit, onJumpTo: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f).clickable(onClick = onJumpTo)) {
            Text(
                message.author?.username ?: "Unknown",
                color = PhantomRed,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                message.content.ifBlank { "(attachment)" },
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2
            )
        }
        IconButton(onClick = onUnpin, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Unpin", tint = TextMuted, modifier = Modifier.size(16.dp))
        }
    }
}

// ==================== MEMBER LIST PANEL ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberListPanel(
    members: List<GuildMember>,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (onDismiss != null) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = VelvetDark
        ) {
            MemberListContent(members = members, modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp))
        }
    } else {
        MemberListContent(members = members, modifier = modifier.background(VelvetDark))
    }
}

@Composable
private fun MemberListContent(members: List<GuildMember>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(top = 8.dp)) {
        Text(
            "Members — ${members.size}",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
        LazyColumn {
            items(members, key = { it.user?.id ?: it.nick ?: "" }) { member ->
                MemberRow(member)
            }
        }
    }
}

@Composable
private fun MemberRow(member: GuildMember) {
    val user = member.user
    val displayName = member.nick ?: user?.displayName?.takeIf { it.isNotBlank() } ?: user?.username ?: "Unknown"
    val avatarUrl = CdnUrlBuilder.avatarUrlOrDefault(
        cdnBase = null,
        staticCdnBase = null,
        userId = user?.id ?: "",
        hash = member.avatarUrl ?: user?.avatarUrl
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(32.dp)
                .background(VelvetSurface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = displayName,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Text(
                    displayName.take(1).uppercase(),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(displayName, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

// ==================== ATTACHMENT VIEWER ====================

@Composable
fun AttachmentViewerDialog(
    attachment: Attachment,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = attachment.url,
                contentDescription = attachment.filename,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}

// ==================== CREATE SERVER SHEET ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateServerSheet(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VelvetDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Create a Server",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Give your server a name to get started",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("My Server", color = TextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PhantomRed,
                    unfocusedBorderColor = BorderSubtle,
                    cursorColor = PhantomRed
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PhantomRed)
            ) { Text("Create Server") }
        }
    }
}

// ==================== CREATE CHANNEL SHEET ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChannelSheet(
    onDismiss: () -> Unit,
    onCreate: (name: String, isVoice: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isVoice by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = VelvetDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Create a Channel",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("new-channel", color = TextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PhantomRed,
                    unfocusedBorderColor = BorderSubtle,
                    cursorColor = PhantomRed
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Voice channel", color = TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(
                    checked = isVoice,
                    onCheckedChange = { isVoice = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = PhantomRed, checkedTrackColor = PhantomRed.copy(alpha = 0.4f))
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name.trim(), isVoice) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PhantomRed)
            ) { Text("Create Channel") }
        }
    }
}
