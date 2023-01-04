package com.dfsek.dfchat.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.dfsek.dfchat.AccountActivity
import com.dfsek.dfchat.RoomActivity
import com.dfsek.dfchat.SettingsDropdown
import com.dfsek.dfchat.parseEvent
import com.dfsek.dfchat.state.LoginState
import com.dfsek.dfchat.state.RoomsState
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.store.Room


@Composable
fun AllRoomsScreen(activity: Activity, applicationContext: Context) {
    Column {
        SettingsDropdown(applicationContext, activity)
        RoomList(activity)
    }
}

@Composable
fun RoomList(activity: Activity) {
    LoginState.matrixClient?.let {
        val scope = rememberCoroutineScope()
        val roomsState = remember { RoomsState(it, scope) }
        LazyColumn {
            items(roomsState.rooms.values.toList().sortedBy { it.first?.event?.originTimestamp }.reversed()) {
                RoomEntry(roomsState, it.second, activity)
            }
        }
    }
}

@Composable
fun RoomEntry(state: RoomsState, room: Room, activity: Activity) {
    Row(modifier = Modifier.clickable {
        activity.startActivity(Intent(activity, RoomActivity::class.java).apply {
            putExtra("room", room.roomId.full)
        })
    }) {
        val chatRoomState = state.getRoom(room.roomId)
        var bytes by remember { mutableStateOf<ByteArray?>(null) }
        var name by remember { mutableStateOf("") }
        var lastContent by remember { mutableStateOf("") }
        LaunchedEffect(room) {
            launch {
                chatRoomState.getRoomAvatar {
                    bytes = it
                }
            }
            launch {
                chatRoomState.getName {
                    name = it
                }
            }
            launch {
                chatRoomState.getLastMessage {
                    lastContent = parseEvent(it.event.content)
                }
            }
        }

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
                modifier = Modifier.size(64.dp).clip(CircleShape)
            )
        } ?: Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.Cyan)
        )

        Column {
            Text(name, fontSize = 18.sp)
            Text(lastContent, fontSize = 12.sp)
        }
    }
}