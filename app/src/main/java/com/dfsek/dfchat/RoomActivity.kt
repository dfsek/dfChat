package com.dfsek.dfchat

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.dfsek.dfchat.state.ChatRoomState
import com.dfsek.dfchat.ui.RoomUI
import net.folivo.trixnity.core.model.RoomId

class RoomActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            intent.getStringExtra("room")?.let { roomId ->
                AccountActivity.matrixClient?.let {
                    val state by remember { mutableStateOf(ChatRoomState(
                        roomId = RoomId(roomId),
                        client = it
                    )) }
                    RoomUI(
                        roomState = state,
                        modifier = Modifier,
                        applicationContext = applicationContext
                    )
                    LaunchedEffect(roomId) {
                        state.fetchMessages()
                    }
                }
            }
        }
    }
}