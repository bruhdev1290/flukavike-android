package com.fluxer.client

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.fluxer.client.navigation.FluxerRoute
import com.fluxer.client.navigation.RoutePaths
import com.fluxer.client.navigation.ShellBranch
import com.fluxer.client.navigation.routeFromIntent
import com.fluxer.client.ui.screens.AboutScreen
import com.fluxer.client.ui.screens.AccountScreen
import com.fluxer.client.ui.screens.ActiveCallScreen
import com.fluxer.client.ui.screens.AppearanceScreen
import com.fluxer.client.ui.screens.ChatScreen
import com.fluxer.client.ui.screens.LanguageScreen
import com.fluxer.client.ui.screens.LoginScreen
import com.fluxer.client.ui.screens.MessagesScreen
import com.fluxer.client.ui.screens.NotificationCenterScreen
import com.fluxer.client.ui.screens.NotificationSettingsScreen
import com.fluxer.client.ui.screens.ProfileScreen
import com.fluxer.client.ui.screens.StarredChannelsScreen
import com.fluxer.client.ui.screens.StorageScreen
import com.fluxer.client.ui.screens.SupportScreen
import com.fluxer.client.ui.components.FluxerIconButton
import com.fluxer.client.ui.theme.BorderSubtle
import com.fluxer.client.ui.theme.FluxerTheme
import com.fluxer.client.ui.theme.PhantomRed
import com.fluxer.client.ui.theme.TextMuted
import com.fluxer.client.ui.theme.TextPrimary
import com.fluxer.client.ui.theme.VelvetBlack
import com.fluxer.client.ui.theme.VelvetDark
import com.fluxer.client.ui.viewmodel.AuthViewModel
import com.fluxer.client.ui.viewmodel.ShellUiState
import com.fluxer.client.ui.viewmodel.ShellViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val incomingRoute = mutableStateOf<FluxerRoute?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { (permission, granted) ->
            Timber.d("Permission $permission: $granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestPermissions()
        incomingRoute.value = routeFromIntent(intent)
        handleNotificationIntent(intent)

        setContent {
            FluxerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FluxerApp(initialRoute = incomingRoute.value)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingRoute.value = routeFromIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        ).forEach { permission ->
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(permission)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun handleNotificationIntent(intent: Intent?) {
        intent?.let {
            Timber.d(
                "Notification intent: type=${it.getStringExtra("notification_type")}, " +
                    "guild=${it.getStringExtra("guild_id")}, channel=${it.getStringExtra("channel_id")}, " +
                    "message=${it.getStringExtra("message_id")}, call=${it.getStringExtra("call_id")}, " +
                    "action=${it.getStringExtra("action")}"
            )
        }
    }
}

@Composable
fun FluxerApp(
    initialRoute: FluxerRoute?,
    shellViewModel: ShellViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val state by shellViewModel.uiState.collectAsState()
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(initialRoute) {
        shellViewModel.consumeIntentRoute(initialRoute)
    }

    LaunchedEffect(authState) {
        if (authState is com.fluxer.client.data.repository.AuthRepository.AuthState.Loading) {
            shellViewModel.navigate(FluxerRoute.Loading)
        }
    }

    when {
        !state.restored -> SplashScreen()
        state.activePath == RoutePaths.Login -> LoginScreen(
            onLoginSuccess = { shellViewModel.navigate(FluxerRoute.Me) }
        )
        state.activePath == RoutePaths.Loading -> SplashScreen()
        state.activePath == RoutePaths.Reconnecting -> ReconnectingScreen()
        !state.authenticated -> LoginScreen(
            onLoginSuccess = { shellViewModel.navigate(FluxerRoute.Me) }
        )
        else -> CanaryShell(
            state = state,
            shellViewModel = shellViewModel,
            authViewModel = authViewModel
        )
    }
}

@Composable
private fun CanaryShell(
    state: ShellUiState,
    shellViewModel: ShellViewModel,
    authViewModel: AuthViewModel
) {
    val isCompact = LocalConfiguration.current.screenWidthDp < 600
    if (isCompact) {
        Scaffold(
            bottomBar = {
                ShellBottomBar(
                    activeBranch = state.activeBranch,
                    onBranchSelected = shellViewModel::navigateBranch
                )
            },
            containerColor = VelvetBlack
        ) { padding ->
            Box(modifier = Modifier.padding(padding).background(VelvetBlack)) {
                ShellContent(state.activePath, shellViewModel, authViewModel)
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(VelvetBlack)
        ) {
            ShellRail(
                activeBranch = state.activeBranch,
                onBranchSelected = shellViewModel::navigateBranch
            )
            Box(modifier = Modifier.weight(1f)) {
                ShellContent(state.activePath, shellViewModel, authViewModel)
            }
        }
    }
}

@Composable
private fun ShellContent(
    path: String,
    shellViewModel: ShellViewModel,
    authViewModel: AuthViewModel
) {
    when {
        path == RoutePaths.Me -> MessagesScreen(
            onBack = { },
            onChannelSelected = { channel ->
                shellViewModel.navigate(FluxerRoute.DmChannel(channel.id))
            },
            onStartCall = { channelId ->
                shellViewModel.navigate(FluxerRoute.DmCall(channelId))
            }
        )
        path.startsWith("/channels/@me/") && path.endsWith("/call") -> {
            val channelId = path.trim('/').split('/').getOrNull(2).orEmpty()
            ActiveCallScreen(
                callId = channelId,
                onEndCall = { shellViewModel.navigate(FluxerRoute.DmChannel(channelId)) }
            )
        }
        path.startsWith("/channels/@me/") -> ChatScreen(
            onLogout = authViewModel::logout,
            onNavigateToSettings = { shellViewModel.navigate(FluxerRoute.You) },
            onNavigateToStarred = { shellViewModel.navigate(FluxerRoute.Favorites) },
            onNavigateToMessages = { shellViewModel.navigate(FluxerRoute.Me) },
            onNavigateToProfile = { shellViewModel.navigate(FluxerRoute.You) },
            onNavigateToVoiceChannel = { shellViewModel.navigate(FluxerRoute.DmCall(it)) },
            initialChannelId = path.trim('/').split('/').getOrNull(2),
            targetMessageId = path.trim('/').split('/').getOrNull(3)
        )
        path == RoutePaths.Favorites -> StarredChannelsScreen(
            onBack = { shellViewModel.navigate(FluxerRoute.Me) },
            onChannelSelected = { channel, server ->
                shellViewModel.navigate(FluxerRoute.GuildChannel(server.id, channel.id))
            }
        )
        path.startsWith("/channels/@favorites/") -> StarredChannelsScreen(
            onBack = { shellViewModel.navigate(FluxerRoute.Favorites) },
            onChannelSelected = { channel, server ->
                shellViewModel.navigate(FluxerRoute.GuildChannel(server.id, channel.id))
            }
        )
        path.startsWith("/channels/") -> GuildOrChannelContent(path, shellViewModel, authViewModel)
        path == RoutePaths.Notifications -> NotificationCenterScreen(
            onBack = { shellViewModel.navigate(FluxerRoute.Me) },
            onNotificationClick = { shellViewModel.navigate(FluxerRoute.Me) }
        )
        path == RoutePaths.You -> ProfileScreen(
            onBack = { shellViewModel.navigate(FluxerRoute.Me) },
            onLogout = authViewModel::logout
        )
        path.startsWith("/settings/guild/") -> PlaceholderScreen(
            title = "Guild settings",
            subtitle = "Guild settings route is ready; native tabs will be filled in the next parity pass.",
            icon = Icons.Default.Settings,
            onBack = { shellViewModel.navigate(FluxerRoute.Me) }
        )
        path.startsWith("/settings/account") -> AccountScreen(
            onBack = { shellViewModel.navigate(FluxerRoute.You) },
            onLogout = authViewModel::logout
        )
        path.startsWith("/settings/appearance") -> AppearanceScreen(onBack = { shellViewModel.navigate(FluxerRoute.You) })
        path.startsWith("/settings/storage") -> StorageScreen(onBack = { shellViewModel.navigate(FluxerRoute.You) })
        path.startsWith("/settings/language") -> LanguageScreen(onBack = { shellViewModel.navigate(FluxerRoute.You) })
        path.startsWith("/settings/notifications") -> NotificationSettingsScreen(
            onBack = { shellViewModel.navigate(FluxerRoute.You) },
            onNavigateToNotificationCenter = { shellViewModel.navigate(FluxerRoute.Notifications) }
        )
        path.startsWith("/settings/support") -> SupportScreen(onBack = { shellViewModel.navigate(FluxerRoute.You) })
        path.startsWith("/settings/about") -> AboutScreen(onBack = { shellViewModel.navigate(FluxerRoute.You) })
        else -> PlaceholderScreen(
            title = "Fluxer",
            subtitle = "This canary route currently redirects into the native shell.",
            icon = Icons.Default.Home,
            onBack = { shellViewModel.navigate(FluxerRoute.Me) }
        )
    }
}

@Composable
private fun GuildOrChannelContent(
    path: String,
    shellViewModel: ShellViewModel,
    authViewModel: AuthViewModel
) {
    val parts = path.trim('/').split('/')
    val guildId = parts.getOrNull(1)
    val channelId = parts.getOrNull(2)
    if (guildId != null && channelId != null) {
        LaunchedEffect(guildId, channelId) {
            shellViewModel.rememberGuildChannel(guildId, channelId)
        }
    }
    ChatScreen(
        onLogout = authViewModel::logout,
        onNavigateToSettings = {
            if (guildId == null) {
                shellViewModel.navigate(FluxerRoute.You)
            } else {
                shellViewModel.navigate(FluxerRoute.GuildSettings(guildId))
            }
        },
        onNavigateToStarred = { shellViewModel.navigate(FluxerRoute.Favorites) },
        onNavigateToMessages = { shellViewModel.navigate(FluxerRoute.Me) },
        onNavigateToProfile = { shellViewModel.navigate(FluxerRoute.You) },
        onNavigateToVoiceChannel = { shellViewModel.navigate(FluxerRoute.DmCall(it)) },
        initialGuildId = guildId,
        initialChannelId = channelId,
        targetMessageId = parts.getOrNull(3)
    )
}

@Composable
private fun ShellBottomBar(
    activeBranch: ShellBranch,
    onBranchSelected: (ShellBranch) -> Unit
) {
    NavigationBar(
        containerColor = VelvetBlack,
        tonalElevation = 0.dp
    ) {
        shellItems().forEach { item ->
            NavigationBarItem(
                selected = activeBranch == item.branch,
                onClick = { onBranchSelected(item.branch) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    selectedIconColor = TextPrimary,
                    selectedTextColor = TextPrimary,
                    indicatorColor = VelvetDark,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                )
            )
        }
    }
}

@Composable
private fun ShellRail(
    activeBranch: ShellBranch,
    onBranchSelected: (ShellBranch) -> Unit
) {
    Column(
        modifier = Modifier
            .width(84.dp)
            .fillMaxHeight()
            .background(VelvetBlack)
            .border(1.dp, BorderSubtle.copy(alpha = 0.35f))
            .padding(horizontal = 14.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        shellItems().forEach { item ->
            FluxerIconButton(
                icon = item.icon,
                contentDescription = item.label,
                onClick = { onBranchSelected(item.branch) },
                tint = if (activeBranch == item.branch) TextPrimary else TextMuted,
                containerColor = if (activeBranch == item.branch) PhantomRed.copy(alpha = 0.24f) else VelvetDark,
                modifier = Modifier.size(46.dp)
            )
        }
    }
}

private data class ShellItem(
    val branch: ShellBranch,
    val label: String,
    val icon: ImageVector
)

private fun shellItems() = listOf(
    ShellItem(ShellBranch.Home, "Home", Icons.Default.Chat),
    ShellItem(ShellBranch.Notifications, "Notifications", Icons.Default.Notifications),
    ShellItem(ShellBranch.You, "You", Icons.Default.AccountCircle)
)

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VelvetBlack),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = VelvetDark,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)
            ) {
            CircularProgressIndicator(color = PhantomRed)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Fluxer", color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
            Text("Getting things ready", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ReconnectingScreen() {
    PlaceholderScreen(
        title = "Reconnecting",
        subtitle = "Restoring your gateway session...",
        icon = Icons.Default.Refresh,
        onBack = null
    )
}

@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onBack: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VelvetBlack)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = PhantomRed, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                color = TextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = TextMuted,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            if (onBack != null) {
                Spacer(modifier = Modifier.height(24.dp))
                androidx.compose.material3.Button(onClick = onBack) {
                    Text("Back")
                }
            }
        }
    }
}
