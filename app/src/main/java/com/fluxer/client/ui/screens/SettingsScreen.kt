package com.fluxer.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fluxer.client.data.model.FontSize as SettingsFontSize
import com.fluxer.client.data.model.MessageDisplayMode as SettingsMessageDisplayMode
import com.fluxer.client.data.model.ThemeMode as SettingsThemeMode
import com.fluxer.client.ui.components.FluxerDivider
import com.fluxer.client.ui.components.FluxerLoadingState
import com.fluxer.client.ui.components.FluxerPageScaffold
import com.fluxer.client.ui.components.FluxerPanel
import com.fluxer.client.ui.theme.*
import com.fluxer.client.data.local.GestureSensitivity
import com.fluxer.client.ui.viewmodel.AppPreferencesViewModel
import com.fluxer.client.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToAccessibility: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    prefsViewModel: AppPreferencesViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val notice by viewModel.notice.collectAsState()
    val biometricLockEnabled by prefsViewModel.biometricLockEnabled.collectAsState()
    val accentColor by prefsViewModel.accentColor.collectAsState()
    val gesturesEnabled by prefsViewModel.gesturesEnabled.collectAsState()
    val gestureSensitivity by prefsViewModel.gestureSensitivity.collectAsState()
    
    FluxerPageScaffold(
        title = "Settings",
        subtitle = "Account, privacy, app preferences, and support",
        onBack = onBack
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                FluxerLoadingState("Loading settings")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingsSection("Your Account") {
                        SettingsMenuItem(
                            icon = Icons.Default.Person,
                            title = "Profile",
                            subtitle = "Display name, bio, avatar, and status",
                            onClick = onNavigateToAccount
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsSection("Notifications") {
                        SettingsMenuItem(
                            icon = Icons.Default.Tune,
                            title = "UnifiedPush & Alerts",
                            subtitle = "Distributor, alert types, and previews",
                            onClick = onNavigateToNotifications
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsSection("App") {
                        SettingsMenuItem(
                            icon = Icons.Default.Palette,
                            title = "Appearance",
                            subtitle = "Accent color, theme presets, and display",
                            onClick = onNavigateToAppearance
                        )
                        SettingsDivider()
                        SettingsMenuItem(
                            icon = Icons.Default.Accessibility,
                            title = "Accessibility",
                            subtitle = "Font size, text scale, and readability",
                            onClick = onNavigateToAccessibility
                        )
                        SettingsDivider()
                        SettingsMenuItem(
                            icon = Icons.Default.Storage,
                            title = "Storage & Data",
                            subtitle = "Cache, downloads, and media storage",
                            onClick = onNavigateToStorage
                        )
                        SettingsDivider()
                        // Gestures toggle + sensitivity inline
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { prefsViewModel.setGesturesEnabled(!gesturesEnabled) }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = VelvetMid
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.SwipeRight,
                                            contentDescription = null,
                                            tint = if (gesturesEnabled) accentColor else TextSecondary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Swipe Gestures",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Swipe to open channel list, close drawers, and navigate",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextMuted
                                    )
                                }
                                Switch(
                                    checked = gesturesEnabled,
                                    onCheckedChange = { prefsViewModel.setGesturesEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = TextPrimary,
                                        checkedTrackColor = accentColor
                                    )
                                )
                            }
                            if (gesturesEnabled) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 72.dp, end = 16.dp, bottom = 14.dp)
                                ) {
                                    Text(
                                        text = "Sensitivity",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TextMuted
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        GestureSensitivity.entries.forEach { option ->
                                            val selected = gestureSensitivity == option
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = if (selected) accentColor.copy(alpha = 0.18f) else VelvetMid,
                                                modifier = Modifier.clickable { prefsViewModel.setGestureSensitivity(option) }
                                            ) {
                                                Text(
                                                    text = option.displayName,
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (selected) accentColor else TextMuted,
                                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsSection("Privacy & Security") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { prefsViewModel.setBiometricLock(!biometricLockEnabled) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Surface(
                                modifier = Modifier.size(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = com.fluxer.client.ui.theme.VelvetMid
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = if (biometricLockEnabled) accentColor else com.fluxer.client.ui.theme.TextSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Biometric Lock",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = com.fluxer.client.ui.theme.TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Require biometric authentication when opening the app",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = com.fluxer.client.ui.theme.TextMuted
                                )
                            }
                            Switch(
                                checked = biometricLockEnabled,
                                onCheckedChange = { prefsViewModel.setBiometricLock(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = com.fluxer.client.ui.theme.TextPrimary,
                                    checkedTrackColor = accentColor
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingsSection("Support") {
                        SettingsMenuItem(
                            icon = Icons.AutoMirrored.Filled.Help,
                            title = "Help & Support",
                            subtitle = "Get help with Flukavike",
                            onClick = onNavigateToSupport
                        )
                        SettingsDivider()
                        SettingsMenuItem(
                            icon = Icons.Default.Info,
                            title = "About",
                            subtitle = "Version 1.0.0",
                            onClick = onNavigateToAbout
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    LogoutRow(onLogout = onLogout)

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            notice?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = VelvetSurface,
                    contentColor = TextPrimary,
                    action = {
                        TextButton(onClick = viewModel::clearNotice) {
                            Text("Dismiss", color = PhantomRed)
                        }
                    }
                ) {
                    Text(message)
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        
        FluxerPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(10.dp),
            color = VelvetMid
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingsDivider() {
    FluxerDivider()
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIcon(icon)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = TextPrimary, checkedTrackColor = PhantomRed)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsChoiceRow(
    icon: ImageVector,
    title: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        Row(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsIcon(icon)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
                Text(selected, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted)
        }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(VelvetSurface)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = TextPrimary) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun LogoutRow(onLogout: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onLogout),
        color = VelvetSurface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = DndRed,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Log Out",
                style = MaterialTheme.typography.bodyLarge,
                color = DndRed,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SettingsIcon(icon: ImageVector) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(10.dp),
        color = VelvetMid
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private fun themeLabel(theme: SettingsThemeMode): String = when (theme) {
    SettingsThemeMode.LIGHT -> "Light"
    SettingsThemeMode.DARK -> "Dark"
    SettingsThemeMode.SYSTEM -> "System"
    SettingsThemeMode.AMOLED -> "AMOLED"
}

private fun messageDisplayLabel(mode: SettingsMessageDisplayMode): String = when (mode) {
    SettingsMessageDisplayMode.COMFORTABLE -> "Comfortable"
    SettingsMessageDisplayMode.COMPACT -> "Compact"
}

private fun fontSizeLabel(size: SettingsFontSize): String = when (size) {
    SettingsFontSize.SMALL -> "Small"
    SettingsFontSize.MEDIUM -> "Medium"
    SettingsFontSize.LARGE -> "Large"
}
