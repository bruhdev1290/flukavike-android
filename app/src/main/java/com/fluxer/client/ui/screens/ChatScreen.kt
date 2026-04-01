package com.fluxer.client.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fluxer.client.data.model.UserStatus
import com.fluxer.client.ui.components.*
import com.fluxer.client.ui.theme.*
import com.fluxer.client.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onLogout: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val messageInput by viewModel.messageInput.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val guilds by viewModel.guilds.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val isLoading by viewModel.isLoadingMessages.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VelvetBlack)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Server Sidebar
            ServerSidebar(
                servers = guilds,
                selectedServerId = null, // TODO: Track selected server
                onServerSelected = { /* TODO */ },
                onAddServer = { /* TODO */ }
            )
            
            // Channel List (shown when server selected)
            if (channels.isNotEmpty()) {
                ChannelList(
                    channels = channels,
                    selectedChannelId = selectedChannel?.id,
                    onChannelSelected = { viewModel.selectChannel(it) }
                )
            }
            
            // Main Chat Area
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                // Top App Bar
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = selectedChannel?.name?.uppercase() ?: "SELECT A CHANNEL",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            ConnectionStatus(connectionState)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { /* Open drawer */ }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = TextSecondary
                            )
                        }
                    },
                    actions = {
                        // User avatar with status
                        UserAvatar(
                            user = currentUser,
                            size = 36.dp,
                            showStatus = true
                        )
                        
                        IconButton(onClick = { /* Settings */ }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextSecondary
                            )
                        }
                        
                        IconButton(onClick = onLogout) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Logout",
                                tint = PhantomRed
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = VelvetDark,
                        titleContentColor = TextPrimary
                    )
                )
                
                // Messages List
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(VelvetBlack)
                ) {
                    if (selectedChannel == null) {
                        // No channel selected state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "WELCOME TO FLUXER",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Select a channel to start messaging",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextMuted
                                )
                            }
                        }
                    } else if (messages.isEmpty() && !isLoading) {
                        // Empty channel
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "NO MESSAGES YET",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextMuted
                            )
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            reverseLayout = false
                        ) {
                            items(messages, key = { it.id }) { message ->
                                val isOwnMessage = message.authorId == currentUser?.id
                                val showAvatar = true // TODO: Check if previous message is from same author
                                
                                MessageBubble(
                                    message = message,
                                    isOwnMessage = isOwnMessage,
                                    showAvatar = showAvatar,
                                    onDelete = { viewModel.deleteMessage(message.id) }
                                )
                            }
                        }
                    }
                    
                    // Loading indicator
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = PhantomRed
                        )
                    }
                }
                
                // Message Input
                if (selectedChannel != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(VelvetDark)
                            .padding(16.dp)
                    ) {
                        MessageInputField(
                            value = messageInput,
                            onValueChange = viewModel::updateMessageInput,
                            onSend = viewModel::sendMessage,
                            placeholder = "Message #${selectedChannel.name}"
                        )
                    }
                }
            }
            
            // Members sidebar (optional, collapsed by default)
            // TODO: Implement member list
        }
        
        // Error snackbar
        error?.let { errorMessage ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                containerColor = DndRed,
                contentColor = TextPrimary,
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("DISMISS", color = TextPrimary)
                    }
                }
            ) {
                Text(errorMessage)
            }
        }
    }
}

@Composable
private fun ConnectionStatus(state: com.fluxer.client.data.remote.GatewayWebSocketManager.ConnectionState) {
    val (text, color) = when (state) {
        is com.fluxer.client.data.remote.GatewayWebSocketManager.ConnectionState.Connected -> 
            "Connected" to OnlineGreen
        is com.fluxer.client.data.remote.GatewayWebSocketManager.ConnectionState.Connecting -> 
            "Connecting..." to AwayYellow
        is com.fluxer.client.data.remote.GatewayWebSocketManager.ConnectionState.Disconnecting -> 
            "Disconnecting..." to WarningOrange
        is com.fluxer.client.data.remote.GatewayWebSocketManager.ConnectionState.Disconnected -> 
            "Disconnected" to OfflineGray
        is com.fluxer.client.data.remote.GatewayWebSocketManager.ConnectionState.Error -> 
            "Connection Error" to DndRed
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, androidx.compose.foundation.shape.RoundedCornerShape(50))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text.uppercase(),
            style = FluxerTextStyles.statusIndicator,
            color = color
        )
    }
}
