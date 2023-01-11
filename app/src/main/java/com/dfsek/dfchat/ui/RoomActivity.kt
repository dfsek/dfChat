package com.dfsek.dfchat.ui

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest
import com.dfsek.dfchat.AppState
import com.dfsek.dfchat.state.ChatRoomState
import com.dfsek.dfchat.util.FixedDefaultFlingBehavior
import com.dfsek.dfchat.util.FixedDefaultFlingBehavior.Companion.fixedFlingBehavior
import com.dfsek.dfchat.util.RenderMessage
import com.dfsek.dfchat.util.TimelineEventWrapper
import com.dfsek.dfchat.util.getPreviewText
import com.dfsek.dfchat.util.vector.multipicker.toContentAttachmentData
import im.vector.lib.multipicker.MultiPicker
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageImageContent
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.math.roundToInt

class RoomActivity : AppCompatActivity() {
    private lateinit var chatRoomState: ChatRoomState
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(colors = AppState.themeColors) {
                Surface {
                    intent.getStringExtra("room")?.let { roomId ->
                        AppState.session?.let { session ->
                            session.getRoom(roomId)?.let {
                                val state = remember {
                                    ChatRoomState(
                                        room = it,
                                        session = session
                                    ).also {
                                        it.startSync()
                                        chatRoomState = it
                                    }
                                }
                                val selectionUIOpen = remember { mutableStateOf(false) }
                                RoomUI(
                                    roomState = state,
                                    isSelectionOpen = selectionUIOpen
                                )
                                if (state.selectedImageEvent != null) {
                                    ImagePreviewUI()
                                }
                                SelectionUI(selectionUIOpen)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ImagePreviewUI() {
        var scale by remember { mutableStateOf(1f) }
        var translation by remember { mutableStateOf(Offset.Zero) }

        var image by remember { mutableStateOf<File?>(null) }

        val imageContent = chatRoomState.selectedImageEvent?.root?.getClearContent()?.toModel<MessageImageContent>()
            ?: throw IllegalStateException("Image preview event is not image.")

        LaunchedEffect(imageContent) {
            try {
                image = AppState.session!!.fileService().downloadFile(imageContent)
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
            }
        }


        val maxScale = 8f
        val minScale = 0.5f

        Box(modifier = Modifier.background(MaterialTheme.colors.background).fillMaxSize()) {
            image?.let {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(it)
                        .crossfade(true)
                        .decoderFactory(BitmapFactoryDecoder.Factory())
                        .build(),
                    contentScale = ContentScale.Fit,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center)
                        .offset { IntOffset(translation.x.roundToInt(), translation.y.roundToInt()) }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }.clip(RectangleShape).fillMaxSize().pointerInput(Unit) {
                            detectTransformGestures { centroid, pan, zoom, rotation ->
                                scale = (scale * zoom).coerceIn(minScale..maxScale)
                                translation = translation.plus(pan.times(scale))
                            }
                        }
                        .clipToBounds()
                )
            } ?: Text("Rendering image...")
        }
        Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colors.onBackground.copy(alpha = 0.5f))) {
            IconButton(onClick = {
                chatRoomState.selectedImageEvent = null
            }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }
            image?.let {
                IconButton(onClick = {
                    Log.d("Downloading image", chatRoomState.selectedImageEvent.toString())
                    val target = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        chatRoomState.selectedImageEvent!!.eventId + "_" + imageContent.body
                    )
                    FileInputStream(it).use {
                        FileOutputStream(target).use { output ->
                            it.copyTo(output)
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Download"
                    )
                }
            }
        }
    }

    @Composable
    fun RoomUI(
        roomState: ChatRoomState,
        isSelectionOpen: MutableState<Boolean>
    ) {
        val scrollState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        val loadMore = remember {
            derivedStateOf {
                val layoutInfo = scrollState.layoutInfo
                val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
                lastVisibleItemIndex > (layoutInfo.totalItemsCount - 3)
            }
        }

        LaunchedEffect(loadMore) {
            snapshotFlow {
                loadMore.value
            }
                .distinctUntilChanged()
                .collect {
                    Log.d("Message Fetching", "reached top of list. Loading more messages...")
                    roomState.loadMore()
                }
        }

        Surface {
            Box(modifier = Modifier.fillMaxSize()) {
                var roomName by remember { mutableStateOf("") }
                Column(modifier = Modifier.fillMaxSize()) {
                    TopBar(roomName, Modifier.statusBarsPadding(), isSelectionOpen)
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

                val lifecycleOwner = LocalLifecycleOwner.current
                LaunchedEffect(roomState) {
                    roomState.getName(lifecycleOwner) {
                        roomName = it
                    }
                }
            }
        }
    }

    @Composable
    fun UserInput(
        onMessageSent: (String) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        var input by remember { mutableStateOf("") }
        val imageUi = remember { mutableStateOf(false) }
        if (imageUi.value) {
            ImageDialog(imageUi)
        }

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

                    IconButton(onClick = {
                        imageUi.value = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Upload"
                        )
                    }
                    TextField(value = input, onValueChange = {
                        input = it
                        chatRoomState.room.typingService().userIsTyping()
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
    fun RoomMessages(
        state: ChatRoomState,
        scrollState: LazyListState,
        modifier: Modifier
    ) {
        Box(modifier = modifier) {
            val events by remember { state.splitEvents }
            val fling = fixedFlingBehavior()
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                reverseLayout = true,
                flingBehavior = fling
            ) {
                items(events) {
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
        timelineEvents: List<TimelineEventWrapper>
    ) {
        Row(modifier = modifier) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(state.getUserAvatar(senderInfo.userId))
                    .crossfade(true)
                    .decoderFactory(BitmapFactoryDecoder.Factory())
                    .build(),
                contentScale = ContentScale.Fit,
                contentDescription = null,
                modifier = Modifier.size(AppState.Preferences.roomAvatarSize.dp).clip(CircleShape)
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
    fun Message(event: TimelineEventWrapper) {
        val menuExpanded = remember { mutableStateOf(false) }
        val deleteDialogOpen = remember { mutableStateOf(false) }
        val messageContent = remember { event.event.root.getClearContent().toModel<MessageContent>() }
        val scope = rememberCoroutineScope()
        Row(modifier = Modifier.combinedClickable(
            onLongClick = {
                menuExpanded.value = true
            },
            onClick = {
                messageContent?.let {
                    if (it.msgType == "m.image") {
                        scope.launch {
                            chatRoomState.selectedImageEvent = event.event
                        }
                    }
                }
            }
        ).fillMaxWidth()) {
            if (deleteDialogOpen.value) {
                DeleteDialog(event.event, deleteDialogOpen)
            }
            MessageDropdown(event.event, menuExpanded, deleteDialogOpen)
            event.RenderEvent(modifier = Modifier)
        }
    }

    @Composable
    fun MessageDropdown(
        event: TimelineEvent,
        expanded: MutableState<Boolean>,
        deleteDialogOpen: MutableState<Boolean>
    ) {
        val clipboardManager = LocalClipboardManager.current
        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = {
                expanded.value = false
            }
        ) {
            DropdownMenuItem(
                onClick = {
                    chatRoomState.replyTo = event
                    expanded.value = false
                },
                enabled = true
            ) {
                Text("Reply")
            }
            DropdownMenuItem(
                onClick = {
                    expanded.value = false
                    deleteDialogOpen.value = true
                },
                enabled = true
            ) {
                Text("Redact")
            }
            DropdownMenuItem(
                onClick = {
                    expanded.value = false
                    clipboardManager.setText(AnnotatedString(event.getPreviewText(false)))
                },
                enabled = true
            ) {
                Text("Copy Text")
            }
        }
    }

    @Composable
    fun ImageDialog(imageDialogOpen: MutableState<Boolean>) {
        var imageUri by remember { mutableStateOf<Uri?>(null) }

        val imagePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = {
                val attachments = MultiPicker.get(MultiPicker.MEDIA).getSelectedFiles(this, it.data)
                    .map {
                        it.toContentAttachmentData()
                    }
                Log.d("ATTACHMENTS", attachments.toString())
                chatRoomState.room.sendService().sendMedias(
                    attachments = attachments,
                    compressBeforeSending = true,
                    roomIds = emptySet()
                )
                AppState.session!!.syncService().startSync(true)
                imageDialogOpen.value = false
            }
        )

        val imagePickerCamera = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = {
                imageUri?.let {
                    MultiPicker.get(MultiPicker.CAMERA).getTakenPhoto(this, it)
                        ?.let {
                            chatRoomState.room.sendService().sendMedia(
                                attachment = it.toContentAttachmentData(),
                                compressBeforeSending = true,
                                roomIds = emptySet()
                            )
                            imageDialogOpen.value = false
                        }
                    imageUri = null
                }
            }

        )

        Dialog(
            onDismissRequest = {
                imageDialogOpen.value = false
            },
            content = {
                Surface(shape = MaterialTheme.shapes.medium) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Upload Image", fontSize = 18.sp, modifier = Modifier.padding(6.dp))
                        Button(onClick = {
                            MultiPicker.get(MultiPicker.IMAGE).startWith(imagePicker)
                        }) {
                            Text("Choose from Gallery")
                        }
                        Divider()
                        Text("Use Camera", fontSize = 18.sp, modifier = Modifier.padding(6.dp))
                        Button(onClick = {
                            imageUri = MultiPicker.get(MultiPicker.CAMERA)
                                .startWithExpectingFile(this@RoomActivity, imagePickerCamera)
                        }) {
                            Text("Take picture with Camera")
                        }
                    }
                }
            }
        )
    }

    @Composable
    fun DeleteDialog(event: TimelineEvent, deleteDialogOpen: MutableState<Boolean>) {
        AlertDialog(
            onDismissRequest = {
                deleteDialogOpen.value = false
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
                    deleteDialogOpen.value = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(onClick = {
                    deleteDialogOpen.value = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    override fun finish() {
        super.finish()
        if (this::chatRoomState.isInitialized) {
            chatRoomState.room.typingService().userStopsTyping()
            chatRoomState.stopSync()
        }
    }
}