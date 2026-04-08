package com.fluxer.client.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.os.StatFs
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fluxer.client.ui.theme.*
import com.fluxer.client.ui.viewmodel.SettingsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    // Storage stats
    var appCacheSize by remember { mutableStateOf(0L) }
    var appDataSize by remember { mutableStateOf(0L) }
    var totalStorage by remember { mutableStateOf(0L) }
    var freeStorage by remember { mutableStateOf(0L) }
    
    // Media stats
    var downloadedImages by remember { mutableStateOf(0) }
    var downloadedFiles by remember { mutableStateOf(0) }
    
    // Network usage
    var autoDownloadMobile by remember { mutableStateOf(false) }
    var autoDownloadWifi by remember { mutableStateOf(true) }
    var mediaQuality by remember { mutableStateOf(MediaQuality.AUTO) }
    
    // Calculate storage on launch
    LaunchedEffect(Unit) {
        appCacheSize = calculateCacheSize(context)
        appDataSize = calculateDataSize(context)
        val storageStats = getStorageStats()
        totalStorage = storageStats.first
        freeStorage = storageStats.second
    }
    
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Storage & Data",
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
            
            // Storage Usage Overview
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = VelvetSurface,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Storage Usage",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Storage bar
                    val usedStorage = totalStorage - freeStorage
                    val usagePercent = if (totalStorage > 0) {
                        (usedStorage.toFloat() / totalStorage.toFloat())
                    } else 0f
                    
                    LinearProgressIndicator(
                        progress = { usagePercent.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = when {
                            usagePercent > 0.9f -> DndRed
                            usagePercent > 0.75f -> WarningOrange
                            else -> PhantomRed
                        },
                        trackColor = VelvetDark,
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${formatBytes(usedStorage)} used",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "${formatBytes(totalStorage)} total",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // App Storage Details
            SettingsSection(title = "App Storage") {
                StorageItem(
                    icon = Icons.Default.Storage,
                    title = "App Cache",
                    subtitle = "Temporary files and images",
                    size = appCacheSize,
                    onClear = { showClearCacheDialog = true }
                )
                
                SettingsDivider()
                
                StorageItem(
                    icon = Icons.Default.DataObject,
                    title = "App Data",
                    subtitle = "Messages, settings, and database",
                    size = appDataSize,
                    onClear = { showClearDataDialog = true },
                    isDestructive = true
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Downloaded Media
            SettingsSection(title = "Downloaded Media") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = "Images & Videos",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$downloadedImages items",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
                    
                    TextButton(
                        onClick = { /* Clear downloaded images */ }
                    ) {
                        Text("Clear", color = PhantomRed)
                    }
                }
                
                SettingsDivider()
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column {
                            Text(
                                text = "Files",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$downloadedFiles items",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
                    
                    TextButton(
                        onClick = { /* Clear downloaded files */ }
                    ) {
                        Text("Clear", color = PhantomRed)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Data Usage
            SettingsSection(title = "Data Usage") {
                // Auto-download on mobile
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { autoDownloadMobile = !autoDownloadMobile }
                        .padding(16.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SignalCellular4Bar,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-download on Mobile Data",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Download media using mobile data",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                        
                        Switch(
                            checked = autoDownloadMobile,
                            onCheckedChange = { autoDownloadMobile = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PhantomRed,
                                checkedTrackColor = PhantomRed.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
                
                SettingsDivider()
                
                // Auto-download on WiFi
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { autoDownloadWifi = !autoDownloadWifi }
                        .padding(16.dp),
                    color = Color.Transparent
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-download on Wi-Fi",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Download media using Wi-Fi",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                        
                        Switch(
                            checked = autoDownloadWifi,
                            onCheckedChange = { autoDownloadWifi = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PhantomRed,
                                checkedTrackColor = PhantomRed.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
                
                SettingsDivider()
                
                // Media Quality
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Media Quality",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MediaQualityOption(
                            quality = MediaQuality.LOW,
                            isSelected = mediaQuality == MediaQuality.LOW,
                            onClick = { mediaQuality = MediaQuality.LOW }
                        )
                        MediaQualityOption(
                            quality = MediaQuality.AUTO,
                            isSelected = mediaQuality == MediaQuality.AUTO,
                            onClick = { mediaQuality = MediaQuality.AUTO }
                        )
                        MediaQualityOption(
                            quality = MediaQuality.HIGH,
                            isSelected = mediaQuality == MediaQuality.HIGH,
                            onClick = { mediaQuality = MediaQuality.HIGH }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Clear Cache Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            containerColor = VelvetSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Clear Cache") },
            text = { 
                Text("This will clear all cached images and temporary files (${formatBytes(appCacheSize)}). Your messages and settings will not be affected.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearCache(context)
                        appCacheSize = calculateCacheSize(context)
                        showClearCacheDialog = false
                    }
                ) {
                    Text("Clear", color = PhantomRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
    
    // Clear Data Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            containerColor = VelvetSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Clear App Data", color = DndRed) },
            text = { 
                Text("This will delete all app data including messages, settings, and your account info. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearAppData(context)
                        appDataSize = calculateDataSize(context)
                        showClearDataDialog = false
                    }
                ) {
                    Text("Clear All Data", color = DndRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

enum class MediaQuality {
    LOW, AUTO, HIGH
}

@Composable
private fun StorageItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    size: Long,
    onClear: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) DndRed else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isDestructive) DndRed else TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatBytes(size),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(end = 8.dp)
            )
            
            TextButton(onClick = onClear) {
                Text(
                    "Clear",
                    color = if (isDestructive) DndRed else PhantomRed
                )
            }
        }
    }
}

@Composable
private fun MediaQualityOption(
    quality: MediaQuality,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val title = when (quality) {
        MediaQuality.LOW -> "Low"
        MediaQuality.AUTO -> "Auto"
        MediaQuality.HIGH -> "High"
    }
    
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(title) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PhantomRed,
            selectedLabelColor = TextPrimary,
            containerColor = VelvetSurface,
            labelColor = TextSecondary
        )
    )
}

// Helper functions
private fun calculateCacheSize(context: Context): Long {
    var size = 0L
    context.cacheDir?.let { size += it.length() }
    context.externalCacheDir?.let { size += it.length() }
    return size
}

private fun calculateDataSize(context: Context): Long {
    return context.filesDir?.let { dir ->
        calculateDirectorySize(dir)
    } ?: 0L
}

private fun calculateDirectorySize(dir: File): Long {
    var size = 0L
    dir.listFiles()?.forEach { file ->
        size += if (file.isDirectory) {
            calculateDirectorySize(file)
        } else {
            file.length()
        }
    }
    return size
}

private fun clearCache(context: Context) {
    context.cacheDir?.deleteRecursively()
    context.externalCacheDir?.deleteRecursively()
}

private fun clearAppData(context: Context) {
    context.filesDir?.deleteRecursively()
    context.cacheDir?.deleteRecursively()
}

private fun getStorageStats(): Pair<Long, Long> {
    val stat = StatFs(android.os.Environment.getDataDirectory().path)
    val blockSize = stat.blockSizeLong
    val totalBlocks = stat.blockCountLong
    val availableBlocks = stat.availableBlocksLong
    
    val total = totalBlocks * blockSize
    val available = availableBlocks * blockSize
    
    return Pair(total, available)
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
