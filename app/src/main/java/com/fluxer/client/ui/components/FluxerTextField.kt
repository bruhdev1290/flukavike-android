package com.fluxer.client.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
nimport androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.fluxer.client.ui.theme.*

/**
 * Sharp, angular text field with Persona 5-inspired styling
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
                text = it.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isFocused) PhantomRed else TextMuted,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        // Text field container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(VelvetDark)
                .border(2.dp, borderColor)
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
                            text = if (passwordVisible) "HIDE" else "SHOW",
                            style = MaterialTheme.typography.labelSmall,
                            color = PhantomRed
                        )
                    }
                }
            }
            
            // Focus indicator line at bottom
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(PhantomRed)
                        .align(Alignment.BottomCenter)
                )
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
 * Message input field for chat
 */
@Composable
fun MessageInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Type a message..."
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(VelvetMid)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) PhantomRed else BorderSubtle
            )
            .onFocusChanged { isFocused = it.isFocused },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextMuted
                    )
                }
                
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = TextPrimary
                    ),
                    cursorBrush = SolidColor(PhantomRed),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() })
                )
            }
            
            // Send button
            if (value.isNotBlank()) {
                IconButton(
                    onClick = onSend,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = PhantomRed
                    )
                }
            }
        }
    }
}
