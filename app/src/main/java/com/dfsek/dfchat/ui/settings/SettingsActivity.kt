package com.dfsek.dfchat.ui.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import kotlin.reflect.KClass

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Settings()
        }
    }

    @Composable
    @Preview
    fun Settings() {
        LazyColumn {
            item {
                SettingsItem("Account", AccountActivity::class)
            }
            item {
                SettingsItem("Verification", VerificationActivity::class)
            }
        }
    }

    @Composable
    fun SettingsItem(text: String, activity: KClass<out Activity>) {
        Row(modifier = Modifier.clickable {
            startActivity(Intent(applicationContext, activity.java))
            finish()
        }) {
            Text(text, fontSize = 30.sp)
        }
    }
}