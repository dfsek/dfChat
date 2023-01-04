package com.dfsek.dfchat.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
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
import com.dfsek.dfchat.SettingsDropdown
import com.dfsek.dfchat.parseEvent
import com.dfsek.dfchat.state.ChatRoomState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.UserId

@Composable
fun RoomUI(
    roomState: ChatRoomState,
    modifier: Modifier,
    applicationContext: Context,
    activity: Activity
) {
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    Surface(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                RoomMessages(roomState, scrollState, Modifier.weight(1f))
                UserInput(
                    onMessageSent = { content ->
                        scope.launch {
                            roomState.sendTextMessage(content)
                        }
                    },
                    modifier = Modifier.navigationBarsPadding().imePadding(),
                )
            }
            var roomName by remember { mutableStateOf("") }

            LaunchedEffect(roomState.roomId) {
                roomState.getName {
                    roomName = it
                }
            }

            RoomTopBar(roomName, Modifier.statusBarsPadding(), applicationContext, activity, roomState, scope)
        }
    }
}

@Composable
fun UserInput(
    onMessageSent: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var input by remember { mutableStateOf("") }

    Surface {
        Row(modifier = modifier) {
            TextField(value = input, onValueChange = {
                input = it
            })
            Button(onClick = {
                if(input.trim().isNotEmpty()) {
                    onMessageSent(input)
                    input = ""
                }
            }) {
                Text("Submit")
            }
        }
    }
}

@Composable
fun RoomTopBar(name: String, modifier: Modifier, applicationContext: Context, activity: Activity, state: ChatRoomState, scope: CoroutineScope) {
    Row(modifier = modifier.background(Color.White).fillMaxWidth()) {
        SettingsDropdown(applicationContext, activity, refresh = {
            scope.launch {
                Log.d("Fetching", "more messages")
                state.clear()
                state.fetchMessages()
            }
        })
        Text(name, fontSize = 24.sp)
    }
}

@Composable
fun RoomMessages(
    state: ChatRoomState,
    scrollState: LazyListState,
    modifier: Modifier
) {
    val scope = rememberCoroutineScope()
    Box(modifier = modifier) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize(),
            reverseLayout = true
        ) {
            items(state.splitEvents()) {
                MessageBlock(state, userId = it.first, timelineEvents = it.second)
            }
        }
    }
}

@Composable
fun MessageBlock(
    state: ChatRoomState,
    modifier: Modifier = Modifier,
    userId: UserId,
    timelineEvents: List<TimelineEvent>
) {
    Row(modifier = modifier) {
        var bytes by remember { mutableStateOf<ByteArray?>(null) }
        bytes?.let {
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
        LaunchedEffect(userId) {
            state.getAvatar(userId) {
                bytes = it
            }
        }

        Column {
            Text(userId.full, fontSize = 14.sp)
            timelineEvents.forEach { event ->
                var content by remember { mutableStateOf(parseEvent(event.event.content)) }

                LaunchedEffect(event) {
                    event.content
                        ?.onSuccess {
                            Log.d("Decrypted event", it.toString())
                            content = parseEvent(it)
                        }?.onFailure {
                            Log.e("Decrypted event", "Failure")
                            it.printStackTrace()
                        }
                }

                Text(content)
            }
        }
    }
}