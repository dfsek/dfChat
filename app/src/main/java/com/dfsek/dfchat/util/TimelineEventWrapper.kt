package com.dfsek.dfchat.util

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastEditNewContent

interface TimelineEventWrapper {
    class Default(override val event: TimelineEvent) : TimelineEventWrapper {
        @Composable
        override fun RenderEvent(modifier: Modifier) {
            Column {
                event.RenderMessage(modifier)
                Row {
                    event.readReceipts.forEach {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(getAvatarUrl(it.roomMember.avatarUrl, 18))
                                .crossfade(true)
                                .decoderFactory(BitmapFactoryDecoder.Factory())
                                .build(),
                            contentScale = ContentScale.Fit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp).clip(CircleShape)
                        )
                    }
                }
            }
        }
    }

    class Redacted(override val event: TimelineEvent, val redactionEvent: TimelineEvent) : TimelineEventWrapper {
        @Composable
        override fun RenderEvent(modifier: Modifier) {
            val jankyRandom = event.eventId.hashCode() and 1 == 0
            Text(if(jankyRandom) "[REDACTED]" else "[DATA EXPUNGED]", color = MaterialTheme.colors.error)
        }
    }

    class Replaced(override val event: TimelineEvent, val replacedBy: TimelineEvent) : TimelineEventWrapper {
        @Composable
        override fun RenderEvent(modifier: Modifier) {
            Row {
                event.getLastEditNewContent()?.RenderContent(modifier = Modifier.weight(1f))
                Icon(
                    contentDescription = "Edited",
                    imageVector = Icons.Default.Edit,
                )
            }
        }

    }

    val event: TimelineEvent

    @Composable
    fun RenderEvent(modifier: Modifier)
}
