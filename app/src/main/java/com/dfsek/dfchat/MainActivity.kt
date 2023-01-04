package com.dfsek.dfchat

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.dfsek.dfchat.ui.AllRoomsScreen

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("Main", "Starting main activity")
        setContent {
            AllRoomsScreen(this, applicationContext)
        }
    }
}