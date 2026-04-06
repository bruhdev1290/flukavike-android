package com.fluxer.client.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fluxer.client.ui.theme.*
import com.fluxer.client.ui.viewmodel.NotificationSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NOTIFICATIONS", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
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
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PhantomRed)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Master switch
                NotificationToggle(
                    icon = Icons.Default.Notifications,
                    title = "Enable Notifications",
                    subtitle = "Receive push notifications",
                    checked = settings.globalEnabled,
                    onCheckedChange = { viewModel.updateGlobalEnabled(it) }
                )
                
                if (settings.globalEnabled) {
                    Divider(color = BorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // Direct Messages
                    NotificationToggle(
                        icon = Icons.Default.Message,
                        title = "Direct Messages",
                        subtitle = "Notifications for DMs",
                        checked = settings.dmNotifications,
                        onCheckedChange = { viewModel.updateDMNotifications(it) }
                    )
                    
                    Divider(color = BorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // Mentions
                    NotificationToggle(
                        icon = Icons.Default.AlternateEmail,
                        title = "Mentions",
                        subtitle = "When someone mentions you",
                        checked = settings.mentionNotifications,
                        onCheckedChange = { viewModel.updateMentionNotifications(it) }
                    )
                    
                    Divider(color = BorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // Calls
                    NotificationToggle(
                        icon = Icons.Default.Call,
                        title = "Calls",
                        subtitle = "Incoming call notifications",
                        checked = settings.callNotifications,
                        onCheckedChange = { viewModel.updateCallNotifications(it) }
                    )
                    
                    Divider(color = BorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // Friend Requests
                    NotificationToggle(
                        icon = Icons.Default.PersonAdd,
                        title = "Friend Requests",
                        subtitle = "When someone adds you",
                        checked = settings.friendRequestNotifications,
                        onCheckedChange = { viewModel.updateFriendRequestNotifications(it) }
                    )
                    
                    Divider(color = BorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // Sound
                    NotificationToggle(
                        icon = Icons.Default.VolumeUp,
                        title = "Sound",
                        subtitle = "Play notification sounds",
                        checked = settings.soundEnabled,
                        onCheckedChange = { viewModel.updateSoundEnabled(it) }
                    )
                    
                    Divider(color = BorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // Vibration
                    NotificationToggle(
                        icon = Icons.Default.Vibration,
                        title = "Vibration",
                        subtitle = "Vibrate on notifications",
                        checked = settings.vibrationEnabled,
                        onCheckedChange = { viewModel.updateVibrationEnabled(it) }
                    )
                    
                    Divider(color = BorderSubtle, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // Show Preview
                    NotificationToggle(
                        icon = Icons.Default.Visibility,
                        title = "Show Preview",
                        subtitle = "Display message content in notifications",
                        checked = settings.showPreview,
                        onCheckedChange = { viewModel.updateShowPreview(it) }
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun NotificationToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PhantomRed,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        
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
