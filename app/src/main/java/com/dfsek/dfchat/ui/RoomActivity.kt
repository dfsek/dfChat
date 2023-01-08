package com.dfsek.dfchat.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest
import com.dfsek.dfchat.SessionHolder
import com.dfsek.dfchat.state.ChatRoomState
import com.dfsek.dfchat.util.RenderMessage
import com.dfsek.dfchat.util.SettingsDropdown
import com.dfsek.dfchat.util.getPreviewText
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageImageContent
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

class RoomActivity : AppCompatActivity() {
    private lateinit var chatRoomState: ChatRoomState
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            intent.getStringExtra("room")?.let { roomId ->
                SessionHolder.currentSession?.let { session ->
                    session.getRoom(roomId)?.let {
                        val state = remember {
                            ChatRoomState(
                                room = it,
                                client = session
                            ).also {
                                it.startSync()
                                chatRoomState = it
                            }

                        }
                        if (state.selectedImageUrl != null) {
                            ImagePreviewUI(state)
                        } else {
                            RoomUI(
                                roomState = state,
                                modifier = Modifier
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ImagePreviewUI(roomState: ChatRoomState) {
        Column {
            Row {
                IconButton(onClick = {
                    roomState.selectedImageUrl = null
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }
            var scale by remember { mutableStateOf(1f) }
            var translation by remember { mutableStateOf(Offset.Zero) }

            val maxScale = 8f
            val minScale = 0.5f
            fun calculateNewScale(k: Float): Float =
                if ((scale <= maxScale && k > 1f) || (scale >= minScale && k < 1f)) scale * k else scale

            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(roomState.selectedImageUrl)
                    .crossfade(true)
                    .decoderFactory(BitmapFactoryDecoder.Factory())
                    .build(),
                contentScale = ContentScale.None,
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxSize()
                    .pointerInput(Unit) {
                        forEachGesture {
                            awaitPointerEventScope {
                                awaitFirstDown()
                                do {
                                    val event = awaitPointerEvent()
                                    scale = calculateNewScale(event.calculateZoom())
                                    translation = if (scale > 1) {
                                        event.calculatePan().plus(translation)
                                    } else {
                                        Offset.Zero
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                    }
            )
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

                val lifecycleOwner = LocalLifecycleOwner.current
                LaunchedEffect(roomState) {
                    roomState.getName(lifecycleOwner) {
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
            Column {
                chatRoomState.replyTo?.let {
                    Text(
                        text = "re: ${it.getPreviewText().substringBefore("\n")}",
                        modifier = Modifier.clickable {
                            chatRoomState.replyTo = null
                        }.fillMaxWidth()
                    )
                }
                Row(modifier = modifier) {
                    TextField(value = input, onValueChange = {
                        input = it
                    }, modifier = Modifier.weight(1f))
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
        Box(modifier = modifier) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .fillMaxSize(),
                reverseLayout = true
            ) {
                items(state.splitEvents()) {
                    MessageBlock(state, senderInfo = it.first, timelineEvents = it.second)
                }
            }
        }
    }

    @Composable
    fun MessageBlock(
        state: ChatRoomState,
        modifier: Modifier = Modifier,
        senderInfo: SenderInfo,
        timelineEvents: List<TimelineEvent>
    ) {
        Row(modifier = modifier) {

            Log.d("User Image", "Drawing image...")
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(state.getUserAvatar(senderInfo.userId))
                    .crossfade(true)
                    .decoderFactory(BitmapFactoryDecoder.Factory())
                    .build(),
                contentScale = ContentScale.Fit,
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(CircleShape)
            )


            Column {
                Text(
                    senderInfo.disambiguatedDisplayName,
                    fontSize = 14.sp,
                    style = TextStyle(fontWeight = FontWeight.Bold)
                )
                timelineEvents.forEach { Message(it) }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Message(event: TimelineEvent) {
        var expanded by remember { mutableStateOf(false) }
        var deleteDialogOpen by remember { mutableStateOf(false) }
        val messageContent = remember { event.root.getClearContent().toModel<MessageContent>() }
        val scope = rememberCoroutineScope()
        Row(modifier = Modifier.combinedClickable(
            onLongClick = {
                expanded = true
            },
            onClick = {
                messageContent?.let {
                    if (it.msgType == "m.image") {
                        val imageContent =
                            event.root.getClearContent().toModel<MessageImageContent>() ?: return@combinedClickable
                        scope.launch {
                            chatRoomState.selectedImageUrl = SessionHolder.currentSession!!.contentUrlResolver()
                                .resolveFullSize(imageContent.url)
                        }
                    }
                }
            }
        ).fillMaxWidth()) {
            if (deleteDialogOpen) {
                AlertDialog(
                    onDismissRequest = {
                        deleteDialogOpen = false
                    },
                    title = {
                        Text("Redaction Confirmation")
                    },
                    text = {
                        Column {
                            Text("Are you sure you want to redact this event?")
                            event.RenderMessage()
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            chatRoomState.redact(event)
                            deleteDialogOpen = false
                        }) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            deleteDialogOpen = false
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {
                DropdownMenuItem(
                    onClick = {
                        chatRoomState.replyTo = event
                        expanded = false
                    },
                    enabled = true
                ) {
                    Text("Reply")
                }
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        deleteDialogOpen = true
                    },
                    enabled = true
                ) {
                    Text("Redact")
                }
            }
            event.RenderMessage()
        }
    }

    override fun finish() {
        super.finish()
        if (this::chatRoomState.isInitialized) {
            chatRoomState.stopSync()
        }
    }
}