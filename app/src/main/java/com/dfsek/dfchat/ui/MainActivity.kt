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
import com.dfsek.dfchat.state.ChatRoomState
import com.dfsek.dfchat.util.SettingsDropdown
import com.dfsek.dfchat.util.getRawText
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.getRoom
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
                        rooms = it
                    }
            }
            LazyColumn {
                items(rooms.sortedBy { it.latestPreviewableEvent?.root?.originServerTs }.reversed()) {
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
        }) {
            val avatarUrl = room.avatarUrl
            val name = room.name
            val lastContent = room.latestPreviewableEvent

            avatarUrl.let {
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
                Text(name ?: "", fontSize = 18.sp)
                Text(lastContent?.getRawText() ?: "", fontSize = 12.sp)
            }
        }
    }
}