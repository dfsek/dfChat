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
import com.dfsek.dfchat.state.LoginState
import com.dfsek.dfchat.ui.RoomUI
import net.folivo.trixnity.core.model.RoomId

class RoomActivity : AppCompatActivity() {
    private lateinit var chatRoomState: ChatRoomState
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            intent.getStringExtra("room")?.let { roomId ->
                LoginState.matrixClient?.let {
                    val state = remember {
                        ChatRoomState(
                            roomId = RoomId(roomId),
                            client = it
                        ).also {
                            it.startSync()
                            chatRoomState = it
                        }

                    }
                    RoomUI(
                        roomState = state,
                        modifier = Modifier,
                        applicationContext = applicationContext,
                        activity = this
                    )
                    LaunchedEffect(roomId) {
                        state.fetchMessages()
                    }
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        if(this::chatRoomState.isInitialized) {
            chatRoomState.stopSync()
        }
    }
}