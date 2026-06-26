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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.fluxer.client.data.model.UserProfile
import com.fluxer.client.data.model.UserStatus
import com.fluxer.client.ui.components.FluxerEmptyState
import com.fluxer.client.ui.components.FluxerLoadingState
import com.fluxer.client.ui.components.FluxerPageScaffold
import com.fluxer.client.ui.components.FluxerPanel
import com.fluxer.client.ui.components.FluxerSectionTitle
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
    
    // Get current user info for fallback
    val currentUser = profile
    
    FluxerPageScaffold(
        title = "You",
        subtitle = currentUser?.username?.let { "@$it" } ?: "Account",
        onBack = onBack
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    FluxerLoadingState("Loading profile")
                }
                currentUser != null -> {
                    ProfileContent(
                        profile = currentUser,
                        isCurrentUser = isCurrentUser,
                        onEditClick = { viewModel.showEditDialog() },
                        onLogout = onLogout
                    )
                }
                else -> {
                    FallbackProfileContent(
                        onLogout = onLogout
                    )
                }
            }
        }
    }
    
    if (showEditDialog && profile != null) {
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
private fun ProfileContent(
    profile: UserProfile,
    isCurrentUser: Boolean,
    onEditClick: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        FluxerPanel(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(VelvetSurface, CircleShape)
                        .border(2.dp, BorderSubtle, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (profile.avatarUrl != null) {
                        AsyncImage(
                            model = profile.avatarUrl,
                            contentDescription = profile.username,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = profile.username.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.displayName ?: profile.username,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "@${profile.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = profile.customStatus?.takeIf { it.isNotBlank() } ?: statusLabel(profile.status),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        FluxerSectionTitle(title = "About")
        FluxerPanel(modifier = Modifier.fillMaxWidth(), tonal = true) {
            Text(
                text = profile.bio ?: "No bio yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = if (profile.bio.isNullOrBlank()) TextMuted else TextSecondary,
                modifier = Modifier.padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        FluxerSectionTitle(title = "Account")
        FluxerPanel(modifier = Modifier.fillMaxWidth(), tonal = true) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Member since",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatMemberSince(profile.createdAt),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Action Buttons
        if (isCurrentUser) {
            // Edit Profile Button
            Button(
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PhantomRed
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit Profile",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Logout Button
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = DndRed
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(DndRed)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Log Out",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            // Message Button for other users
            Button(
                onClick = { /* TODO */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PhantomRed
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Message,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Send Message",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun FallbackProfileContent(
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        FluxerEmptyState(
            title = "Profile unavailable",
            body = "We couldn't load your account details right now.",
            icon = Icons.Default.Person,
            modifier = Modifier.weight(1f)
        )
        
        // Logout Button
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = DndRed
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(DndRed)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Log Out",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
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

private fun statusLabel(status: UserStatus): String = when (status) {
    UserStatus.ONLINE -> "Online"
    UserStatus.AWAY -> "Away"
    UserStatus.DND -> "Do not disturb"
    UserStatus.OFFLINE -> "Offline"
}

private fun formatMemberSince(isoString: String?): String {
    return try {
        val instant = java.time.Instant.parse(isoString)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")
            .withZone(java.time.ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        "Unknown"
    }
}
