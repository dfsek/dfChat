package com.dfsek.dfchat.util

import androidx.compose.foundation.layout.Row
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastEditNewContent

interface TimelineEventWrapper {
    class Default(override val event: TimelineEvent) : TimelineEventWrapper {
        @Composable
        override fun RenderEvent(modifier: Modifier) {
            event.RenderMessage(modifier)
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
