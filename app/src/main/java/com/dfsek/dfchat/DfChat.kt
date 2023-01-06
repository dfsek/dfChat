package com.dfsek.dfchat

import android.app.Application
import android.content.Context
import android.util.Log
import com.dfsek.dfchat.util.RoomDisplayNameFallbackProviderImpl
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.MatrixConfiguration

class DfChat : Application() {
    private lateinit var matrix: Matrix

    override fun onCreate() {
        super.onCreate()
        createMatrix()
        val lastSession = matrix.authenticationService().getLastAuthenticatedSession()
        if (lastSession != null) {
            Log.i("Session", "Restoring previous session.")
            SessionHolder.currentSession = lastSession
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