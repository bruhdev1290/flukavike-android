package com.fluxer.client

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fluxer.client.ui.screens.*
import com.fluxer.client.ui.theme.FluxerTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach { (permission, granted) ->
            Timber.d("Permission $permission: $granted")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        // Request necessary permissions
        requestPermissions()
        
        // Handle notification intents
        handleNotificationIntent(intent)

        setContent {
            FluxerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    FluxerApp()
                }
            }
        }
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        
        // Notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Microphone permission for voice/calls
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        
        // Phone permission for CallKit integration
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_PHONE_STATE)
        }
        
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
    
    private fun handleNotificationIntent(intent: android.content.Intent?) {
        intent?.let {
            val notificationType = it.getStringExtra("notification_type")
            val channelId = it.getStringExtra("channel_id")
            val callId = it.getStringExtra("call_id")
            val action = it.getStringExtra("action")
            
            Timber.d("Notification intent: type=$notificationType, channel=$channelId, call=$callId, action=$action")
            
            // Handle notification actions
            when (action) {
                "accept_call" -> {
                    // Handle call acceptance
                }
                "decline_call" -> {
                    // Handle call decline
                }
            }
        }
    }
}

@Composable
fun FluxerApp() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("chat") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        
        composable("chat") {
            ChatScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("chat") { inclusive = true }
                    }
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToMessages = {
                    navController.navigate("messages")
                },
                onNavigateToProfile = {
                    navController.navigate("profile")
                }
            )
        }
        
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToNotifications = {
                    navController.navigate("notifications")
                },
                onNavigateToSupport = {
                    navController.navigate("support")
                }
            )
        }
        
        composable("starred") {
            StarredChannelsScreen(
                onBack = { navController.popBackStack() },
                onChannelSelected = { channel, server ->
                    navController.navigate("chat") {
                        // TODO: Pass channel and server info to chat screen
                        popUpTo("chat") { inclusive = true }
                    }
                }
            )
        }
        
        composable("messages") {
            MessagesScreen(
                onBack = { navController.popBackStack() },
                onChannelSelected = { channel ->
                    navController.popBackStack()
                    // TODO: Select channel in ChatScreen
                }
            )
        }
        
        composable("notifications") {
            NotificationSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("voice_channel/{channelId}",
            arguments = listOf(navArgument("channelId") { type = NavType.StringType })
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getString("channelId")!!
            VoiceChannelScreen(
                channelId = channelId,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("call/{callId}",
            arguments = listOf(navArgument("callId") { type = NavType.StringType })
        ) { backStackEntry ->
            val callId = backStackEntry.arguments?.getString("callId")!!
            ActiveCallScreen(
                callId = callId,
                onEndCall = { navController.popBackStack() }
            )
        }
        
        composable("profile") {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable("support") {
            SupportScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
