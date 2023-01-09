package com.dfsek.dfchat

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import com.dfsek.dfchat.AppState.Preferences.updatePrefs
import com.dfsek.dfchat.AppState.updateTheme
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
        updateTheme()
        updatePrefs()
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