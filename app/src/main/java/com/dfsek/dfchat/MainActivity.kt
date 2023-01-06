package com.dfsek.dfchat

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.dfsek.dfchat.state.RoomsState
import com.dfsek.dfchat.ui.RoomEntry

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("Main", "Starting main activity")
        setContent {
            AllRoomsScreen(this, applicationContext)
        }
    }
    @Composable
    fun AllRoomsScreen(activity: Activity, applicationContext: Context) {
        Column {
            SettingsDropdown(applicationContext, activity)
            RoomList(activity)
        }
    }

    @Composable
    fun RoomList(activity: Activity) {
        SessionHolder.currentSession?.let {
            val scope = rememberCoroutineScope()
            val lifecycleOwner = LocalLifecycleOwner.current
            val roomsState = remember { RoomsState(it, scope, lifecycleOwner) }
            LazyColumn {
                items(roomsState.rooms) {
                    RoomEntry(roomsState, it, activity)
                }
            }
        }
    }
}