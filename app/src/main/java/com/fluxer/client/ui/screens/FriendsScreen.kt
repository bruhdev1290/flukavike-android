package com.fluxer.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fluxer.client.data.model.Relationship
import com.fluxer.client.data.model.User
import com.fluxer.client.ui.theme.*
import com.fluxer.client.ui.viewmodel.FriendsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onDismiss: () -> Unit,
    onStartDm: (String) -> Unit = {},
    viewModel: FriendsViewModel = hiltViewModel()
) {
    val friends by viewModel.friends.collectAsState()
    val pendingIncoming by viewModel.pendingIncoming.collectAsState()
    val pendingOutgoing by viewModel.pendingOutgoing.collectAsState()
    val blocked by viewModel.blocked.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val addFriendInput by viewModel.addFriendInput.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Friends (${friends.size})",
        "Pending (${pendingIncoming.size + pendingOutgoing.size})",
        "Blocked (${blocked.size})"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .background(VelvetDark)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(VelvetBlack)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.People,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Friends",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
            }
        }

        // Add Friend bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(VelvetBlack)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = addFriendInput,
                onValueChange = viewModel::updateAddFriendInput,
                placeholder = { Text("Add by username", color = TextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PhantomRed,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = PhantomRed
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = viewModel::sendFriendRequest,
                enabled = addFriendInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PhantomRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Send", color = TextPrimary)
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = VelvetBlack,
            contentColor = PhantomRed,
            indicator = { tabPositions ->
                Box(
                    Modifier
                        .tabIndicatorOffset(tabPositions[selectedTab])
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(PhantomRed)
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selectedTab == index) PhantomRed else TextMuted
                        )
                    }
                )
            }
        }

        // Error
        error?.let {
            Text(
                text = it,
                color = DndRed,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Content
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PhantomRed)
            }
        } else {
            val list = when (selectedTab) {
                0 -> friends
                1 -> pendingIncoming + pendingOutgoing
                else -> blocked
            }

            if (list.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = when (selectedTab) {
                            0 -> "No friends yet. Add someone!"
                            1 -> "No pending requests"
                            else -> "No blocked users"
                        },
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(list, key = { it.id }) { relationship ->
                        RelationshipRow(
                            relationship = relationship,
                            tab = selectedTab,
                            onMessage = { onStartDm(relationship.user.id) },
                            onAccept = { viewModel.sendFriendRequest() },
                            onRemove = { viewModel.removeRelationship(relationship.user.id) },
                            onBlock = { viewModel.blockUser(relationship.user.id) },
                            onUnblock = { viewModel.removeRelationship(relationship.user.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RelationshipRow(
    relationship: Relationship,
    tab: Int,
    onMessage: () -> Unit,
    onAccept: () -> Unit,
    onRemove: () -> Unit,
    onBlock: () -> Unit,
    onUnblock: () -> Unit
) {
    val user = relationship.user
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        if (!user.avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(VelvetSurface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.displayName?.takeIf { it.isNotBlank() } ?: user.username.take(1).uppercase(),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName?.takeIf { it.isNotBlank() } ?: user.username,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = when (relationship.type) {
                    1 -> "Friend"
                    2 -> "Blocked"
                    3 -> "Incoming request"
                    4 -> "Outgoing request"
                    else -> ""
                },
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }

        // Action buttons
        when (tab) {
            0 -> {
                IconButton(onClick = onMessage) {
                    Icon(Icons.Default.Message, contentDescription = "Message", tint = TextSecondary)
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.PersonRemove, contentDescription = "Remove friend", tint = TextMuted)
                }
            }
            1 -> {
                if (relationship.type == 3) {
                    IconButton(onClick = onAccept) {
                        Icon(Icons.Default.Check, contentDescription = "Accept", tint = OnlineGreen)
                    }
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = "Decline/Cancel", tint = DndRed)
                }
            }
            2 -> {
                IconButton(onClick = onUnblock) {
                    Icon(Icons.Default.LockOpen, contentDescription = "Unblock", tint = TextSecondary)
                }
            }
        }
    }
    HorizontalDivider(color = BorderSubtle.copy(alpha = 0.3f), thickness = 0.5.dp)
}
