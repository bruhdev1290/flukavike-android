package com.fluxer.client.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fluxer.client.ui.components.*
import com.fluxer.client.ui.theme.*
import com.fluxer.client.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.loginError.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val customInstanceUrl by viewModel.customInstanceUrl.collectAsState()
    val activeInstanceBaseUrl by viewModel.activeInstanceBaseUrl.collectAsState()
    val instanceMessage by viewModel.instanceMessage.collectAsState()

    var instanceInput by remember(customInstanceUrl) { mutableStateOf(customInstanceUrl) }
    
    // Navigate on successful auth
    LaunchedEffect(authState) {
        if (authState is com.fluxer.client.data.repository.AuthRepository.AuthState.Authenticated) {
            onLoginSuccess()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VelvetBlack)
            .drawBehind {
                // Draw diagonal accent lines
                drawLine(
                    color = PhantomRed.copy(alpha = 0.1f),
                    start = Offset(0f, size.height * 0.3f),
                    end = Offset(size.width, size.height * 0.7f),
                    strokeWidth = 200f
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo/Brand
            Spacer(modifier = Modifier.height(48.dp))
            
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(PhantomRed, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "F",
                    style = MaterialTheme.typography.displayLarge,
                    color = TextPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Title
            Text(
                text = "FLUKAVIKE",
                style = MaterialTheme.typography.displaySmall.copy(
                    letterSpacing = 4.sp
                ),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Mode toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TabButton(
                    text = "SIGN IN",
                    isSelected = isLoginMode,
                    onClick = { 
                        isLoginMode = true
                        viewModel.clearError()
                    }
                )
                Spacer(modifier = Modifier.width(32.dp))
                TabButton(
                    text = "REGISTER",
                    isSelected = !isLoginMode,
                    onClick = { 
                        isLoginMode = false
                        viewModel.clearError()
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Form fields
            AnimatedContent(
                targetState = isLoginMode,
                transitionSpec = {
                    fadeIn() + slideInHorizontally { it / 4 } togetherWith
                    fadeOut() + slideOutHorizontally { -it / 4 }
                }
            ) { loginMode ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!loginMode) {
                        // Username field for registration
                        FluxerTextField(
                            value = username,
                            onValueChange = { username = it },
                            hint = "Choose a username",
                            label = "USERNAME",
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = TextMuted
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    FluxerTextField(
                        value = email,
                        onValueChange = { email = it },
                        hint = "Enter your email",
                        label = "EMAIL",
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = TextMuted
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    FluxerTextField(
                        value = password,
                        onValueChange = { password = it },
                        hint = "Enter your password",
                        label = "PASSWORD",
                        isPassword = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = TextMuted
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (loginMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { /* TODO: Forgot password */ },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = "FORGOT PASSWORD?",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            FluxerTextField(
                value = instanceInput,
                onValueChange = {
                    instanceInput = it
                    viewModel.clearInstanceMessage()
                },
                hint = "https://web.fluxer.app",
                label = "INSTANCE URL (OPTIONAL)",
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { viewModel.applyCustomInstance(instanceInput) }
                ) {
                    Text("APPLY INSTANCE", color = PhantomRed)
                }

                TextButton(
                    onClick = {
                        instanceInput = ""
                        viewModel.applyCustomInstance("")
                    }
                ) {
                    Text("USE DEFAULT", color = TextMuted)
                }
            }

            Text(
                text = "CURRENT: $activeInstanceBaseUrl",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            AnimatedVisibility(
                visible = instanceMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Text(
                    text = instanceMessage ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
            
            // Error message
            AnimatedVisibility(
                visible = error != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = DndRed.copy(alpha = 0.1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DndRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error ?: "",
                        style = MaterialTheme.typography.labelMedium,
                        color = DndRed,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Submit button
            LoadingButton(
                text = if (isLoginMode) "SIGN IN" else "CREATE ACCOUNT",
                isLoading = isLoading,
                onClick = {
                    if (isLoginMode) {
                        viewModel.login(email, password)
                    } else {
                        viewModel.register(email, username, password)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(onClick = onClick) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) 
                        androidx.compose.ui.text.font.FontWeight.Bold 
                    else 
                        androidx.compose.ui.text.font.FontWeight.Normal
                ),
                color = if (isSelected) PhantomRed else TextMuted
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(2.dp)
                    .background(PhantomRed)
            )
        }
    }
}
