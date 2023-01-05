package com.dfsek.dfchat.state

import android.util.Log
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key
import net.folivo.trixnity.client.key.DeviceTrustLevel
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.client.verification.*
import net.folivo.trixnity.clientserverapi.model.devices.Device
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationMethod

class VerificationState(val client: MatrixClient) {
    suspend fun getDevices(consumer: (List<Pair<Device, DeviceTrustLevel>>) -> Unit) {
        Log.d("Fetching devices", "...")
        client.api.devices.getDevices()
            .onSuccess {
                consumer(it.map {
                    Log.d("Device found", it.toString())
                    Pair(it, client.key.getTrustLevel(client.userId, it.deviceId).first())
                })
            }
            .onFailure {
                it.printStackTrace()
            }
    }

    suspend fun maybeDeviceVerification(consumer: (SelfVerificationMethod.CrossSignedDeviceVerification) -> Unit) {
        client.verification.getSelfVerificationMethods()
            .collectLatest {
                if (it is VerificationService.SelfVerificationMethods.CrossSigningEnabled) {
                    it.methods
                        .forEach {
                            Log.d("Verification method", it.toString())
                            if (it is SelfVerificationMethod.CrossSignedDeviceVerification) {
                                consumer(it)
                                return@collectLatest
                            }
                        }
                }
            }
    }
}