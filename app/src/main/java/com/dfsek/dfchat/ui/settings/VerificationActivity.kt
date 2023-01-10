package com.dfsek.dfchat.ui.settings

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dfsek.dfchat.AppState
import com.dfsek.dfchat.state.VerificationState
import com.dfsek.dfchat.util.DynamicContent
import org.matrix.android.sdk.api.session.crypto.model.DeviceInfo
import org.matrix.android.sdk.api.session.crypto.verification.*

class VerificationActivity : AppCompatActivity() {
    var listener: VerificationService.Listener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colors = AppState.themeColors) {
                AppState.session?.let { session ->
                    val state = VerificationState(session)
                    Surface(modifier = Modifier.fillMaxSize()) {
                        DynamicContent(state.devices) {
                            Column {
                                SettingsTopBar("Verification")
                                Verification(state)
                                it.forEach {
                                    Device(it)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        AppState.session?.let { session ->
            listener?.let { session.cryptoService().verificationService().removeListener(it) }
        }
    }

    @Composable
    fun Device(info: DeviceInfo) {
        Column(modifier = Modifier.padding(6.dp)) {
            info.deviceId?.let { Text(it) }
            info.displayName?.let { Text(it) }
            info.lastSeenIp?.let { Text("Last seen at $it") }
            Divider()
        }
    }

    @Composable
    fun Verification(state: VerificationState) {
        var pendingStart: PendingVerificationRequest? by remember { mutableStateOf(null) }
        var incomingPendingAccept: Pair<IncomingSasVerificationTransaction.UxState, IncomingSasVerificationTransaction>? by remember {
            mutableStateOf(
                null
            )
        }

        listener = remember {
            object : VerificationService.Listener {
                override fun verificationRequestCreated(pr: PendingVerificationRequest) {
                    pendingStart = pr
                }

                override fun transactionUpdated(tx: VerificationTransaction) {
                    if (tx is IncomingSasVerificationTransaction) {
                        incomingPendingAccept = Pair(tx.uxState, tx)

                        Log.d("Verification transaction", tx.uxState.toString())
                    }
                    Log.d("Verification transaction", tx.state.toString())
                }

                override fun transactionCreated(tx: VerificationTransaction) {
                    if (tx is IncomingSasVerificationTransaction) {
                        incomingPendingAccept = Pair(tx.uxState, tx)

                        Log.d("Verification transaction", tx.uxState.toString())
                    }
                    Log.d("Verification transaction", tx.state.toString())
                }
            }.also {
                state.registerListener(it)
            }
        }

        pendingStart?.let { request ->
            Button(onClick = {
                pendingStart = null
                request.transactionId?.let { transactionId ->
                    AppState.session!!
                        .cryptoService()
                        .verificationService()
                        .readyPendingVerification(
                            methods = listOf(VerificationMethod.SAS),
                            otherUserId = request.otherUserId,
                            transactionId = transactionId
                        )
                }
            }) {
                Text("Verify with ${request.requestInfo?.fromDevice}")
            }
        }

        incomingPendingAccept?.let { transaction ->
            val (buttonText, message) =
                when (transaction.first) {
                    IncomingSasVerificationTransaction.UxState.SHOW_ACCEPT -> Pair("Accept Verification", "")
                    IncomingSasVerificationTransaction.UxState.SHOW_SAS -> Pair(
                        "Keys Match",
                        transaction.second.getEmojiCodeRepresentation().joinToString { it.emoji })

                    IncomingSasVerificationTransaction.UxState.CANCELLED_BY_ME -> Pair(
                        null,
                        "You cancelled verification."
                    )

                    IncomingSasVerificationTransaction.UxState.CANCELLED_BY_OTHER -> Pair(
                        null,
                        "Other device cancelled verification."
                    )

                    IncomingSasVerificationTransaction.UxState.WAIT_FOR_VERIFICATION -> Pair(null, "Waiting...")
                    IncomingSasVerificationTransaction.UxState.WAIT_FOR_KEY_AGREEMENT -> Pair(null, "Waiting...")
                    else -> Pair(null, "")
                }

            buttonText?.let {
                Button(onClick = {
                    incomingPendingAccept = null
                    if (transaction.first == IncomingSasVerificationTransaction.UxState.SHOW_SAS) {
                        transaction.second.userHasVerifiedShortCode()
                    } else {
                        transaction.second.performAccept()
                    }
                }) {
                    Text(it)
                }
            }

            Text(message)

        }
    }
}