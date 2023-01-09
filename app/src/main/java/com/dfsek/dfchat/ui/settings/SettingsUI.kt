package com.dfsek.dfchat.ui.settings

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
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