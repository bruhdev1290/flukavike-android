package com.fluxer.client.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fluxer.client.data.local.FontScale
import com.fluxer.client.ui.theme.*
import com.fluxer.client.ui.viewmodel.AppPreferencesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilityScreen(
    onBack: () -> Unit,
    viewModel: AppPreferencesViewModel = hiltViewModel()
) {
    val accentColor by viewModel.accentColor.collectAsState()
    val fontScale by viewModel.fontScale.collectAsState()

    var boldText by remember { mutableStateOf(false) }
    var reduceMotion by remember { mutableStateOf(false) }
    var highContrast by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Accessibility",
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

            // Text Size
            SettingsSection(title = "Text Size") {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = "Adjusts all text throughout the app",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FontScale.entries.forEach { scale ->
                            val isSelected = fontScale == scale
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setFontScale(scale) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) accentColor.copy(alpha = 0.15f) else VelvetMid,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, accentColor) else null
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Aa",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSelected) accentColor else TextSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = scale.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) accentColor else TextMuted
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live preview
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = VelvetMid
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Preview",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = "This is how messages look at this size.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = "andrew · Just now",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Readability
            SettingsSection(title = "Readability") {
                AccessibilityToggle(
                    icon = Icons.Default.FormatBold,
                    title = "Bold Text",
                    subtitle = "Makes all text heavier for easier reading",
                    checked = boldText,
                    accentColor = accentColor,
                    onCheckedChange = { boldText = it }
                )
                SettingsDivider()
                AccessibilityToggle(
                    icon = Icons.Default.Contrast,
                    title = "Increase Contrast",
                    subtitle = "Sharper borders and higher text contrast",
                    checked = highContrast,
                    accentColor = accentColor,
                    onCheckedChange = { highContrast = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Motion
            SettingsSection(title = "Motion") {
                AccessibilityToggle(
                    icon = Icons.Default.Animation,
                    title = "Reduce Motion",
                    subtitle = "Limits animations and transitions",
                    checked = reduceMotion,
                    accentColor = accentColor,
                    onCheckedChange = { reduceMotion = it }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AccessibilityToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextPrimary,
                checkedTrackColor = accentColor
            )
        )
    }
}
