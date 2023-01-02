package com.dfsek.dfchat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.core.model.RoomId
import kotlin.streams.toList

class MainActivity : AppCompatActivity() {

    data class RoomInfo(
        val name: String,
        val avatarUrl: String?
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("Main", "Starting main activity")
        setContent {
            Column {
                val allRooms = remember { mutableStateOf(emptyMap<RoomId, Room?>()) }
                SettingsDropdown()
                AllRooms(allRooms.value.values.stream().map {
                    Pair(
                        if (it?.name?.explicitName != null) {
                            it.name!!.explicitName as String
                        } else if (it?.name?.heroes?.isNotEmpty() == true) {
                            it.name!!.heroes[0].full
                        } else {
                            it?.name.toString()
                        }, it?.avatarUrl
                    )
                }.toList(), AccountActivity.matrixClient)
                Log.i("Main", "Refreshing rooms")
                LaunchedEffect(AccountActivity.matrixClient) {
                    launch {
                        if (AccountActivity.matrixClient != null) {
                            AccountActivity.matrixClient!!.room.getAll()
                                .onEach {
                                    allRooms.value = it.mapValues {
                                        it.value.first()
                                    }
                                    it.forEach { roomId, _ ->
                                        Log.d("Room", roomId.full)
                                    }
                                }.collect()
                        }
                    }
                }
            }
        }
    }

    @Composable
    @Preview
    fun SettingsDropdown() {
        Box(
            contentAlignment = Alignment.Center
        ) {
            var expanded by remember {
                mutableStateOf(false)
            }
            IconButton(onClick = {
                expanded = true
            }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Open Options"
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
                        startActivity(Intent(applicationContext, SettingsActivity::class.java))
                        expanded = false
                    },
                    enabled = true
                ) {
                    Text("Settings")
                }
                DropdownMenuItem(
                    onClick = {
                        startActivity(Intent(applicationContext, MainActivity::class.java))
                        finish()
                        expanded = false
                    },
                    enabled = true
                ) {
                    Text("Refresh")
                }
            }
        }
    }

    @Composable
    fun Room(name: String, image: String?, client: MatrixClient?) {
        Row {
            if (client == null || image == null) {
                Box(
                    modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.Cyan)
                )
            } else {
                val bytes = remember { mutableStateOf<ByteArray?>(null) }
                if (bytes.value != null) {
                    Log.d("Channel Image", "Drawing image...")
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(bytes.value)
                            .crossfade(true)
                            .decoderFactory(BitmapFactoryDecoder.Factory())
                            .build(),
                        contentScale = ContentScale.Fit,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp).clip(CircleShape)
                    )
                }
                LaunchedEffect(image, client) {
                    launch {
                        client.api.media.download(image)
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
            Text(text = name)
        }
    }

    @Preview
    @Composable
    fun PreviewRoom() {
        Room("computer - general", null, null)
    }

    @Composable
    fun AllRooms(rooms: List<Pair<String, String?>>, client: MatrixClient?) {
        LazyColumn {
            items(rooms) {
                Room(it.first, it.second, client)
            }
        }
    }
}