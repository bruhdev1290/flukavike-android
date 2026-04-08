package com.fluxer.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fluxer.client.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    onBack: () -> Unit
) {
    var selectedTheme by remember { mutableStateOf(ThemeMode.SYSTEM) }
    var useCompactMode by remember { mutableStateOf(false) }
    var showAvatarBorders by remember { mutableStateOf(true) }
    var animationsEnabled by remember { mutableStateOf(true) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Appearance",
                        style = MaterialTheme.typography.titleLarge,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Theme Selection
            SettingsSection(title = "Theme") {
                ThemeOption(
                    icon = Icons.Default.LightMode,
                    title = "Light",
                    isSelected = selectedTheme == ThemeMode.LIGHT,
                    onClick = { selectedTheme = ThemeMode.LIGHT }
                )
                SettingsDivider()
                ThemeOption(
                    icon = Icons.Default.DarkMode,
                    title = "Dark",
                    isSelected = selectedTheme == ThemeMode.DARK,
                    onClick = { selectedTheme = ThemeMode.DARK }
                )
                SettingsDivider()
                ThemeOption(
                    icon = Icons.Default.PhoneAndroid,
                    title = "System Default",
                    isSelected = selectedTheme == ThemeMode.SYSTEM,
                    onClick = { selectedTheme = ThemeMode.SYSTEM }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Accent Color
            SettingsSection(title = "Accent Color") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Choose your accent color",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AccentColorOption(
                            color = PhantomRed,
                            isSelected = true,
                            onClick = { }
                        )
                        AccentColorOption(
                            color = InfoCyan,
                            isSelected = false,
                            onClick = { }
                        )
                        AccentColorOption(
                            color = OnlineGreen,
                            isSelected = false,
                            onClick = { }
                        )
                        AccentColorOption(
                            color = WarningOrange,
                            isSelected = false,
                            onClick = { }
                        )
                        AccentColorOption(
                            color = Color(0xFF9B59B6),
                            isSelected = false,
                            onClick = { }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Display Options
            SettingsSection(title = "Display") {
                // Compact Mode Toggle
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { useCompactMode = !useCompactMode }
                        .padding(16.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Compact Mode",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Reduce padding for more content",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                        
                        Switch(
                            checked = useCompactMode,
                            onCheckedChange = { useCompactMode = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PhantomRed,
                                checkedTrackColor = PhantomRed.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
                
                SettingsDivider()
                
                // Avatar Borders Toggle
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAvatarBorders = !showAvatarBorders }
                        .padding(16.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Avatar Borders",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Show colored borders around avatars",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                        
                        Switch(
                            checked = showAvatarBorders,
                            onCheckedChange = { showAvatarBorders = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PhantomRed,
                                checkedTrackColor = PhantomRed.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
                
                SettingsDivider()
                
                // Animations Toggle
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { animationsEnabled = !animationsEnabled }
                        .padding(16.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Animations",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Enable UI animations",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                        
                        Switch(
                            checked = animationsEnabled,
                            onCheckedChange = { animationsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PhantomRed,
                                checkedTrackColor = PhantomRed.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

@Composable
private fun ThemeOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        color = Color.Transparent
    ) {
        Row(
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
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = PhantomRed,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun AccentColorOption(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.padding(3.dp)
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(VelvetBlack.copy(alpha = 0.3f))
            )
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
