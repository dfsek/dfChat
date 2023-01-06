package com.dfsek.dfchat.state

import android.util.Log
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.crosssigning.DeviceTrustLevel

class VerificationState(val client: Session) {
    suspend fun getDevices(consumer: (List<Pair<String, DeviceTrustLevel>>) -> Unit) {
        Log.d("Fetching devices", "...")

    }

}