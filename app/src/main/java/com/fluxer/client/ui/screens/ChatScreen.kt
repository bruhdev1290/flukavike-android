@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.fluxer.client.ui.screens

import android.net.Uri
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
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
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
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToUserProfile: (String) -> Unit = {},
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
    val searchResults by viewModel.searchResults.collectAsState()
    val replyingTo by viewModel.replyingTo.collectAsState()
    val favoriteChannelIds by viewModel.favoriteChannelIds.collectAsState()
    val unreadCountsByChannel by viewModel.unreadCountsByChannel.collectAsState()
    val typingUsers by viewModel.typingUsersInChannel.collectAsState()
    val pendingAttachmentUri by viewModel.pendingAttachmentUri.collectAsState()
    val pendingAttachmentMetadata by viewModel.pendingAttachmentMetadata.collectAsState()
    val isSendingMessage by viewModel.isSendingMessage.collectAsState()
    val uploadProgress by viewModel.uploadProgress.collectAsState()
    val pinnedMessages by viewModel.pinnedMessages.collectAsState()
    val showPinnedMessages by viewModel.showPinnedMessages.collectAsState()
    val guildMembers by viewModel.guildMembers.collectAsState()
    val showMemberList by viewModel.showMemberList.collectAsState()
    val invitePreview by viewModel.invitePreview.collectAsState()
    val editingMessage by viewModel.editingMessage.collectAsState()
    val activeChannel = selectedChannel

    var showCustomStatus by remember { mutableStateOf(false) }
    var showJoinServer by remember { mutableStateOf(false) }
    var showCreateServer by remember { mutableStateOf(false) }
    var showCreateChannel by remember { mutableStateOf(false) }
    var showAddServerMenu by remember { mutableStateOf(false) }
    var profileCardUser by remember { mutableStateOf<com.fluxer.client.data.model.User?>(null) }
    var profileCardUserId by remember { mutableStateOf("") }
    var viewingAttachment by remember { mutableStateOf<com.fluxer.client.data.model.Attachment?>(null) }
    
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
    
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        viewModel.setPendingAttachment(uri)
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
                if (isCompact && channels.isNotEmpty()) channelDrawerOpen = true
            },
            onHomeSelected = onNavigateToMessages,
            unreadCountsByGuild = unreadCountsByGuild,
            onJoinServer = { showAddServerMenu = true },
            modifier = Modifier.width(sidebarWidth),
            isCompact = isCompact
        )
        
        // Main Content Area with optional Channel List
        Box(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Channel List (persistent on larger screens)
                if (channels.isNotEmpty() && !isCompact) {
                    val channelListWidth = if (isMedium) 200.dp else 240.dp
                    Column(modifier = Modifier.width(channelListWidth)) {
                        if (selectedServer != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = { showCreateChannel = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Create channel",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        ChannelListContent(
                            channels = channels,
                            selectedChannelId = activeChannel?.id,
                            onChannelSelected = { viewModel.selectChannel(it) },
                            onNavigateToVoiceChannel = onNavigateToVoiceChannel,
                            unreadCountsByChannel = unreadCountsByChannel,
                            favoriteChannelIds = favoriteChannelIds,
                            modifier = Modifier.weight(1f)
                        )
                    }
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
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isCompact) {
                                    // Non-compact: all icons visible
                                    if (activeChannel != null) {
                                        FluxerIconButton(
                                            icon = if (isFavoriteChannel) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = if (isFavoriteChannel) "Remove favorite" else "Favorite channel",
                                            onClick = viewModel::toggleFavoriteForSelectedChannel,
                                            tint = if (isFavoriteChannel) AlertYellow else TextSecondary
                                        )
                                        FluxerIconButton(
                                            icon = Icons.Default.PushPin,
                                            contentDescription = "Pinned messages",
                                            onClick = {
                                                viewModel.loadPinnedMessages()
                                                viewModel.togglePinnedMessages()
                                            },
                                            tint = if (showPinnedMessages) TextPrimary else TextSecondary,
                                            containerColor = if (showPinnedMessages) PhantomRed.copy(alpha = 0.2f) else VelvetMid
                                        )
                                        if (selectedServer != null) {
                                            FluxerIconButton(
                                                icon = Icons.Default.Group,
                                                contentDescription = "Member list",
                                                onClick = viewModel::toggleMemberList,
                                                tint = if (showMemberList) TextPrimary else TextSecondary,
                                                containerColor = if (showMemberList) PhantomRed.copy(alpha = 0.2f) else VelvetMid
                                            )
                                        }
                                    }
                                    FluxerIconButton(
                                        icon = Icons.Default.AlternateEmail,
                                        contentDescription = "Mentions",
                                        onClick = onNavigateToNotifications
                                    )
                                    FluxerIconButton(
                                        icon = Icons.Default.Search,
                                        contentDescription = "Search",
                                        onClick = { viewModel.toggleSearch() },
                                        tint = if (isSearching) TextPrimary else TextSecondary,
                                        containerColor = if (isSearching) PhantomRed.copy(alpha = 0.2f) else VelvetMid
                                    )
                                    ConnectionStatus(connectionState)
                                } else {
                                    // Compact: search stays; secondary actions in ⋮ overflow
                                    FluxerIconButton(
                                        icon = Icons.Default.Search,
                                        contentDescription = "Search",
                                        onClick = { viewModel.toggleSearch() },
                                        tint = if (isSearching) TextPrimary else TextSecondary,
                                        containerColor = if (isSearching) PhantomRed.copy(alpha = 0.2f) else VelvetMid
                                    )
                                    var overflowOpen by remember { mutableStateOf(false) }
                                    Box {
                                        FluxerIconButton(
                                            icon = Icons.Default.MoreVert,
                                            contentDescription = "More options",
                                            onClick = { overflowOpen = true }
                                        )
                                        DropdownMenu(
                                            expanded = overflowOpen,
                                            onDismissRequest = { overflowOpen = false }
                                        ) {
                                            if (activeChannel != null) {
                                                DropdownMenuItem(
                                                    text = { Text(if (isFavoriteChannel) "Remove favorite" else "Favorite", color = TextPrimary) },
                                                    leadingIcon = { Icon(if (isFavoriteChannel) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = null, tint = if (isFavoriteChannel) AlertYellow else TextSecondary) },
                                                    onClick = { viewModel.toggleFavoriteForSelectedChannel(); overflowOpen = false }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Pinned messages", color = TextPrimary) },
                                                    leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null, tint = TextSecondary) },
                                                    onClick = { viewModel.loadPinnedMessages(); viewModel.togglePinnedMessages(); overflowOpen = false }
                                                )
                                                if (selectedServer != null) {
                                                    DropdownMenuItem(
                                                        text = { Text("Member list", color = TextPrimary) },
                                                        leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, tint = TextSecondary) },
                                                        onClick = { viewModel.toggleMemberList(); overflowOpen = false }
                                                    )
                                                }
                                            }
                                            DropdownMenuItem(
                                                text = { Text("Mentions", color = TextPrimary) },
                                                leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = TextSecondary) },
                                                onClick = { onNavigateToNotifications(); overflowOpen = false }
                                            )
                                        }
                                    }
                                }
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
                    if (isSearching) {
                        FluxerTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            hint = "Search in #${activeChannel?.displayName() ?: ""}",
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        )
                        if (searchQuery.isNotBlank() && searchResults.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                Text("No results", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (searchResults.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 400.dp)
                                    .background(VelvetDark)
                            ) {
                                items(searchResults, key = { it.id }) { msg ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.toggleSearch() }
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            Modifier
                                                .size(32.dp)
                                                .background(VelvetSurface, RoundedCornerShape(50)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                msg.author?.username?.take(1)?.uppercase() ?: "?",
                                                color = TextSecondary,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                msg.author?.username ?: "Unknown",
                                                color = PhantomRed,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                            )
                                            Text(
                                                msg.content.ifBlank { "(attachment)" },
                                                color = TextSecondary,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.3f), thickness = 0.5.dp)
                                }
                            }
                        }
                    }

                    // Messages List + optional member sidebar
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
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
                                                },
                                                onPin = { viewModel.pinMessage(message.id) },
                                                onEdit = if (isOwnMessage) { { viewModel.startEditMessage(message) } } else null,
                                                onAvatarClick = { _ ->
                                                    profileCardUser = message.author
                                                    profileCardUserId = message.authorId.takeIf { it.isNotBlank() }
                                                        ?: message.author?.id ?: ""
                                                },
                                                onViewAttachment = { attachment -> viewingAttachment = attachment }
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
                        
                        // Jump-to-unread / scroll-to-bottom pill
                        val showJumpToBottom by remember {
                            derivedStateOf { listState.firstVisibleItemIndex > 3 }
                        }
                        if (showJumpToBottom) {
                            Box(
                                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                            ) {
                                Surface(
                                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                                    color = VelvetSurface,
                                    shape = RoundedCornerShape(20.dp),
                                    shadowElevation = 4.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            if (selectedChannelUnread > 0) "${selectedChannelUnread} unread" else "Jump to bottom",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
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
                    if (!isCompact && showMemberList) {
                        MemberListPanel(
                            members = guildMembers,
                            modifier = Modifier.width(200.dp).fillMaxHeight()
                        )
                    }
                    } // end Row (messages + member panel)


                    // Message Input - with proper bottom insets handling
                    if (activeChannel != null) {
                        if (typingUsers.isNotEmpty()) {
                            TypingIndicator(
                                typingUsers = typingUsers,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(VelvetBlack)
                                    .padding(horizontal = if (isCompact) 8.dp else 16.dp).padding(bottom = 2.dp)
                            )
                        }
                        if (pendingAttachmentUri != null) {
                            AttachmentPreviewBar(
                                uri = pendingAttachmentUri!!,
                                metadata = pendingAttachmentMetadata,
                                progress = uploadProgress,
                                onRemove = { viewModel.setPendingAttachment(null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(VelvetBlack)
                                    .padding(horizontal = if (isCompact) 8.dp else 16.dp)
                            )
                        }
                        // Edit mode banner
                        if (editingMessage != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(PhantomRed.copy(alpha = 0.12f))
                                    .padding(horizontal = if (isCompact) 12.dp else 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = PhantomRed, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Editing message",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PhantomRed,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = viewModel::cancelEdit, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel edit", tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
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
                                onSend = if (editingMessage != null) viewModel::submitEdit else viewModel::sendMessage,
                                placeholder = if (editingMessage != null) "Edit message…" else "Message #${activeChannel.name}",
                                isCompact = isCompact,
                                replyingTo = if (editingMessage == null) replyingTo else null,
                                onCancelReply = viewModel::cancelReply,
                                onAttachmentClick = {
                                    imagePicker.launch("image/*")
                                },
                                hasAttachment = pendingAttachmentUri != null,
                                isSending = isSendingMessage
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

    // ── Feature sheets ───────────────────────────────────────────────────────

    profileCardUser?.let { user ->
        ProfileCardSheet(
            user = user,
            onDismiss = { profileCardUser = null; profileCardUserId = "" },
            onViewFullProfile = { onNavigateToUserProfile(profileCardUserId) },
            onSendMessage = null
        )
    }

    viewingAttachment?.let { attachment ->
        AttachmentViewerDialog(
            attachment = attachment,
            onDismiss = { viewingAttachment = null }
        )
    }

    if (isCompact && showMemberList) {
        MemberListPanel(
            members = guildMembers,
            onDismiss = { viewModel.toggleMemberList() }
        )
    }

    if (showPinnedMessages) {
        PinnedMessagesSheet(
            pinnedMessages = pinnedMessages,
            onDismiss = { viewModel.togglePinnedMessages() },
            onUnpin = { viewModel.unpinMessage(it) },
            onJumpTo = { viewModel.togglePinnedMessages() }
        )
    }

    if (showCustomStatus) {
        val currentUser2 by viewModel.currentUser.collectAsState()
        CustomStatusSheet(
            currentStatus = null,
            onDismiss = { showCustomStatus = false },
            onSave = { status ->
                viewModel.setCustomStatus(status)
                showCustomStatus = false
            }
        )
    }

    if (showAddServerMenu) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showAddServerMenu = false },
            containerColor = VelvetDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Add a Server",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showAddServerMenu = false; showJoinServer = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Join Server")
                    }
                    Button(
                        onClick = { showAddServerMenu = false; showCreateServer = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PhantomRed)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Create Server")
                    }
                }
            }
        }
    }

    if (showJoinServer) {
        JoinServerSheet(
            onDismiss = { showJoinServer = false; viewModel.clearInvitePreview() },
            invitePreview = invitePreview,
            onPreviewCode = { viewModel.previewInvite(it) },
            onJoin = { code ->
                viewModel.joinViaInvite(code) { showJoinServer = false }
            }
        )
    }

    if (showCreateServer) {
        CreateServerSheet(
            onDismiss = { showCreateServer = false },
            onCreate = { name ->
                viewModel.createServer(name) { showCreateServer = false }
            }
        )
    }

    if (showCreateChannel) {
        CreateChannelSheet(
            onDismiss = { showCreateChannel = false },
            onCreate = { name, isVoice ->
                viewModel.createChannel(name, isVoice) { showCreateChannel = false }
            }
        )
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

@Composable
private fun TypingIndicator(
    typingUsers: List<String>,
    modifier: Modifier = Modifier
) {
    val text = when (typingUsers.size) {
        1 -> "${typingUsers[0]} is typing…"
        2 -> "${typingUsers[0]} and ${typingUsers[1]} are typing…"
        3 -> "${typingUsers[0]}, ${typingUsers[1]}, and ${typingUsers[2]} are typing…"
        else -> "Several people are typing…"
    }
    AnimatedVisibility(
        visible = typingUsers.isNotEmpty(),
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = OfflineGray,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AttachmentPreviewBar(
    uri: Uri,
    metadata: com.fluxer.client.data.model.AttachmentMetadata?,
    progress: Float?,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(vertical = 6.dp)
            .background(VelvetSurface, RoundedCornerShape(8.dp))
            .padding(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = uri,
                contentDescription = "Attachment preview",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metadata?.displayName ?: uri.lastPathSegment ?: "attachment",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        metadata?.mimeType,
                        metadata?.sizeBytes?.let(::formatPreviewBytes)
                    ).joinToString(" • ").ifBlank { "Ready to send" },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp), enabled = progress == null) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        progress?.let {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { it.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = PhantomRed,
                trackColor = VelvetDark
            )
        }
    }
}

private fun formatPreviewBytes(size: Long): String {
    if (size < 1024) return "$size B"
    val kb = size / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format("%.1f MB", mb)
}
