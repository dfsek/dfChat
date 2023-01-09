package com.dfsek.dfchat.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

@Composable
fun Activity.SettingsTopBar(title: String) {
    Box (modifier = Modifier
        .background(MaterialTheme.colors.surface)
        .fillMaxWidth()) {
        BackButton(modifier = Modifier.align(Alignment.CenterStart))
        Text(title, fontSize = 24.sp, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colors.onSurface)
    }
}

@Composable
fun Activity.BackButton(modifier: Modifier = Modifier) {
    IconButton(onClick = {
        finish()
    }, modifier = modifier) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowLeft,
            contentDescription = "Back"
        )
    }
}

@Composable
fun Activity.SettingsDropdown(modifier: Modifier = Modifier, refresh: () -> Unit = {}) {
    Box(
        modifier = modifier,
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
                    refresh()
                    expanded = false
                },
                enabled = true
            ) {
                Text("Refresh")
            }
        }
    }
}