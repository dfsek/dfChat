package com.dfsek.dfchat.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.RoomSummaryQueryParams
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

class RoomsState(
    private val client: Session,
    scope: CoroutineScope,
    private val lifecycleOwner: LifecycleOwner
) {
    var rooms: List<ChatRoomState> by mutableStateOf(emptyList())
        private set

    init {
        scope.launch {
            client.roomService().getRoomSummariesLive(RoomSummaryQueryParams.Builder().build()).observe(lifecycleOwner) {
                rooms = it.map { getRoom(it.roomId) }
            }
        }
    }

    fun getRoom(id: String): ChatRoomState = ChatRoomState(id, client, lifecycleOwner)
}