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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
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
    val messages = viewModel.messages.collectAsLazyPagingItems()
    val messageInput by viewModel.messageInput.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val guilds by viewModel.guilds.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()
    val isLoading by viewModel.isLoadingMessages.collectAsState()
    val error by viewModel.error.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val activeChannel = selectedChannel
    
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Responsive layout state
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isCompact = screenWidth < 600.dp
    val isMedium = screenWidth >= 600.dp && screenWidth < 840.dp
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.itemCount) {
        if (messages.itemCount > 0) {
            scope.launch {
                listState.animateScrollToItem(messages.itemCount - 1)
            }
        }
    }
    
    // Responsive sidebar width
    val sidebarWidth = when {
        isCompact -> 56.dp
        isMedium -> 64.dp
        else -> 72.dp
    }
    
    // Channel drawer for compact screens
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            if (isCompact && channels.isNotEmpty()) {
                ModalDrawerSheet(
                    drawerContainerColor = VelvetMid,
                    drawerContentColor = TextPrimary
                ) {
                    ChannelListContent(
                        channels = channels,
                        selectedChannelId = activeChannel?.id,
                        onChannelSelected = { 
                            viewModel.selectChannel(it)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.width(280.dp)
                    )
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VelvetBlack)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Server Sidebar
                ServerSidebar(
                    servers = guilds,
                    selectedServerId = selectedServer?.id,
                    onServerSelected = { viewModel.selectServer(it) },
                    onAddServer = { /* TODO */ },
                    modifier = Modifier.width(sidebarWidth),
                    isCompact = isCompact
                )
                
                // Channel List (persistent on larger screens, drawer on compact)
                if (channels.isNotEmpty() && !isCompact) {
                    ChannelListContent(
                        channels = channels,
                        selectedChannelId = activeChannel?.id,
                        onChannelSelected = { viewModel.selectChannel(it) },
                        modifier = Modifier.width(if (isMedium) 200.dp else 240.dp)
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
                        IconButton(
                            onClick = { 
                                if (isCompact && channels.isNotEmpty()) {
                                    scope.launch { drawerState.open() }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = TextSecondary
                            )
                        }
                    },
                    actions = {
                        // Search
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = TextSecondary
                            )
                        }
                        
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
                
                // Search Bar
                AnimatedVisibility(visible = searchQuery.isNotEmpty() || isSearching) {
                    FluxerTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        hint = "Search in #${activeChannel?.name}",
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                }

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
                        } else if (messages.itemCount == 0 && !isLoading) {
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
                            reverseLayout = true
                        ) {
                            items(
                                count = messages.itemCount,
                                key = { index -> messages[index]?.id ?: index }
                            ) { index ->
                                val message = messages[index]
                                if (message != null) {
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
                    }
                    
                    // Loading indicator
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = PhantomRed
                        )
                    }

                    // Error state
                    if (error != null) {
                        ErrorState(
                            message = error!!,
                            onRetry = { messages.retry() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
                
                // Message Input
                if (activeChannel != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(VelvetDark)
                            .padding(horizontal = if (isCompact) 8.dp else 16.dp, vertical = 12.dp)
                    ) {
                        MessageInputField(
                            value = messageInput,
                            onValueChange = viewModel::updateMessageInput,
                            onSend = viewModel::sendMessage,
                            placeholder = "Message #${activeChannel.name}",
                            isCompact = isCompact
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
