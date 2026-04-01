package com.fluxer.client.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.fluxer.client.data.model.Attachment

@Composable
fun ChatAttachment(attachment: Attachment) {
    if (attachment.contentType?.startsWith("image/") == true) {
        Box(modifier = Modifier.padding(vertical = 4.dp)) {
            Image(
                painter = rememberAsyncImagePainter(attachment.url),
                contentDescription = "Image attachment: ${attachment.filename}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}
