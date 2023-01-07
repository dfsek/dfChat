package com.dfsek.dfchat.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
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
import com.dfsek.dfchat.util.SettingsDropdown
import com.dfsek.dfchat.util.getAvatarUrl
import com.dfsek.dfchat.util.getPreviewText
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.model.RoomSummary

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("Main", "Starting main activity")
        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                AllRoomsScreen(this, applicationContext)
            }
        }
    }

    @Composable
    fun AllRoomsScreen(activity: Activity, applicationContext: Context) {
        Column {
            SettingsDropdown(applicationContext, activity)
            RoomList(activity)
        }
    }

    @Composable
    fun RoomList(activity: Activity) {
        SessionHolder.currentSession?.let { session ->
            val lifecycleOwner = LocalLifecycleOwner.current
            var rooms: List<RoomSummary> by remember { mutableStateOf(emptyList()) }
            LaunchedEffect(session) {
                session.roomService().getRoomSummariesLive(RoomSummaryQueryParams.Builder().build())
                    .observe(lifecycleOwner) {
                        val reversed = it.sortedBy { it.latestPreviewableEvent?.root?.originServerTs }.reversed()
                        Log.d("Rooms Reversed", reversed.equals(rooms).toString())
                        rooms = reversed
                    }
            }
            LazyColumn {
                items(rooms, key = {
                    it.roomId
                }) {
                    Log.d("ROOM", it.roomId)
                    RoomEntry(it, activity)
                }
            }
        }
    }

    @Composable
    fun RoomEntry(room: RoomSummary, activity: Activity) {
        Row(modifier = Modifier.clickable {
            activity.startActivity(Intent(activity, RoomActivity::class.java).apply {
                putExtra("room", room.roomId)
            })
        }.fillMaxWidth()) {
            var avatarUrl by remember { mutableStateOf<String?>(null) }
            val name = remember {room.displayName }
            val lastContent = remember { room.latestPreviewableEvent}

            LaunchedEffect(room) {
                avatarUrl = getAvatarUrl(room.avatarUrl)
            }

            avatarUrl?.let {
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
                Text(lastContent?.getPreviewText() ?: "", fontSize = 12.sp)
            }
        }
    }
}