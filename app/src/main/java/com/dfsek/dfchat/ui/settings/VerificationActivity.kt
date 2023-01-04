package com.dfsek.dfchat.ui.settings

import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dfsek.dfchat.state.VerificationState
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.key.DeviceTrustLevel
import net.folivo.trixnity.client.verification
import net.folivo.trixnity.clientserverapi.model.devices.Device
import com.dfsek.dfchat.state.LoginState
import com.dfsek.dfchat.userString
import kotlinx.coroutines.launch

class VerificationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginState.matrixClient?.let {
                val state = remember { VerificationState(it) }
                Devices(state)
            }
        }
    }

    @Composable
    fun Devices(state: VerificationState) {
        var devices by remember { mutableStateOf<List<Pair<Device, DeviceTrustLevel>>>(emptyList()) }
        LaunchedEffect(state) {
            state.getDevices {
                    devices = it
            }
        }

        Column {
            devices.forEach {
                Device(it.first, it.second)
                Divider(startIndent = 8.dp, thickness = 1.dp, color = Color.Black)
            }
        }
    }

    @Composable
    fun Device(device: Device, verification: DeviceTrustLevel) {
        Column {
            Text(device.deviceId)
            device.displayName?.let {  Text("($it)") }
            device.lastSeenIp?.let { Text("Last seen at $it") }
            Text("Verification: ${verification.userString()}")
        }
    }
}