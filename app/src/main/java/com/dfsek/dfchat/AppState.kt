package com.dfsek.dfchat

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.util.Log
import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dfsek.dfchat.ui.settings.GeneralActivity
import com.dfsek.dfchat.util.GENERAL_PREFS
import com.dfsek.dfchat.util.THEME_KEY
import com.dfsek.dfchat.util.THEME_PREFS
import com.dfsek.dfchat.util.toColor
import org.matrix.android.sdk.api.session.Session
import kotlin.math.roundToInt


object AppState {
    var session: Session? by mutableStateOf(null)
    var themeColors: Colors by mutableStateOf(lightColors()) // immediately overwritten.

    object Preferences {
        var spacesAvatarSize by mutableStateOf(-1)
        var dmAvatarSize by mutableStateOf(-1)
        var roomAvatarSize by mutableStateOf(-1)

        fun Context.updatePrefs() = getSharedPreferences(GENERAL_PREFS, Application.MODE_PRIVATE).let {
            spacesAvatarSize = it.getInt(GeneralActivity.AVATAR_SIZE_SPACES_KEY, 36)
            dmAvatarSize = it.getInt(GeneralActivity.AVATAR_SIZE_DMS_KEY, 48)
            roomAvatarSize = it.getInt(GeneralActivity.AVATAR_SIZE_ROOMS_KEY, 48)
        }
    }

    fun Context.updateTheme() = getSharedPreferences(THEME_PREFS, Application.MODE_PRIVATE).let {
        val theme = it.getString(THEME_KEY, "System")
        themeColors = when(theme) {
            "System" -> if(resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) darkColors() else lightColors()
            "Light" -> lightColors()
            "Dark" -> darkColors()
            "Custom" -> parseCustomTheme(it)
            else -> {
                Log.e("INVALID THEME", "No such theme $theme, defaulting to light theme.")
                lightColors()
            }
        }
    }

    private fun parseCustomTheme(preferences: SharedPreferences): Colors {
        val default = lightColors()
        return Colors(
            primary = preferences.getString("custom.Primary", null)?.toColor() ?: default.primary,
            primaryVariant = preferences.getString("custom.Primary Variant", null)?.toColor() ?: default.primaryVariant,
            onPrimary = preferences.getString("custom.Primary Text", null)?.toColor() ?: default.onPrimary,
            secondary = preferences.getString("custom.Secondary", null)?.toColor() ?: default.secondary,
            secondaryVariant = preferences.getString("custom.Secondary Variant", null)?.toColor() ?: default.secondaryVariant,
            onSecondary = preferences.getString("custom.Secondary Text", null)?.toColor() ?: default.onSecondary,
            background = preferences.getString("custom.Background", null)?.toColor() ?: default.background,
            onBackground = preferences.getString("custom.Background Text", null)?.toColor() ?: default.onBackground,
            surface = preferences.getString("custom.Surface", null)?.toColor() ?: default.surface,
            onSurface = preferences.getString("custom.Surface Text", null)?.toColor() ?: default.onSurface,
            error = preferences.getString("custom.Error", null)?.toColor() ?: default.error,
            onError = preferences.getString("custom.Error Text", null)?.toColor() ?: default.onError,
            isLight = preferences.getBoolean("custom.isLight", false),
        )
    }
}