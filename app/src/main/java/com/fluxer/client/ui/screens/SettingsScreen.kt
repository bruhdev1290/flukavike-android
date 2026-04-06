package com.fluxer.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fluxer.client.ui.theme.*
import com.fluxer.client.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
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
                // YOUR ACCOUNT Section
                SettingsSectionHeader("YOUR ACCOUNT")
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = VelvetSurface,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        SettingsMenuItem(
                            icon = Icons.Default.Person,
                            title = "Account",
                            showArrow = true,
                            onClick = { /* TODO */ }
                        )
                        SettingsDivider()
                        SettingsMenuItem(
                            icon = Icons.Default.Security,
                            title = "Security & Login",
                            showArrow = true,
                            onClick = { /* TODO */ }
                        )
                        SettingsDivider()
                        SettingsMenuItem(
                            icon = Icons.Default.Star,
                            title = "Fluxer Plutonium",
                            showArrow = true,
                            onClick = { /* TODO */ }
                        )
                        SettingsDivider()
                        SettingsMenuItem(
                            icon = Icons.Default.CardGiftcard,
                            title = "Gifts & Codes",
                            showArrow = true,
                            onClick = { /* TODO */ }
                        )
                        SettingsDivider()
                        SettingsMenuItem(
                            icon = Icons.Default.PrivacyTip,
                            title = "Privacy Dashboard",
                            showArrow = true,
                            onClick = { /* TODO */ }
                        )
                        SettingsDivider()
                        SettingsMenuItem(
                            icon = Icons.Default.Apps,
                            title = "Authorized Apps",
                            showArrow = true,
                            onClick = { /* TODO */ }
                        )
                        SettingsDivider()
                        SettingsMenuItem(
                            icon = Icons.Default.Block,
                            title = "Blocked Users",
                            showArrow = true,
                            onClick = { /* TODO */ }
                        )
                        SettingsDivider()
                        SettingsMenuItem(
                            icon = Icons.Default.Devices,
                            title = "Linked Devices",
                            showArrow = true,
                            onClick = { /* TODO */ }
                        )
                        SettingsDivider()
                        SettingsMenuItem(
                            icon = Icons.Default.People,
                            title = "Connections",
                            showArrow = true,
                            onClick = { /* TODO */ }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // APPLICATION Section
                SettingsSectionHeader("APPLICATION")
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = VelvetSurface,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        SettingsMenuItem(
                            icon = Icons.Default.Palette,
                            title = "Look & Feel",
                            showArrow = true,
                            onClick = { /* TODO */ }
                        )
                        SettingsDivider()
                        SettingsMenuItem(
                            icon = Icons.Default.Accessibility,
                            title = "Accessibility",
                            showArrow = true,
                            onClick = { /* TODO */ }
                        )
                        SettingsDivider()
                        SettingsMenuItem(
                            icon = Icons.Default.Message,
                            title = "Messages & Media",
                            showArrow = true,
                            onClick = onNavigateToNotifications
                        )
                        SettingsDivider()
                        SettingsMenuItem(
                            icon = Icons.Default.Notifications,
                            title = "Notifications",
                            showArrow = true,
                            onClick = onNavigateToNotifications
                        )
                        SettingsDivider()
                        SettingsMenuItem(
                            icon = Icons.Default.Language,
                            title = "Language",
                            showArrow = true,
                            onClick = { /* TODO */ }
                        )
                        SettingsDivider()
                        SettingsMenuItem(
                            icon = Icons.Default.Storage,
                            title = "Storage",
                            showArrow = true,
                            onClick = { /* TODO */ }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // SUPPORT Section
                SettingsSectionHeader("SUPPORT")
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = VelvetSurface,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        SettingsMenuItem(
                            icon = Icons.Default.Help,
                            title = "Help & Support",
                            showArrow = true,
                            onClick = { /* TODO */ }
                        )
                        SettingsDivider()
                        SettingsMenuItem(
                            icon = Icons.Default.Info,
                            title = "About",
                            showArrow = true,
                            onClick = { /* TODO */ }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Logout Button
                TextButton(
                    onClick = { /* TODO */ },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = "Log Out",
                        color = DndRed,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = TextMuted,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
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
private fun SettingsDivider() {
    Divider(
        color = BorderSubtle.copy(alpha = 0.5f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 56.dp)
    )
}
