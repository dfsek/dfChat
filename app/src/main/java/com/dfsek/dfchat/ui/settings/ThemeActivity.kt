package com.dfsek.dfchat.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.dfsek.dfchat.AppState
import com.dfsek.dfchat.AppState.updateTheme
import com.dfsek.dfchat.util.THEME_KEY
import com.dfsek.dfchat.util.THEME_PREFS
import com.dfsek.dfchat.util.toColor
import com.dfsek.dfchat.util.toHexString

class ThemeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val preferences = remember { getSharedPreferences(THEME_PREFS, MODE_PRIVATE) }

            MaterialTheme(colors = AppState.themeColors) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column {
                        SettingsTopBar("Theme")
                        ThemeSelector(preferences)
                    }
                }
            }
        }
    }

    @Composable
    fun ThemeSelector(preferences: SharedPreferences) {
        val themes = remember { listOf("System", "Light", "Dark", "Custom") }
        var currentTheme by remember { mutableStateOf(preferences.getString(THEME_KEY, themes[0])) }
        val customTheme = remember { mutableStateOf(AppState.themeColors) }

        fun setTheme(theme: String) {
            preferences.edit {
                putString(THEME_KEY, theme)
            }
            currentTheme = theme
            updateTheme()
        }

        LazyColumn {
            item {
                Text("Theme", fontSize = 18.sp)
            }
            items(themes) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (it == currentTheme),
                        onClick = {
                            setTheme(it)
                        }
                    )
                ) {
                    RadioButton(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        selected = (it == currentTheme),
                        onClick = { setTheme(it) }
                    )
                    Text(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        text = it,
                    )
                }
            }

            if (currentTheme == "Custom") {
                CustomThemeUI(preferences, customTheme)
            }
        }
    }

    fun LazyListScope.CustomThemeUI(preferences: SharedPreferences, customTheme: MutableState<Colors>) {
        var theme by customTheme

        Color(preferences, "Primary", theme.primary) {
            theme = theme.copy(primary = it)
        }
        Color(preferences, "Primary Variant", theme.primaryVariant) {
            theme = theme.copy(primaryVariant = it)
        }
        Color(preferences, "Primary Text", theme.onPrimary) {
            theme = theme.copy(onPrimary = it)
        }

        Color(preferences, "Secondary", theme.secondary) {
            theme = theme.copy(secondary = it)
        }
        Color(preferences, "Secondary Variant", theme.secondaryVariant) {
            theme = theme.copy(secondaryVariant = it)
        }
        Color(preferences, "Secondary Text", theme.onSecondary) {
            theme = theme.copy(onSecondary = it)
        }

        Color(preferences, "Background", theme.background) {
            theme = theme.copy(background = it)
        }
        Color(preferences, "Background Text", theme.onBackground) {
            theme = theme.copy(onBackground = it)
        }

        Color(preferences, "Surface", theme.surface) {
            theme = theme.copy(surface = it)
        }
        Color(preferences, "Surface Text", theme.onSurface) {
            theme = theme.copy(onSurface = it)
        }

        Color(preferences, "Error", theme.error) {
            theme = theme.copy(error = it)
        }
        Color(preferences, "Error Text", theme.onError) {
            theme = theme.copy(onError = it)
        }

        item {
            Row {
                Checkbox(checked = theme.isLight, onCheckedChange = {
                    preferences.edit {
                        putBoolean("custom.isLight", it)
                    }
                    theme = theme.copy(isLight = it)
                })
                Text("Dark Theme")
            }

        }
    }

    fun LazyListScope.Color(preferences: SharedPreferences, name: String, color: Color, update: (Color) -> Unit) {
        item {
            var error by remember { mutableStateOf("") }
            val text = remember { mutableStateOf(color.toHexString()) }
            Column {
                Text(text = name)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(color)
                            .align(Alignment.CenterVertically)
                    )
                    TextField(value = text.value, onValueChange = {
                        error = try {
                            text.value = it
                            val newColor = it.toColor()
                            preferences.edit {
                                putString("custom.$name", newColor.toHexString())
                            }
                            update(newColor)
                            updateTheme()
                            ""
                        } catch (e: IllegalArgumentException) {
                            "Could not parse color."
                        }
                    })
                }
                Text(text = error, color = MaterialTheme.colors.error)
            }
        }
    }
}