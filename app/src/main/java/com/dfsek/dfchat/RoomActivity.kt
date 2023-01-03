package com.dfsek.dfchat

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.room
import net.folivo.trixnity.clientserverapi.model.rooms.GetEvents
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.Event

class RoomActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface {
                Box(modifier = Modifier.fillMaxSize()) {
                    intent.getStringExtra("room")?.let { roomId ->
                        Log.d("Opening room ID", roomId)
                        var roomName by remember { mutableStateOf("") }
                        var events by remember { mutableStateOf<List<Pair<UserId, List<Event.RoomEvent<*>>>>>(emptyList()) }
                        Row(modifier = Modifier.statusBarsPadding().height(24.dp).background(Color.White)) {
                            SettingsDropdown()
                            Text(roomName, fontSize = 32.sp)
                        }
                        fun handleEvent(event: Event.RoomEvent<*>) {
                            Log.d("Event decoded", parseEvent(event.content))
                            val user = event.sender
                            events = if (events.isNotEmpty() && events.last().first == user) {
                                events.update(
                                    events.last()
                                        .copy(second = events.last().second.plus(event)),
                                    events.size - 1
                                )
                            } else {
                                events.plus(Pair(user, mutableListOf(event)))
                            }
                        }
                        LaunchedEffect(AccountActivity.matrixClient) {
                            launch {
                                AccountActivity.matrixClient!!.room.getById(RoomId(roomId))
                                    .collectLatest {
                                        roomName = it!!.getHumanName()
                                    }
                            }
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            val state = LazyListState()
                            LazyColumn(state = state, modifier = Modifier.weight(1f)) {
                                items(events.reversed()) {
                                    Log.d("USER", it.first.toString())
                                    it.second.reversed().forEach {
                                        Log.d("MESSAGE", it.toString())
                                    }
                                    EventBlock(RoomId(roomId), it.second, it.first)
                                }
                            }
                            InfiniteScrollHandler(state) {
                                Log.d("SCROLL", "Reached top!")
                                CoroutineScope(Dispatchers.Default).launch {
                                    suspend fun goBack() {
                                        Log.d("Starting", events.first().second.first().toString())
                                        AccountActivity.matrixClient!!.room.getTimelineEvents(
                                            roomId = RoomId(roomId),
                                            startFrom = events.first().second.first().id,
                                            direction = GetEvents.Direction.BACKWARDS
                                        )
                                            .collectLatest { flow ->
                                                Log.d("More events", "Collecting new event stream.")
                                                val event = flow.first()
                                                event?.let { handleEvent(it.event) }
                                                event?.content?.onSuccess {
                                                    //Log.d("Decrypt?", it.toString())
                                                }?.onFailure {
                                                    it.printStackTrace()
                                                }

                                            }
                                    }
                                    if (events.isNotEmpty()) {
                                        goBack()
                                    } else {
                                        AccountActivity.matrixClient!!.room.getLastTimelineEvents(RoomId(roomId))
                                            .collectLatest { flowFlow ->
                                                Log.d("More events", "Collecting new event stream.")
                                                flowFlow?.collectLatest { flow ->
                                                    val event = flow.first()
                                                    event?.let { handleEvent(it.event) }
                                                    event?.content?.onSuccess {
                                                        //Log.d("Decrypt?", it.toString())
                                                    }?.onFailure {
                                                        it.printStackTrace()
                                                    }
                                                }
                                            }
                                        goBack()
                                    }
                                }
                            }
                            Row(modifier = Modifier.navigationBarsPadding().imePadding()) {
                                val message = remember { mutableStateOf("") }
                                TextField(
                                    value = message.value,
                                    onValueChange = {
                                        message.value = it
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun InfiniteScrollHandler(
        listState: LazyListState, buffer: Int = 5, action: () -> Unit
    ) {
        var lastTotalItems = -1
        val loadMore = remember {
            derivedStateOf {
                val layoutInfo = listState.layoutInfo
                val totalItemsNumber = layoutInfo.totalItemsCount
                val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
                val loadMore =
                    lastVisibleItemIndex > (totalItemsNumber - buffer) && (lastTotalItems != totalItemsNumber)

                loadMore
            }
        }
        LaunchedEffect(loadMore) {
            snapshotFlow { loadMore.value }
                .distinctUntilChanged()
                .collect {
                    if (it) {
                        lastTotalItems = listState.layoutInfo.totalItemsCount
                        action()
                    }
                }
        }
    }

    @Composable
    fun EventBlock(roomId: RoomId, eventIds: List<Event.RoomEvent<*>>, userId: UserId) {
        Row {
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
            } ?: Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Cyan)
            )

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

            Column {
                Text(userId.full, fontSize = 14.sp)
                eventIds.reversed().forEach { event ->
                    Text(parseEvent(event.content))
                }
            }
        }
    }
}