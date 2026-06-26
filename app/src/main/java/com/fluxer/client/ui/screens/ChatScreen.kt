@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.fluxer.client.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.fluxer.client.data.model.displayName
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
    onNavigateToVoiceChannel: (String) -> Unit = {},
    initialGuildId: String? = null,
    initialChannelId: String? = null,
    targetMessageId: String? = null,
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
    val replyingTo by viewModel.replyingTo.collectAsState()
    val favoriteChannelIds by viewModel.favoriteChannelIds.collectAsState()
    val unreadCountsByChannel by viewModel.unreadCountsByChannel.collectAsState()
    val activeChannel = selectedChannel
    
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Responsive layout state
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isCompact = screenWidth < 600.dp
    val isMedium = screenWidth >= 600.dp && screenWidth < 840.dp
    
    // Channel drawer state (for compact screens)
    var channelDrawerOpen by remember { mutableStateOf(false) }
    val unreadCountsByGuild = remember(guilds, unreadCountsByChannel) {
        guilds.associate { guild ->
            val count = guild.channels.sumOf { channel -> unreadCountsByChannel[channel.id] ?: 0 }
            guild.id to count
        }
    }
    val selectedChannelUnread = activeChannel?.let { unreadCountsByChannel[it.id] ?: 0 } ?: 0
    val isFavoriteChannel = activeChannel?.let { favoriteChannelIds.contains(it.id) } == true
    
    // Image picker for attachments
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // TODO: Upload image and send as attachment
            Timber.d("Selected image: $uri")
        }
    }
    
    // Log paging state for debugging
    LaunchedEffect(messages.loadState) {
        Timber.d("Messages load state: ${messages.loadState}")
    }

    LaunchedEffect(initialGuildId, initialChannelId, guilds, channels) {
        if (!initialGuildId.isNullOrBlank()) {
            viewModel.selectServerById(initialGuildId)
        }
        if (!initialChannelId.isNullOrBlank()) {
            viewModel.selectChannelById(initialChannelId, initialGuildId)
        }
        if (!targetMessageId.isNullOrBlank()) {
            viewModel.jumpToMessage(targetMessageId)
        }
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
            onHomeSelected = onNavigateToMessages,
            unreadCountsByGuild = unreadCountsByGuild,
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
                        onNavigateToVoiceChannel = onNavigateToVoiceChannel,
                        unreadCountsByChannel = unreadCountsByChannel,
                        favoriteChannelIds = favoriteChannelIds,
                        modifier = Modifier.width(if (isMedium) 200.dp else 240.dp)
                    )
                }
                
                // Main Chat Area
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .padding(if (isCompact) 0.dp else 12.dp)
                ) {
                    FluxerPanel(
                        modifier = Modifier.fillMaxWidth(),
                        tonal = false
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isCompact && channels.isNotEmpty()) {
                                FluxerIconButton(
                                    icon = Icons.Default.Menu,
                                    contentDescription = "Channels",
                                    onClick = { channelDrawerOpen = true }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedChannel?.displayName() ?: "Conversation",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = selectedServer?.name ?: "Direct messages and guild activity",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (activeChannel != null) {
                                    FluxerIconButton(
                                        icon = if (isFavoriteChannel) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = if (isFavoriteChannel) "Remove favorite" else "Favorite channel",
                                        onClick = viewModel::toggleFavoriteForSelectedChannel,
                                        tint = if (isFavoriteChannel) AlertYellow else TextSecondary
                                    )
                                }
                                FluxerIconButton(
                                    icon = Icons.Default.Search,
                                    contentDescription = "Search",
                                    onClick = { viewModel.toggleSearch() },
                                    tint = if (isSearching) TextPrimary else TextSecondary,
                                    containerColor = if (isSearching) PhantomRed.copy(alpha = 0.2f) else VelvetMid
                                )
                                if (!isCompact) {
                                    ConnectionStatus(connectionState)
                                }
                                UserAvatar(
                                    user = currentUser,
                                    size = 36.dp,
                                    showStatus = true,
                                    onClick = onNavigateToProfile
                                )
                                if (!isCompact) {
                                    FluxerIconButton(
                                        icon = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        onClick = onNavigateToSettings
                                    )
                                    FluxerIconButton(
                                        icon = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "Logout",
                                        onClick = onLogout,
                                        tint = PhantomRed
                                    )
                                }
                            }
                        }
                    }

                    if (!isCompact) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FluxerInlineStat(
                                label = "Guilds",
                                value = guilds.size.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            FluxerInlineStat(
                                label = "Channels",
                                value = channels.size.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            FluxerInlineStat(
                                label = "Unread",
                                value = selectedChannelUnread.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    // Search Bar
                    AnimatedVisibility(visible = isSearching) {
                        FluxerTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            hint = "Search in #${activeChannel?.displayName() ?: ""}",
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
                                FluxerEmptyState(
                                    title = "Nothing selected",
                                    body = "Choose a conversation from the rail or channel list to start chatting.",
                                    icon = Icons.Default.Chat
                                )
                            }
                            // Loading state from paging
                            messages.loadState.refresh is LoadState.Loading -> {
                                FluxerLoadingState("Loading messages")
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
                                FluxerEmptyState(
                                    title = "No messages yet",
                                    body = "Say hello when you're ready. This conversation is still quiet.",
                                    icon = Icons.Default.ChatBubbleOutline
                                )
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
                                                onDelete = { viewModel.deleteMessage(message.id) },
                                                onReply = { viewModel.startReply(message) },
                                                onAddReaction = { emoji ->
                                                    viewModel.addReaction(message.id, emoji)
                                                }
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
                                .background(VelvetBlack)
                                .padding(horizontal = if (isCompact) 8.dp else 16.dp, vertical = 12.dp)
                                .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars)
                        ) {
                            MessageInputField(
                                value = messageInput,
                                onValueChange = viewModel::updateMessageInput,
                                onSend = viewModel::sendMessage,
                                placeholder = "Message #${activeChannel.name}",
                                isCompact = isCompact,
                                replyingTo = replyingTo,
                                onCancelReply = viewModel::cancelReply,
                                onAttachmentClick = {
                                    imagePicker.launch("image/*")
                                }
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
                        .background(VelvetDark)
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
                                        text = selectedServer?.name ?: "Channels",
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
                                onNavigateToVoiceChannel = {
                                    channelDrawerOpen = false
                                    onNavigateToVoiceChannel(it)
                                },
                                unreadCountsByChannel = unreadCountsByChannel,
                                favoriteChannelIds = favoriteChannelIds,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
            }
            
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
            text = text,
            style = FluxerTextStyles.statusIndicator,
            color = color
        )
    }
}
