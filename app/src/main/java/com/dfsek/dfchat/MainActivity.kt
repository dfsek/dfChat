package com.dfsek.dfchat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.core.model.RoomId
import kotlin.streams.toList

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("Main", "Starting main activity")
        setContent {
            Column {
                val allRooms = remember { mutableStateOf(emptyMap<RoomId, Room?>()) }
                SettingsDropdown()
                AllRooms(allRooms.value.values.stream().map {
                    if(it?.name?.explicitName != null) {
                        it.name!!.explicitName as String
                    } else if(it?.name?.heroes?.isNotEmpty() == true) {
                        it.name!!.heroes[0].full as String
                    } else {
                        it?.name.toString()
                    }
                }.toList())
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
    fun Room(name: String) {
        Column {
            Text(text = name)
        }
    }

    @Preview
    @Composable
    fun PreviewRoom() {
        Room("computer - general")
    }

    @Composable
    fun AllRooms(rooms: List<String>) {
        LazyColumn {
            items(rooms) {
                Room(it)
            }
        }
    }

    @Preview
    @Composable
    fun PreviewAllRooms() {
        AllRooms(
            listOf(
                "computer - general",
                "computer - stramurtcraft",
                "dfsek",
                "test"
            )
        )
    }
}