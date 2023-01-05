package com.dfsek.dfchat.ui.settings

import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Button
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.verification.*
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationMethod

class VerificationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginState.matrixClient?.let {
                Column {
                    val state = remember { VerificationState(it) }
                    var verification by remember {
                        mutableStateOf<SelfVerificationMethod.CrossSignedDeviceVerification?>(
                            null
                        )
                    }

                    verification?.let {
                        VerifySelf(it)
                    }

                    LaunchedEffect(state) {
                        state.maybeDeviceVerification {
                            verification = it
                        }
                    }

                    Devices(state)
                }
            }
        }
    }

    @Composable
    fun VerifySelf(verification: SelfVerificationMethod.CrossSignedDeviceVerification) {
        var active by remember { mutableStateOf<ActiveDeviceVerification?>(null) }
        var error by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()
        Column {
            active?.let {
                var verificationString by remember {
                    mutableStateOf<Pair<ActiveSasVerificationState.ComparisonByUser, String>?>(
                        null
                    )
                }

                if (verificationString != null) {
                    Text(text = verificationString!!.second)
                    Button(onClick = {
                        CoroutineScope(Dispatchers.Default).launch {
                            verificationString!!.first.match()
                        }
                        runOnUiThread {
                            finish()
                        }
                    }) {
                        Text("Emojis are real?")
                    }
                }

                LaunchedEffect(it) {
                    it.state
                        .collectLatest {
                            launch {
                                Log.d("Verification status", it.toString())
                                if (it is ActiveVerificationState.Ready) {
                                    it.start(VerificationMethod.Sas)
                                } else if (it is ActiveVerificationState.Start) {
                                    val clientSasVerification = it.method as ActiveSasVerificationMethod
                                    clientSasVerification
                                        .state
                                        .collectLatest {
                                            if (it is ActiveSasVerificationState.ComparisonByUser) {
                                                Log.d("Verification status", it.toString())
                                                verificationString = Pair(
                                                    it,
                                                    it.emojis.map { it.second }.joinToString(",")
                                                )
                                            }
                                        }
                                }

                            }
                        }
                }
            } ?: Button(onClick = {
                scope.launch {
                    verification.createDeviceVerification()
                        .onSuccess {
                            active = it
                        }
                        .onFailure {
                            error = it.message.toString()
                            it.printStackTrace()
                        }
                }
            }) {
                Text("Start Verification")
            }
            Text(error)
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
            device.displayName?.let { Text("($it)") }
            device.lastSeenIp?.let { Text("Last seen at $it") }
            Text("Verification: ${verification.userString()}")
        }
    }
}