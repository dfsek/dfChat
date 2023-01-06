package com.dfsek.dfchat.state

import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.verification.VerificationService

class VerificationState(val client: Session) {
    val devices = client.cryptoService().getMyDevicesInfoLive()

    fun registerListener(listener: VerificationService.Listener) = client.cryptoService().verificationService().addListener(listener)
}