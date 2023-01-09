package com.dfsek.dfchat.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.dfsek.dfchat.util.THEME_KEY
import com.dfsek.dfchat.util.THEME_PREFS

class ThemeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val preferences = remember {getSharedPreferences(THEME_PREFS, MODE_PRIVATE) }

            ThemeSelector(preferences)
        }
    }

    @Composable
    fun ThemeSelector(preferences: SharedPreferences) {
        val themes = remember { listOf("System", "Light", "Dark", "Custom") }
        var currentTheme by remember { mutableStateOf(preferences.getString(THEME_KEY, themes[0])) }

        fun setTheme(theme: String) {
            preferences.edit {
                putString(THEME_KEY, theme)
            }
            currentTheme = theme
        }

        Column {
            Text("Theme", fontSize = 18.sp)
            themes.forEach {
                Row(modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (it == currentTheme),
                            onClick = {
                                setTheme(it)
                            }
                        )
                        .padding(horizontal = 16.dp)
                ) {
                    RadioButton(
                        selected = (it == currentTheme),
                        onClick = { setTheme(it) }
                    )
                    Text(
                        text = it,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }

    @Composable
    fun CustomThemeUI(preferences: SharedPreferences) {

    }
}