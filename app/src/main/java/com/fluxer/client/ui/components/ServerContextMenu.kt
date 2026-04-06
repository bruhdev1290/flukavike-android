package com.fluxer.client.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fluxer.client.data.model.Server
import com.fluxer.client.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerContextMenu(
    server: Server,
    onDismiss: () -> Unit,
    onMarkAsRead: () -> Unit,
    onNotificationSettings: () -> Unit,
    onPrivacySettings: () -> Unit,
    onEditProfile: () -> Unit,
    onMuteCommunity: () -> Unit,
    onHideMutedChannels: (Boolean) -> Unit,
    onLeaveCommunity: () -> Unit,
    onReportCommunity: () -> Unit,
    onDebugCommunity: () -> Unit,
    hideMutedChannels: Boolean = false
) {
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
            // Header with server icon and name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = RoundedCornerShape(16.dp),
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
                                style = MaterialTheme.typography.headlineSmall,
                                color = PhantomRed
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = server.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Online/Members count
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(OnlineGreen, RoundedCornerShape(50))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${server.onlineCount} Online",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(OfflineGray, RoundedCornerShape(50))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${server.memberCount} Members",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Mark as Read section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = VelvetSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                ServerContextMenuItem(
                    icon = Icons.Default.Visibility,
                    text = "Mark as Read",
                    onClick = onMarkAsRead
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Settings section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = VelvetSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    ServerContextMenuItem(
                        icon = Icons.Default.Notifications,
                        text = "Notification Settings",
                        onClick = onNotificationSettings
                    )
                    ServerContextMenuItem(
                        icon = Icons.Default.Shield,
                        text = "Privacy Settings",
                        onClick = onPrivacySettings
                    )
                    ServerContextMenuItem(
                        icon = Icons.Default.Person,
                        text = "Edit Community Profile",
                        onClick = onEditProfile
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mute section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = VelvetSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    ServerContextMenuItem(
                        icon = null,
                        text = "Mute Community",
                        showArrow = true,
                        onClick = onMuteCommunity
                    )
                    ServerContextMenuItemWithToggle(
                        text = "Hide Muted Channels",
                        checked = hideMutedChannels,
                        onCheckedChange = onHideMutedChannels
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Destructive actions section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = VelvetSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    ServerContextMenuItem(
                        icon = Icons.Default.Logout,
                        text = "Leave Community",
                        isDestructive = true,
                        onClick = onLeaveCommunity
                    )
                    ServerContextMenuItem(
                        icon = Icons.Default.Flag,
                        text = "Report Community",
                        isDestructive = true,
                        onClick = onReportCommunity
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Debug section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = VelvetSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                ServerContextMenuItem(
                    icon = Icons.Default.BugReport,
                    text = "Debug Community",
                    onClick = onDebugCommunity
                )
            }
        }
    }
}

@Composable
private fun ServerContextMenuItem(
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

@Composable
private fun ServerContextMenuItemWithToggle(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextPrimary,
                checkedTrackColor = PhantomRed,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = VelvetLight
            )
        )
    }
}
