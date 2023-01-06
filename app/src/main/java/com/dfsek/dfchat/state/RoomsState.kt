package com.dfsek.dfchat.state

import android.util.Log
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
    val rooms = mutableStateMapOf<String, ChatRoomState>()

    init {
        client.roomService().getRoomSummariesLive(RoomSummaryQueryParams.Builder().build()).observe(lifecycleOwner) {
            it.forEach {
                scope.launch {
                    rooms[it.roomId] = getRoom(it.roomId)
                }
            }
        }
    }

    fun getRooms() = rooms.values.sortedBy { it.latestEvent?.root?.originServerTs }.reversed()
    fun getRoom(id: String): ChatRoomState = rooms.computeIfAbsent(id) {
        ChatRoomState(it, client, lifecycleOwner)
    }
}