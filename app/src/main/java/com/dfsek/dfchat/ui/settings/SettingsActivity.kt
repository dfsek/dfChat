package com.dfsek.dfchat.ui.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.ui.Modifier
import com.dfsek.dfchat.AppState
import kotlin.reflect.KClass

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colors = AppState.themeColors) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Settings()
                }
            }
        }
    }

    @Composable
    @Preview
    fun Settings() {
        Column {
            SettingsTopBar("Settings")
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    SettingsItem("Account", AccountActivity::class)
                }
                item {
                    SettingsItem("Verification", VerificationActivity::class)
                }
                item {
                    SettingsItem("Theme", ThemeActivity::class)
                }
            }
        }
    }

    @Composable
    fun SettingsItem(text: String, activity: KClass<out Activity>) {
        Row(modifier = Modifier.clickable {
            startActivity(Intent(applicationContext, activity.java))
        }.fillMaxWidth()) {
            Text(text, fontSize = 30.sp)
        }
    }
}