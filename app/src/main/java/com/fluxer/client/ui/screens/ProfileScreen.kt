package com.fluxer.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fluxer.client.data.model.UserProfile
import com.fluxer.client.data.model.UserStatus
import com.fluxer.client.ui.theme.*
import com.fluxer.client.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: String? = null,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isCurrentUser by viewModel.isCurrentUser.collectAsState()
    val showEditDialog by viewModel.showEditDialog.collectAsState()
    
    LaunchedEffect(userId) {
        viewModel.loadProfile(userId)
    }
    
    Scaffold(
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
            profile?.let { userProfile ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Purple Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF5865F2),
                                        Color(0xFF4752C4)
                                    )
                                )
                            )
                    ) {
                        // Back button
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.TopStart)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                        
                        // Settings button (for current user)
                        if (isCurrentUser) {
                            IconButton(
                                onClick = { /* TODO: Settings */ },
                                modifier = Modifier
                                    .padding(16.dp)
                                    .align(Alignment.TopEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = TextPrimary
                                )
                            }
                        }
                    }
                    
                    // Profile content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Avatar overlapping banner
                        Box(
                            modifier = Modifier.offset(y = (-48).dp)
                        ) {
                            ProfileAvatarLarge(
                                avatarUrl = userProfile.avatarUrl,
                                username = userProfile.username,
                                status = userProfile.status
                            )
                        }
                        
                        // Username and discriminator
                        Column(
                            modifier = Modifier.offset(y = (-32).dp)
                        ) {
                            Text(
                                text = userProfile.displayName ?: userProfile.username,
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = "${userProfile.username}#${userProfile.discriminator}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextMuted
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Edit Profile Button (for current user)
                            if (isCurrentUser) {
                                Surface(
                                    onClick = { viewModel.showEditDialog() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    color = Color(0xFF5865F2)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = TextPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Edit Profile",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // About Me Section
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = VelvetSurface
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "About me",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    if (userProfile.bio.isNullOrBlank()) {
                                        Text(
                                            text = "i eat cement", // Placeholder like screenshot
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = TextSecondary
                                        )
                                    } else {
                                        Text(
                                            text = userProfile.bio,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = TextSecondary
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Member Since
                                    Text(
                                        text = "Fluxer Member Since",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    Text(
                                        text = formatMemberSince(userProfile.createdAt),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextSecondary
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Note Section
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = VelvetSurface
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Note",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        
                                        IconButton(
                                            onClick = { /* TODO: Edit note */ },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit note",
                                                tint = TextMuted,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    
                                    Text(
                                        text = "(only visible to you)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = "No note yet.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextMuted
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Actions for other users
                            if (!isCurrentUser) {
                                OtherUserActions(
                                    onMessage = { /* TODO */ },
                                    onCall = { /* TODO */ },
                                    onFriendRequest = { /* TODO */ }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
    
    if (showEditDialog) {
        EditProfileDialog(
            currentProfile = profile,
            onDismiss = { viewModel.hideEditDialog() },
            onSave = { displayName, bio, customStatus ->
                viewModel.updateProfile(displayName, bio, customStatus)
            }
        )
    }
}

@Composable
private fun ProfileAvatarLarge(
    avatarUrl: String?,
    username: String,
    status: UserStatus
) {
    Box {
        Surface(
            modifier = Modifier
                .size(96.dp)
                .border(6.dp, VelvetBlack, CircleShape),
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
                        style = MaterialTheme.typography.displaySmall,
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
                .size(24.dp)
                .align(Alignment.BottomEnd)
                .offset(x = (-4).dp, y = (-4).dp)
                .background(VelvetBlack, CircleShape)
                .padding(3.dp)
                .background(statusColor, CircleShape)
        )
    }
}

@Composable
private fun OtherUserActions(
    onMessage: () -> Unit,
    onCall: () -> Unit,
    onFriendRequest: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "ACTIONS",
            style = MaterialTheme.typography.labelMedium,
            color = PhantomRed,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = VelvetSurface
        ) {
            Column {
                ActionItem(
                    icon = Icons.Default.Message,
                    title = "Send Message",
                    onClick = onMessage
                )
                ActionItem(
                    icon = Icons.Default.Call,
                    title = "Start Voice Call",
                    onClick = onCall
                )
                ActionItem(
                    icon = Icons.Default.PersonAdd,
                    title = "Add Friend",
                    onClick = onFriendRequest
                )
            }
        }
    }
}

@Composable
private fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PhantomRed,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary
        )
    }
}

@Composable
private fun EditProfileDialog(
    currentProfile: UserProfile?,
    onDismiss: () -> Unit,
    onSave: (String?, String?, String?) -> Unit
) {
    var displayName by remember { mutableStateOf(currentProfile?.displayName ?: "") }
    var bio by remember { mutableStateOf(currentProfile?.bio ?: "") }
    var customStatus by remember { mutableStateOf(currentProfile?.customStatus ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VelvetSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("Edit Profile") },
        text = {
            Column {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PhantomRed,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio", color = TextMuted) },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PhantomRed,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customStatus,
                    onValueChange = { customStatus = it },
                    label = { Text("Custom Status", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PhantomRed,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        displayName.takeIf { it.isNotBlank() },
                        bio.takeIf { it.isNotBlank() },
                        customStatus.takeIf { it.isNotBlank() }
                    )
                    onDismiss()
                }
            ) {
                Text("Save", color = PhantomRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

private fun formatMemberSince(isoString: String?): String {
    return try {
        val instant = java.time.Instant.parse(isoString)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")
            .withZone(java.time.ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        "Feb 10, 2026" // Placeholder
    }
}
