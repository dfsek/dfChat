package com.dfsek.dfchat.ui.rooms

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.dfsek.dfchat.ui.RoomTopBar
import com.dfsek.dfchat.ui.SelectionUI
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams

class DirectMessagesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                val selectionUIOpen = remember { mutableStateOf(false) }
                Column {
                    RoomTopBar("Direct Messages", modifier = Modifier, selectionUIOpen)
                    RoomList(remember { RoomSummaryQueryParams.Builder().build() }) {
                        val reversed = it.sortedBy { it.latestPreviewableEvent?.root?.originServerTs }.reversed()
                        reversed.filter { it.isDirect }
                    }
                }
                SelectionUI(selectionUIOpen)
            }
        }
    }
}