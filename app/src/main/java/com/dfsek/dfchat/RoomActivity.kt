package com.dfsek.dfchat

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import net.folivo.trixnity.api.client.e
import net.folivo.trixnity.core.model.EventId

class RoomActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
        }
    }

    @Composable
    fun Event(eventId: EventId) {
        Row {
            val userProfile = remember {
                mutableStateOf<ByteArray?>(null)
            }


        }
    }
}