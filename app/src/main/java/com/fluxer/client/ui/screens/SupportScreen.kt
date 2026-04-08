package com.fluxer.client.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fluxer.client.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Form state
    var selectedIssueType by remember { mutableStateOf(IssueType.ERRORS) }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var includeLogs by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Support",
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
                .padding(16.dp)
        ) {
            // Issue Type Dropdown
            Text(
                text = "Issue Type",
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedIssueType.displayName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = VelvetSurface,
                        unfocusedContainerColor = VelvetSurface,
                        focusedBorderColor = PhantomRed,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(VelvetSurface)
                ) {
                    IssueType.entries.forEach { issueType ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = issueType.displayName,
                                    color = TextPrimary
                                )
                            },
                            onClick = {
                                selectedIssueType = issueType
                                expanded = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = TextPrimary
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Subject Field
            Text(
                text = "Subject",
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                placeholder = { Text("Brief description of your issue", color = TextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = VelvetSurface,
                    unfocusedContainerColor = VelvetSurface,
                    focusedBorderColor = PhantomRed,
                    unfocusedBorderColor = BorderSubtle,
                    focusedPlaceholderColor = TextMuted,
                    unfocusedPlaceholderColor = TextMuted
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Message Field
            Text(
                text = "Message",
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                placeholder = { Text("Please describe your issue in detail...", color = TextMuted) },
                minLines = 5,
                maxLines = 10,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = VelvetSurface,
                    unfocusedContainerColor = VelvetSurface,
                    focusedBorderColor = PhantomRed,
                    unfocusedBorderColor = BorderSubtle,
                    focusedPlaceholderColor = TextMuted,
                    unfocusedPlaceholderColor = TextMuted
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Include Logs Checkbox
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { includeLogs = !includeLogs },
                color = VelvetSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Include App Logs",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Helps us diagnose issues faster",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                    
                    Checkbox(
                        checked = includeLogs,
                        onCheckedChange = { includeLogs = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PhantomRed,
                            uncheckedColor = TextMuted
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Note about email
            Text(
                text = "This will open your default email app to send the support request.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Send Button
            Button(
                onClick = {
                    composeSupportEmail(
                        context = context,
                        issueType = selectedIssueType,
                        subject = subject,
                        message = message,
                        includeLogs = includeLogs
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PhantomRed
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = subject.isNotBlank() && message.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Send Support Request",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

enum class IssueType(val displayName: String) {
    ERRORS("Errors"),
    FEATURE_REQUEST("Feature Request"),
    SECURITY("Security"),
    PRIVACY_INQUIRY("Privacy Inquiry")
}

private fun composeSupportEmail(
    context: android.content.Context,
    issueType: IssueType,
    subject: String,
    message: String,
    includeLogs: Boolean
) {
    val recipient = "correspondencesandrew@gmail.com"
    
    // Build the email body
    val emailBody = buildString {
        appendLine("Issue Type: ${issueType.displayName}")
        appendLine()
        appendLine("Message:")
        appendLine(message)
        appendLine()
        appendLine("---")
        appendLine("App Version: ${getAppVersion(context)}")
        appendLine("Android Version: ${android.os.Build.VERSION.RELEASE}")
        appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        if (includeLogs) {
            appendLine()
            appendLine("[LOGS ATTACHED]")
        }
    }
    
    // Build subject with issue type prefix
    val fullSubject = "[Fluxer Support] [${issueType.displayName}] $subject"
    
    // Create email intent
    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
        putExtra(Intent.EXTRA_SUBJECT, fullSubject)
        putExtra(Intent.EXTRA_TEXT, emailBody)
    }
    
    // If logs should be included, we would need to attach them
    // For now, we'll just include the logs indicator in the body
    // In a real implementation, you'd save logs to a file and use ACTION_SEND with a file URI
    
    // Check if there's an email app available
    if (emailIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(Intent.createChooser(emailIntent, "Send support request via:"))
    } else {
        // Fallback: try with ACTION_SEND
        val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
            putExtra(Intent.EXTRA_SUBJECT, fullSubject)
            putExtra(Intent.EXTRA_TEXT, emailBody)
        }
        context.startActivity(Intent.createChooser(fallbackIntent, "Send support request via:"))
    }
}

private fun getAppVersion(context: android.content.Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "Unknown"
    } catch (e: Exception) {
        "Unknown"
    }
}
