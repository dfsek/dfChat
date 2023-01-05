package com.dfsek.dfchat.ui.settings

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dfsek.dfchat.openUrlInChromeCustomTab
import com.dfsek.dfchat.state.VerificationState
import net.folivo.trixnity.client.key.DeviceTrustLevel
import net.folivo.trixnity.clientserverapi.model.devices.Device
import com.dfsek.dfchat.state.LoginState
import com.dfsek.dfchat.userString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.key
import net.folivo.trixnity.client.media
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.verification.*
import net.folivo.trixnity.clientserverapi.client.UIA
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationRequest
import net.folivo.trixnity.clientserverapi.model.uia.AuthenticationType
import net.folivo.trixnity.core.model.events.m.key.verification.VerificationMethod

class VerificationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginState.matrixClient?.let {
                Column {
                    val state = remember { VerificationState(it) }
                    VerifySelf(state)

                    Devices(state)

                    BootstrapCrossSigning(state, rememberCoroutineScope())
                }
            }
        }
    }

    sealed interface VerifyState<T> {
        class Ready(override val verificationState: ActiveVerificationState.Ready) : VerifyState<ActiveVerificationState.Ready>{
            @Composable
            override fun VerifyUI(state: VerificationState, scope: CoroutineScope) {
                LaunchedEffect(verificationState) {
                    scope.launch {
                        verificationState.start(VerificationMethod.Sas)
                    }
                }
            }
        }

        class Start(override val verificationState: ActiveVerificationState.Start) : VerifyState<ActiveVerificationState.Start> {
            @Composable
            override fun VerifyUI(state: VerificationState, scope: CoroutineScope) {
                var verificationData by remember { mutableStateOf<Pair<ActiveSasVerificationState.ComparisonByUser, String>?>(null) }
                val clientSasVerification = verificationState.method as ActiveSasVerificationMethod
                LaunchedEffect(verificationState) {
                    clientSasVerification
                        .state
                        .collectLatest {
                            if (it is ActiveSasVerificationState.ComparisonByUser) {
                                Log.d("Verification status", it.toString())
                                verificationData = Pair(
                                    it,
                                    it.emojis.map { it.second }.joinToString(",")
                                )
                            } else if(it is ActiveSasVerificationState.TheirSasStart) {
                                it.accept()
                            }
                        }
                }
                verificationData?.let {
                    Text(it.second, fontSize = 18.sp)
                    Button(onClick = {
                        scope.launch {
                            it.first.match()
                        }
                    }) {
                        Text("Emojis Match")
                    }
                }
            }
        }

        class TheirRequest(override val verificationState: ActiveVerificationState.TheirRequest) : VerifyState<ActiveVerificationState.TheirRequest> {
            @Composable
            override fun VerifyUI(state: VerificationState, scope: CoroutineScope) {
                Button(onClick = {
                    scope.launch {
                        verificationState.ready()
                    }
                }) {
                    Text("Verify device ${verificationState.content.fromDevice}")
                }
            }
        }

        class PartlyDone(override val verificationState: ActiveVerificationState.PartlyDone) : VerifyState<ActiveVerificationState.PartlyDone> {
            @Composable
            override fun VerifyUI(state: VerificationState, scope: CoroutineScope) {
                Text("Waiting for other device...", fontSize = 18.sp)
            }
        }

        object Done : VerifyState<ActiveVerificationState.Done> {
            override val verificationState = ActiveVerificationState.Done
            @Composable
            override fun VerifyUI(state: VerificationState, scope: CoroutineScope) {

            }
        }

        val verificationState: T

        @Composable
        fun VerifyUI(state: VerificationState, scope: CoroutineScope)
    }

    @Composable
    fun VerifySelf(state: VerificationState) {
        val scope = rememberCoroutineScope()

        var verifyState by remember { mutableStateOf<VerifyState<*>>(VerifyState.Done) }
        var selfVerification by remember { mutableStateOf<SelfVerificationMethod.CrossSignedDeviceVerification?>(null) }

        Column {
            LaunchedEffect(state) {
                scope.launch {
                    state.listenForDevices {
                        Log.d("Verification status", it.toString())
                        verifyState = when(it) {
                            is ActiveVerificationState.Ready -> VerifyState.Ready(it)
                            is ActiveVerificationState.Start -> VerifyState.Start(it)
                            is ActiveVerificationState.TheirRequest -> VerifyState.TheirRequest(it)
                            is ActiveVerificationState.PartlyDone -> VerifyState.PartlyDone(it)
                            is ActiveVerificationState.Done -> VerifyState.Done
                            is ActiveVerificationState.Cancel -> VerifyState.Done
                            else -> verifyState
                        }
                    }
                }
                scope.launch {
                    state.maybeDeviceVerification {
                        selfVerification = it
                    }
                }
            }

            selfVerification?.let {
                if(verifyState is VerifyState.Done) {
                    Button(onClick = {
                        scope.launch {
                            it.createDeviceVerification()
                        }
                    }) {
                        Text("Verify This Device")
                    }
                }
            }

            verifyState.VerifyUI(state, scope)
        }
    }

    @Composable
    fun BootstrapCrossSigning(state: VerificationState, scope: CoroutineScope) {
        var currentState by remember { mutableStateOf<UIA.Step<Unit>?>(null) }
        Text("WARNING: Bootstrapping cross-signing is a potentially destructive action.")
        Button(onClick = {
            scope.launch {
                val resumed = currentState
                if(resumed == null) {
                    val signing = state.client.key.bootstrapCrossSigning().result.getOrThrow()

                    Log.d("Cross-Signing", signing.toString())
                    if (signing is UIA.Success<*>) {
                        return@launch
                    } else if (signing is UIA.Step<*>) {
                        openUrlInChromeCustomTab(
                            this@VerificationActivity,
                            null,
                            signing.getFallbackUrl(AuthenticationType.SSO).toString()
                        )
                        Log.d("Cross-Signing", "Launched WebView")
                        currentState = signing as UIA.Step<Unit>
                    }
                } else {
                    currentState = null
                    Log.d("Cross-Signing", "Authenticating with Fallback")
                    val result = resumed.authenticate(AuthenticationRequest.Fallback).getOrThrow()
                    Log.d("Cross-Signing", result.toString())
                }
            }
        }) {
            Text(currentState?.let { "Continue Cross-Signing"} ?: "Bootstrap Cross-Signing")
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