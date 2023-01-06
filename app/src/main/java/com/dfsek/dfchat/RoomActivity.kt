package com.dfsek.dfchat

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.dfsek.dfchat.state.ChatRoomState
import com.dfsek.dfchat.ui.RoomUI

class RoomActivity : AppCompatActivity() {
    private lateinit var chatRoomState: ChatRoomState
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val lifecycleOwner = LocalLifecycleOwner.current
            intent.getStringExtra("room")?.let { roomId ->
                SessionHolder.currentSession?.let {
                    val state = remember {
                        ChatRoomState(
                            roomId = roomId,
                            client = it,
                            lifecycleOwner
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
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        if (this::chatRoomState.isInitialized) {
            chatRoomState.stopSync()
        }
    }
}