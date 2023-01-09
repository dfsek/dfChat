package com.dfsek.dfchat.ui

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.dfsek.dfchat.AppState
import com.dfsek.dfchat.ui.rooms.DirectMessagesActivity
import com.dfsek.dfchat.ui.rooms.GroupMessagesActivity
import com.dfsek.dfchat.ui.settings.SettingsDropdown
import com.dfsek.dfchat.util.getAvatarUrl
import com.dfsek.dfchat.util.getPreviewText
import org.matrix.android.sdk.api.query.SpaceFilter
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.model.RoomSummary
import org.matrix.android.sdk.api.session.space.Space
import java.util.Stack

@Composable
fun Activity.RootSpacesSelection(modifier: Modifier = Modifier, isOpen: MutableState<Boolean>) {
    Column(modifier = modifier
        .fillMaxWidth(0.75f)
        .fillMaxHeight()
        .background(MaterialTheme.colors.background)
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) {
        var rootSpaces by remember { mutableStateOf<List<Room>>(emptyList()) }
        val session = remember { AppState.session as Session }
        val spaceStack = remember { Stack<Space>() }
        var currentRooms by remember { mutableStateOf<List<Room>?>(null) }
        LaunchedEffect(AppState.session) {
            rootSpaces = session
                .spaceService()
                .getRootSpaceSummaries()
                .map { session.spaceService().getSpace(it.roomId)?.asRoom() as Room }
            if (currentRooms == null) currentRooms = rootSpaces
            Log.d("Root Spaces", "Populating root spaces, Size: ${rootSpaces.size}")
        }

        Row {
            IconButton(onClick = {
                if (spaceStack.empty()) {
                    isOpen.value = false
                } else {
                    spaceStack.pop()
                    currentRooms = if (spaceStack.empty()) {
                        rootSpaces.map { it.roomSummary() as RoomSummary }
                    } else {
                        session
                            .spaceService()
                            .getSpaceSummaries(
                                RoomSummaryQueryParams.Builder()
                                    .apply { spaceFilter = SpaceFilter.ActiveSpace(spaceStack.peek().spaceId) }.build()
                            )
                    }.map { session.roomService().getRoom(it.roomId) as Room }
                }
            }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Close"
                )
            }
        }
        val avatarSize = 36
        LazyColumn {
            if(spaceStack.empty()) {
                item {
                    Row(modifier = Modifier.fillMaxWidth().clickable {
                        runOnUiThread {
                            startActivity(Intent(applicationContext, DirectMessagesActivity::class.java))
                            finish()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Direct Messages",
                            modifier = Modifier.size(avatarSize.dp)
                        )
                        Text("Direct Messages", fontSize = 18.sp)
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth().clickable {
                        runOnUiThread {
                            startActivity(Intent(applicationContext, GroupMessagesActivity::class.java))
                            finish()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Group Messages",
                            modifier = Modifier.size(avatarSize.dp)
                        )
                        Text("Group Messages", fontSize = 18.sp)
                    }
                }
            }

            items(currentRooms ?: emptyList(), key = {
                it.roomId
            }) { room ->
                Row(modifier = Modifier.clickable {
                    room.asSpace()?.let {
                        spaceStack.push(it)
                        currentRooms = session
                            .spaceService()
                            .getSpaceSummaries(
                                RoomSummaryQueryParams.Builder()
                                    .apply { spaceFilter = SpaceFilter.ActiveSpace(it.spaceId) }.build()
                            )
                            .filter { !it.isDirect } // don't include DMs in space previews
                            .map { session.roomService().getRoom(it.roomId) as Room }
                    } ?: startActivity(Intent(applicationContext, RoomActivity::class.java).apply {
                        putExtra("room", room.roomId)
                    })
                }.fillMaxWidth()) {
                    var avatarUrl by remember { mutableStateOf<String?>(null) }
                    val name = remember { room.roomSummary()?.displayName ?: "" }
                    val lastContent = remember { room.roomSummary()?.latestPreviewableEvent }


                    LaunchedEffect(room) {
                        avatarUrl = getAvatarUrl(room.roomSummary()?.avatarUrl, avatarSize)
                    }

                    avatarUrl?.let {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(it)
                                .crossfade(true)
                                .decoderFactory(BitmapFactoryDecoder.Factory())
                                .build(),
                            contentScale = ContentScale.Fit,
                            contentDescription = null,
                            modifier = Modifier.size(avatarSize.dp).clip(CircleShape)
                        )
                    } ?: Box(
                        modifier = Modifier.size(avatarSize.dp).clip(CircleShape).background(Color.Cyan)
                    )

                    Column {
                        Text(name, fontSize = 18.sp)
                        Text(lastContent?.getPreviewText() ?: "", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun BackButton(modifier: Modifier = Modifier, isSelectionOpen: MutableState<Boolean>) {
    IconButton(onClick = {
        isSelectionOpen.value = true
    }, modifier = modifier) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "Spaces"
        )
    }
}

@Composable
fun Activity.TopBar(
    name: String,
    modifier: Modifier,
    isSelectionOpen: MutableState<Boolean>
) {
    Box (modifier = modifier
        .background(MaterialTheme.colors.surface)
        .fillMaxWidth()) {
        BackButton(modifier = Modifier.align(Alignment.CenterStart), isSelectionOpen)
        Text(name, fontSize = 24.sp, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colors.onSurface)
        SettingsDropdown(modifier = Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
fun Activity.SelectionUI(isSelectionOpen: MutableState<Boolean>) {
    if(isSelectionOpen.value) {
        Box(modifier = Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
            isSelectionOpen.value = false
        }) {
            RootSpacesSelection(modifier = Modifier, isSelectionOpen)
        }
    }
}