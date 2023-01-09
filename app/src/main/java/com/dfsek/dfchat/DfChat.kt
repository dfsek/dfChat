package com.dfsek.dfchat

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import com.dfsek.dfchat.util.RoomDisplayNameFallbackProviderImpl
import com.dfsek.dfchat.util.THEME_KEY
import com.dfsek.dfchat.util.THEME_PREFS
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.MatrixConfiguration

class DfChat : Application() {
    private lateinit var matrix: Matrix

    override fun onCreate() {
        super.onCreate()
        createMatrix()
        val lastSession = matrix.authenticationService().getLastAuthenticatedSession()
        getSharedPreferences(THEME_PREFS, MODE_PRIVATE).let {
            val theme = it.getString(THEME_KEY, "System")
            AppState.themeColors = when(theme) {
                "System" -> if(resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) darkColors() else lightColors()
                "Light" -> lightColors()
                "Dark" -> darkColors()
                else -> {
                    Log.e("INVALID THEME", "No such theme $theme, defaulting to light theme.")
                    lightColors()
                }
            }
        }
        if (lastSession != null) {
            Log.i("Session", "Restoring previous session.")
            AppState.session = lastSession
            lastSession.open()
            lastSession.syncService().startSync(true)
        }
    }
    private fun createMatrix() {
        matrix = Matrix(
            context = this,
            matrixConfiguration = MatrixConfiguration(
                roomDisplayNameFallbackProvider = RoomDisplayNameFallbackProviderImpl
            )
        )
    }

    companion object {
        fun getMatrix(context: Context): Matrix {
            return (context.applicationContext as DfChat).matrix
        }
    }

}