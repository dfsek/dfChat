package com.dfsek.dfchat

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Contextual
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.toFlowList
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.Event

class RoomActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column {
                intent.getStringExtra("room")?.let { roomId ->
                    Log.d("Opening room ID", roomId)
                    SettingsDropdown()
                    var events by remember { mutableStateOf<List<Pair<UserId, List<Event.RoomEvent<*>>>>>(emptyList()) }
                    val ids = remember { mutableSetOf<EventId>() }
                    LaunchedEffect(AccountActivity.matrixClient) {
                        AccountActivity.matrixClient!!.api.sync.sync()
                            .onSuccess {
                                AccountActivity.matrixClient!!.room.getLastTimelineEvents(RoomId(roomId))
                                    .collectLatest { flowFlow ->
                                        Log.d("More events", "Collecting new event stream.")
                                        flowFlow?.collectLatest {
                                            val event = it.first()
                                            Log.d("Event decoded", parseEvent(event!!.event.content))
                                            val user = event.event.sender
                                            if(ids.contains(event.eventId)) {
                                                Log.d("Event decoded", "Duplicate event.")
                                                return@collectLatest
                                            }
                                            ids.add(event.eventId)
                                            events = if (events.isNotEmpty() && events.last().first == user) {
                                                events.update(events.last().copy(second = events.last().second.plus(event.event)), events.size - 1)
                                            } else {
                                                events.plus(Pair(user, mutableListOf()))
                                            }
                                            event.content?.onSuccess {
                                                //Log.d("Decrypt?", it.toString())
                                            }?.onFailure {
                                                it.printStackTrace()
                                            }
                                        }
                                    }
                            }
                    }

                    LazyColumn {
                        items(events.reversed()) {
                            EventBlock(RoomId(roomId), it.second, it.first)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun EventBlock(roomId: RoomId, eventIds: List<Event.RoomEvent<*>>, userId: UserId) {
        Row {
            val avatarUrl = remember { mutableStateOf<String?>(null) }
            avatarUrl.value?.let {
                val bytes = remember { mutableStateOf<ByteArray?>(null) }
                bytes.value?.let {
                    Log.d("Channel Image", "Drawing image...")
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(it)
                            .crossfade(true)
                            .decoderFactory(BitmapFactoryDecoder.Factory())
                            .build(),
                        contentScale = ContentScale.Fit,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(CircleShape)
                    )
                }
                LaunchedEffect(roomId, eventIds) {
                    AccountActivity.matrixClient!!.api.users.getAvatarUrl(userId)
                        .onSuccess {
                            it?.let { url ->
                                AccountActivity.matrixClient!!.api.media.download(url)
                                    .onSuccess { media ->
                                        val length = media.contentLength!!.toInt()
                                        val byteArray = ByteArray(length)
                                        media.content.readFully(byteArray, 0, length)
                                        Log.d("Image type", media.contentType.toString())
                                        bytes.value = byteArray
                                    }
                            }
                        }
                }
            } ?: Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Cyan)
            )

            Column {
                Text(userId.full, fontSize = 14.sp)
                eventIds.reversed().forEach { event ->
                    Text(parseEvent(event.content))
                }
            }
        }
    }
}