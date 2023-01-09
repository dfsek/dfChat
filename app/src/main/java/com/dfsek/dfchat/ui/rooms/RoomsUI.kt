package com.dfsek.dfchat.ui.rooms

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest
import com.dfsek.dfchat.AppState
import com.dfsek.dfchat.ui.RoomActivity
import com.dfsek.dfchat.util.getAvatarUrl
import com.dfsek.dfchat.util.getPreviewText
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.model.RoomSummary



@Composable
fun Activity.RoomEntry(room: RoomSummary) {
    Row(modifier = Modifier.clickable {
        startActivity(Intent(applicationContext, RoomActivity::class.java).apply {
            putExtra("room", room.roomId)
        })
    }.fillMaxWidth()) {
        var avatarUrl by remember { mutableStateOf<String?>(null) }
        val name = remember { room.displayName }
        val lastContent = remember { room.latestPreviewableEvent }

        LaunchedEffect(room) {
            avatarUrl = getAvatarUrl(room.avatarUrl)
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
                modifier = Modifier.size(AppState.Preferences.dmAvatarSize.dp).clip(CircleShape)
            )
        } ?: Box(
            modifier = Modifier.size(AppState.Preferences.dmAvatarSize.dp).clip(CircleShape).background(Color.Cyan)
        )

        Column {
            Text(name, fontSize = 18.sp)
            Text(lastContent?.getPreviewText() ?: "", fontSize = 12.sp)
        }
    }
}

@Composable
fun Activity.RoomList(
    queryParams: RoomSummaryQueryParams,
    filter: (List<RoomSummary>) -> List<RoomSummary>
) {
    AppState.session?.let { session ->
        val lifecycleOwner = LocalLifecycleOwner.current
        var rooms: List<RoomSummary> by remember { mutableStateOf(emptyList()) }
        LaunchedEffect(session) {
            session.roomService().getRoomSummariesLive(queryParams)
                .observe(lifecycleOwner) {
                    rooms = filter(it)
                }
        }
        LazyColumn {
            items(rooms, key = {
                it.roomId
            }) {
                Log.d("ROOM", it.roomId)
                RoomEntry(it)
            }
        }
    }
}