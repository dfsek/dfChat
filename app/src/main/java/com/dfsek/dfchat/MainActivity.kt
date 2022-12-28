package com.dfsek.dfchat

import android.content.Intent
import android.os.Bundle
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

class MainActivity : AppCompatActivity() {
    private val listItems = arrayOf(Pair("Settings", ::launchSettings))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column {
                settingsDropdown()
            }
        }
    }

    @Composable
    @Preview
    fun settingsDropdown() {
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
                listItems.forEachIndexed { _, itemValue ->
                    DropdownMenuItem(
                        onClick = {
                            itemValue.second()
                            expanded = false
                        },
                        enabled = true
                    ) {
                        Text(text = itemValue.first)
                    }
                }
            }
        }
    }

    @Composable
    fun room(name: String) {
        Column {
            Text(text = name)
        }
    }

    @Preview
    @Composable
    fun previewRoom() {
        room("computer - general")
    }

    @Composable
    fun allRooms(rooms: List<String>) {
        LazyColumn {
            items(rooms) {
                room(it)
            }
        }
    }

    @Preview
    @Composable
    fun previewAllRooms() {
        allRooms(listOf(
            "computer - general",
            "computer - stramurtcraft",
            "dfsek",
            "test"
        ))
    }

    private fun launchSettings(): Boolean {
        startActivity(Intent(applicationContext, SettingsActivity::class.java))
        return true
    }
}