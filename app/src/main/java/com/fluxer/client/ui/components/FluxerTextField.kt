package com.fluxer.client.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fluxer.client.data.model.Message
import com.fluxer.client.ui.theme.*

/**
 * Shared premium text field styling.
 */
@Composable
fun FluxerTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    label: String? = null,
    isPassword: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    leadingIcon: @Composable (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val borderColor = when {
        isError -> DndRed
        isFocused -> PhantomRed
        else -> BorderSubtle
    }
    
    Column(modifier = modifier) {
        // Label
        label?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = if (isFocused) TextSecondary else TextMuted,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(VelvetDark, RoundedCornerShape(12.dp))
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .onFocusChanged { isFocused = it.isFocused },
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leadingIcon?.let {
                    Box(modifier = Modifier.padding(end = 12.dp)) {
                        it()
                    }
                }
                
                Box(modifier = Modifier.weight(1f)) {
                    // Hint
                    if (value.isEmpty() && !isFocused) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextMuted
                        )
                    }
                    
                    // Input
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = TextPrimary
                        ),
                        cursorBrush = SolidColor(PhantomRed),
                        singleLine = true,
                        visualTransformation = if (isPassword && !passwordVisible) {
                            PasswordVisualTransformation()
                        } else {
                            VisualTransformation.None
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = keyboardType,
                            imeAction = imeAction
                        ),
                        keyboardActions = KeyboardActions(onAny = { onImeAction() })
                    )
                }
                
                // Password visibility toggle
                if (isPassword) {
                    TextButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = if (passwordVisible) "Hide" else "Show",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
        
        // Error message
        AnimatedVisibility(
            visible = isError && errorMessage != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = errorMessage ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = DndRed,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

/**
 * Message input field for chat with reply support and attachment button
 */
@Composable
fun MessageInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Type a message...",
    isCompact: Boolean = false,
    replyingTo: Message? = null,
    onCancelReply: () -> Unit = {},
    onAttachmentClick: () -> Unit = {},
    hasAttachment: Boolean = false,
    isSending: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    
    val horizontalPadding = if (isCompact) 12.dp else 16.dp
    val verticalPadding = if (isCompact) 10.dp else 14.dp
    val iconSize = if (isCompact) 20.dp else 24.dp
    
    Column(modifier = modifier.fillMaxWidth()) {
        // Reply preview
        AnimatedVisibility(
            visible = replyingTo != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            ReplyBanner(
                message = replyingTo,
                onCancel = onCancelReply,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (isCompact) 48.dp else 56.dp)
                .background(VelvetDark, RoundedCornerShape(if (isCompact) 18.dp else 20.dp))
                .border(
                    width = 1.dp,
                    color = if (isFocused) PhantomRed.copy(alpha = 0.6f) else BorderSubtle,
                    shape = RoundedCornerShape(if (isCompact) 18.dp else 20.dp)
                )
                .onFocusChanged { isFocused = it.isFocused },
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attachment button
                IconButton(
                    onClick = onAttachmentClick,
                    enabled = !isSending,
                    modifier = Modifier
                        .padding(end = if (isCompact) 4.dp else 8.dp)
                        .size(if (isCompact) 32.dp else 40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add attachment",
                        tint = TextMuted,
                        modifier = Modifier.size(iconSize)
                    )
                }
                
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = if (isCompact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                            color = TextMuted
                        )
                    }
                    
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSending,
                        textStyle = (if (isCompact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge).copy(
                            color = TextPrimary
                        ),
                        cursorBrush = SolidColor(PhantomRed),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSend() })
                    )
                }
                
                // Send button
                AnimatedVisibility(
                    visible = value.isNotBlank() || hasAttachment || isSending,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    IconButton(
                        onClick = onSend,
                        enabled = !isSending,
                        modifier = Modifier
                            .padding(start = if (isCompact) 4.dp else 8.dp)
                            .size(if (isCompact) 32.dp else 40.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isCompact) 28.dp else 32.dp)
                                .background(PhantomRed, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(iconSize - 4.dp),
                                    strokeWidth = 2.dp,
                                    color = TextPrimary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(iconSize - 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reply banner showing the message being replied to
 */
@Composable
private fun ReplyBanner(
    message: Message?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (message == null) return
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = VelvetSurface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reply icon
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = PhantomRed,
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Reply info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Replying to ${message.author?.username ?: "Unknown"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = PhantomRed
                )
                Text(
                    text = message.content.take(50) + if (message.content.length > 50) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Cancel button
            IconButton(onClick = onCancel, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel reply",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
