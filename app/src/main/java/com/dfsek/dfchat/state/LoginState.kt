package com.dfsek.dfchat.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dfsek.dfchat.login
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.fromStore
import net.folivo.trixnity.client.media.InMemoryMediaStore
import net.folivo.trixnity.client.store.repository.realm.createRealmRepositoriesModule
import net.folivo.trixnity.clientserverapi.client.SyncState

class LoginState {
    companion object {
        init {
            tryLogIn()
        }

        @JvmStatic
        var matrixClient: MatrixClient? by mutableStateOf(null)
            private set

        private fun tryLogIn() {
            CoroutineScope(Dispatchers.Default).launch {
                val matrixClient = MatrixClient.fromStore(
                    repositoriesModule = createRealmRepositoriesModule(),
                    mediaStore = InMemoryMediaStore(),
                    scope = CoroutineScope(Dispatchers.Default),
                ).getOrThrow()
                matrixClient?.startSync()
                matrixClient?.syncState?.first { it == SyncState.RUNNING }
                this@Companion.matrixClient = matrixClient
            }
        }

        suspend fun logout() {
            val client = matrixClient
            if (client != null) {
                client.logout()
                matrixClient = null
            }
        }

        suspend fun logInToken(baseUrl: Url, token: String) {
            val matrixClient = MatrixClient.login(
                baseUrl = baseUrl,
                identifier = null,
                token = token,
                mediaStore = InMemoryMediaStore(),
                deviceId = "dfchat",
                repositoriesModule = createRealmRepositoriesModule(),
                scope = CoroutineScope(Dispatchers.Default)
            ).getOrThrow()
            matrixClient.startSync()
            matrixClient.syncState.first { it == SyncState.RUNNING }
            this.matrixClient = matrixClient
        }
    }
}