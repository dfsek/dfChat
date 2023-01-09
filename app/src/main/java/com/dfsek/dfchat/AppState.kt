package com.dfsek.dfchat

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.matrix.android.sdk.api.session.Session


object AppState {
    var session: Session? by mutableStateOf(null)
    var themeColors: Colors by mutableStateOf(lightColors()) // immediately overwritten.

}