package com.dfsek.dfchat.state

import android.util.Log
import kotlinx.coroutines.flow.first
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key
import net.folivo.trixnity.client.key.DeviceTrustLevel
import net.folivo.trixnity.clientserverapi.model.devices.Device

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
}