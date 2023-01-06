package com.dfsek.dfchat.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest
import com.dfsek.dfchat.SessionHolder
import com.dfsek.dfchat.state.ChatRoomState
import com.dfsek.dfchat.util.SettingsDropdown
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

class RoomActivity : AppCompatActivity() {
    private lateinit var chatRoomState: ChatRoomState
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val lifecycleOwner = LocalLifecycleOwner.current
            intent.getStringExtra("room")?.let { roomId ->
                SessionHolder.currentSession?.let {
                    val state = remember {
                        ChatRoomState(
                            roomId = roomId,
                            client = it,
                            lifecycleOwner
                        ).also {
                            it.startSync()
                            chatRoomState = it
                        }

                    }
                    RoomUI(
                        roomState = state,
                        modifier = Modifier
                    )
                }
            }
        }
    }
    @Composable
    fun RoomUI(
        roomState: ChatRoomState,
        modifier: Modifier,
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

                RoomTopBar(roomName, Modifier.statusBarsPadding())
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
                    if (input.trim().isNotEmpty()) {
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
    fun RoomTopBar(
        name: String,
        modifier: Modifier
    ) {
        Row(modifier = modifier.background(Color.White).fillMaxWidth()) {
            SettingsDropdown(applicationContext, this@RoomActivity)
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
                    MessageBlock(state, userId = it.first.disambiguatedDisplayName, timelineEvents = it.second)
                }
            }
        }
    }

    @Composable
    fun MessageBlock(
        state: ChatRoomState,
        modifier: Modifier = Modifier,
        userId: String,
        timelineEvents: List<TimelineEvent>
    ) {
        Row(modifier = modifier) {

            Log.d("Channel Image", "Drawing image...")
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(state.getUserAvatar(userId))
                    .crossfade(true)
                    .decoderFactory(BitmapFactoryDecoder.Factory())
                    .build(),
                contentScale = ContentScale.Fit,
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(CircleShape)
            )


            Column {
                Text(userId, fontSize = 14.sp)
                timelineEvents.forEach { event ->
                    val content by remember { mutableStateOf(event.root.toContentStringWithIndent()) }

                    Text(content)
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        if (this::chatRoomState.isInitialized) {
            chatRoomState.stopSync()
        }
    }
}