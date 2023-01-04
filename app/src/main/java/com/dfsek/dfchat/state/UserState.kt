package com.dfsek.dfchat.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.user

class UserState(val client: MatrixClient) {
    suspend fun getAvatar(consume: suspend (ByteArray) -> Unit) {
        client.avatarUrl.let { urlFlow ->
            urlFlow.collectLatest { url ->
                if (url != null) {
                    client.api.media.download(url)
                        .onSuccess {
                            val bytes = ByteArray(it.contentLength!!.toInt())
                            it.content.readFully(bytes, 0, it.contentLength!!.toInt())
                            consume(bytes)
                        }
                }
            }
        }
    }

    fun getUsername() = client.userId.full

    fun logout() {
        CoroutineScope(Dispatchers.Default).launch {
            LoginState.logout()
        }
    }
}