package com.dfsek.dfchat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.matrix.android.sdk.api.session.Session


object SessionHolder {
    var currentSession: Session? by mutableStateOf(null)
}