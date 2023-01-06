package com.dfsek.dfchat.state

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams

class RoomsState(
    private val client: Session,
    scope: CoroutineScope,
    lifecycleOwner: LifecycleOwner
) {
    val rooms = mutableStateMapOf<String, ChatRoomState>()

    init {
        client.roomService().getRoomSummariesLive(RoomSummaryQueryParams.Builder().build()).observe(lifecycleOwner) {
            it.forEach {
                scope.launch {
                    client.getRoom(it.roomId)?.let { it1 ->
                        rooms[it.roomId] = ChatRoomState(it1, client)
                    }
                }
            }
        }
    }
}