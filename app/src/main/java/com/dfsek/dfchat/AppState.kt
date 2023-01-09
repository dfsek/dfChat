package com.dfsek.dfchat

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dfsek.dfchat.util.THEME_KEY
import com.dfsek.dfchat.util.THEME_PREFS
import org.matrix.android.sdk.api.session.Session


object AppState {
    var session: Session? by mutableStateOf(null)
    var themeColors: Colors by mutableStateOf(lightColors()) // immediately overwritten.
    fun Context.updateTheme() = getSharedPreferences(THEME_PREFS, Application.MODE_PRIVATE).let {
        val theme = it.getString(THEME_KEY, "System")
        themeColors = when(theme) {
            "System" -> if(resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) darkColors() else lightColors()
            "Light" -> lightColors()
            "Dark" -> darkColors()
            else -> {
                Log.e("INVALID THEME", "No such theme $theme, defaulting to light theme.")
                lightColors()
            }
        }
    }
}