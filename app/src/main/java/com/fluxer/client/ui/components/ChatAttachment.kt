package com.fluxer.client.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fluxer.client.data.model.Attachment
import com.fluxer.client.ui.theme.TextMuted
import com.fluxer.client.ui.theme.TextSecondary
import com.fluxer.client.ui.theme.VelvetSurface

@Composable
fun ChatAttachment(
    attachment: Attachment,
    onViewAttachment: ((Attachment) -> Unit)? = null
) {
    val type = attachment.contentType.orEmpty()
    val clickableModifier = if (onViewAttachment != null) {
        Modifier.clickable { onViewAttachment(attachment) }
    } else {
        Modifier
    }

    when {
        type.startsWith("image/") -> {
            AsyncImage(
                model = attachment.url,
                contentDescription = "Image attachment: ${attachment.filename}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .then(clickableModifier),
                contentScale = ContentScale.Crop
            )
        }
        type.startsWith("video/") -> AttachmentRow(attachment, "Video", Icons.Default.Movie, clickableModifier)
        type.startsWith("audio/") -> AttachmentRow(attachment, "Audio", Icons.Default.AudioFile, clickableModifier)
        else -> AttachmentRow(attachment, "File", Icons.Default.Description, clickableModifier)
    }
}

@Composable
private fun AttachmentRow(
    attachment: Attachment,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    clickModifier: Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(VelvetSurface)
            .then(clickModifier)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.filename,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$label • ${formatBytes(attachment.size)}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

private fun formatBytes(size: Long): String {
    if (size < 1024) return "$size B"
    val kb = size / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format("%.1f MB", mb)
}
