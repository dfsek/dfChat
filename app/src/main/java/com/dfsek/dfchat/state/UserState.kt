package com.dfsek.dfchat.state

import com.dfsek.dfchat.AppState
import com.dfsek.dfchat.util.getAvatarUrl
import org.matrix.android.sdk.api.session.Session

class UserState(val client: Session) {
    fun getAvatar(): String? {
        return client.userService()
            .getUser(client.myUserId)
            ?.avatarUrl?.let { getAvatarUrl(it) }
    }

    fun getUsername() = client.myUserId

    suspend fun logout() {
        client.signOutService().signOut(true)
        AppState.clearSession()
    }
}