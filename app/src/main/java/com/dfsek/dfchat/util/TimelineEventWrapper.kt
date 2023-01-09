package com.dfsek.dfchat.util

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

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

    val event: TimelineEvent

    @Composable
    fun RenderEvent(modifier: Modifier)
}
