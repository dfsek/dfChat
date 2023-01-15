package com.dfsek.dfchat.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest
import com.dfsek.dfchat.AppState
import org.matrix.android.sdk.api.session.events.model.Content
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.getMsgType
import org.matrix.android.sdk.api.session.room.model.message.MessageType
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.getLastEditNewContent

interface TimelineEventWrapper {
    class Default(override val event: TimelineEvent) : TimelineEventWrapper {
        @Composable
        override fun RenderContent(modifier: Modifier) {
            event.RenderMessage(modifier)
        }
    }

    class Redacted(override val event: TimelineEvent, val redactionEvent: TimelineEvent) : TimelineEventWrapper {
        @Composable
        override fun RenderContent(modifier: Modifier) {
            val jankyRandom = event.eventId.hashCode() and 1 == 0
            Text(if (jankyRandom) "[REDACTED]" else "[DATA EXPUNGED]", color = MaterialTheme.colors.error)
        }

        override val canRedact = false
    }

    class Replaced(override val event: TimelineEvent, val replacedBy: TimelineEvent) : TimelineEventWrapper {
        @Composable
        override fun RenderContent(modifier: Modifier) {
            Row {
                event.getLastEditNewContent()?.RenderContent(modifier = Modifier.weight(1f))
                Icon(
                    contentDescription = "Edited",
                    imageVector = Icons.Default.Edit,
                )
            }
        }
    }

    class Replied(override val event: TimelineEvent, val repliedTo: String) : TimelineEventWrapper {
        @Composable
        override fun RenderContent(modifier: Modifier) {
            Column {
                Row(modifier = Modifier
                    .padding(PaddingValues(start = 6.dp, end = 6.dp))
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.primary.copy(alpha = 0.1f))) {

                    var repliedToEventContent by remember { mutableStateOf<Pair<Content, String>?>(null) }
                    LaunchedEffect(repliedTo) {
                        if(repliedTo.startsWith("\$local")) return@LaunchedEffect
                        val event = AppState.session!!.eventService().getEvent(event.roomId, repliedTo)
                        AppState.session!!.userService().getUser(event.senderId!!)?.displayName?.let {
                            repliedToEventContent = Pair(event.getClearContent()!!, it)
                        }
                    }

                    repliedToEventContent?.let {
                        Column(modifier = Modifier.padding(PaddingValues(start = 6.dp, end = 6.dp, bottom = 6.dp))) {
                            Text(
                                it.second,
                                fontSize = 14.sp,
                                style = TextStyle(fontWeight = FontWeight.Bold)
                            )
                            it.first.RenderContent(modifier)
                        }
                    } ?: Text("Rendering event...")
                }
                event.RenderMessage(modifier)
            }
        }
    }

    val event: TimelineEvent

    @Composable
    fun RenderEvent(modifier: Modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            RenderContent(modifier)
            RenderReadReceipts(modifier = Modifier.align(Alignment.End))
        }
    }

    val canRedact
        get() = isMine

    val canEditText
        get() = event.root.getMsgType() == MessageType.MSGTYPE_TEXT && isMine

    val isMine
        get() = event.senderInfo.userId == AppState.session!!.myUserId

    @Composable
    fun RenderContent(modifier: Modifier)

    @Composable
    fun RenderReadReceipts(modifier: Modifier) {
        Row(modifier) {
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
