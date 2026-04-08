@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.fluxer.client.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.fluxer.client.data.model.UserStatus
import com.fluxer.client.ui.components.*
import com.fluxer.client.ui.theme.*
import com.fluxer.client.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onLogout: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToStarred: () -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
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
    
    // Channel drawer state (for compact screens)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var channelDrawerOpen by remember { mutableStateOf(false) }
    
    // Log paging state for debugging
    LaunchedEffect(messages.loadState) {
        Timber.d("Messages load state: ${messages.loadState}")
    }
    
    // Scroll to bottom when new messages arrive (only for new messages, not on initial load)
    var previousMessageCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(messages.itemCount) {
        if (messages.itemCount > 0 && messages.itemCount > previousMessageCount && previousMessageCount > 0) {
            scope.launch {
                listState.animateScrollToItem(0) // With reverseLayout=true, 0 is the bottom
            }
        }
        previousMessageCount = messages.itemCount
    }
    
    // Responsive sidebar width
    val sidebarWidth = when {
        isCompact -> 56.dp
        isMedium -> 64.dp
        else -> 72.dp
    }
    
    // Root layout - Server Sidebar is always visible on the left
    Row(modifier = Modifier.fillMaxSize()) {
        // Server Sidebar - Always visible, never covered
        ServerSidebar(
            servers = guilds,
            selectedServerId = selectedServer?.id,
            onServerSelected = { 
                viewModel.selectServer(it)
                // On compact screens, open channel drawer when server selected
                if (isCompact && channels.isNotEmpty()) {
                    channelDrawerOpen = true
                }
            },
            onAddServer = { /* TODO */ },
            modifier = Modifier.width(sidebarWidth),
            isCompact = isCompact
        )
        
        // Main Content Area with optional Channel List
        Box(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Channel List (persistent on larger screens)
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
                            // Show hamburger menu on compact screens when channels exist
                            if (isCompact && channels.isNotEmpty()) {
                                IconButton(
                                    onClick = { channelDrawerOpen = true }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Channels",
                                        tint = TextSecondary
                                    )
                                }
                            }
                        },
                        actions = {
                            // Search
                            IconButton(onClick = { viewModel.toggleSearch() }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = if (isSearching) PhantomRed else TextSecondary
                                )
                            }
                            
                            // User avatar with status - clickable to view profile
                            UserAvatar(
                                user = currentUser,
                                size = 36.dp,
                                showStatus = true,
                                onClick = onNavigateToProfile
                            )
                            
                            IconButton(onClick = onNavigateToSettings) {
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
                    AnimatedVisibility(visible = isSearching) {
                        FluxerTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            hint = "Search in #${activeChannel?.name ?: ""}",
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
                        when {
                            // No channel selected
                            selectedChannel == null -> {
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
                            }
                            // Loading state from paging
                            messages.loadState.refresh is LoadState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = PhantomRed)
                                }
                            }
                            // Error state from paging
                            messages.loadState.refresh is LoadState.Error -> {
                                val loadStateError = messages.loadState.refresh as LoadState.Error
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Failed to load messages",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = loadStateError.error.message ?: "Unknown error",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextMuted
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { messages.retry() },
                                            colors = ButtonDefaults.buttonColors(containerColor = PhantomRed)
                                        ) {
                                            Text("Retry")
                                        }
                                    }
                                }
                            }
                            // Empty state
                            messages.itemCount == 0 -> {
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
                            }
                            // Messages list
                            else -> {
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
                                    
                                    // Loading more at bottom
                                    item {
                                        if (messages.loadState.append is LoadState.Loading) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    color = PhantomRed,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Error state from ViewModel
                        if (error != null) {
                            ErrorState(
                                message = error!!,
                                onRetry = { messages.retry() },
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                    
                    // Message Input - with proper bottom insets handling
                    if (activeChannel != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(VelvetDark)
                                .padding(horizontal = if (isCompact) 8.dp else 16.dp, vertical = 12.dp)
                                .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars)
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
            }
            
            // Compact Channel Drawer - Slides OVER the content, not replacing server sidebar
            if (isCompact && channelDrawerOpen && channels.isNotEmpty()) {
                // Backdrop to close drawer when clicking outside
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
                        .clickable { channelDrawerOpen = false }
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(280.dp)
                        .background(VelvetMid)
                ) {
                        Column {
                            // Drawer Header
                            Surface(
                                color = VelvetDark,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedServer?.name?.uppercase() ?: "CHANNELS",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    
                                    IconButton(
                                        onClick = { channelDrawerOpen = false }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Close",
                                            tint = TextSecondary
                                        )
                                    }
                                }
                            }
                            
                            // Channel List
                            ChannelListContent(
                                channels = channels,
                                selectedChannelId = activeChannel?.id,
                                onChannelSelected = { 
                                    viewModel.selectChannel(it)
                                    channelDrawerOpen = false
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
            }
            
            // Error snackbar
            error?.let { errorMessage ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
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
