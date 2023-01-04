package com.dfsek.dfchat.state

import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.RoomId

class RoomsState(
    val client: MatrixClient,
    scope: CoroutineScope
) {
    val rooms: MutableMap<RoomId, Pair<TimelineEvent?, Room>> = mutableStateMapOf()

    init {
        scope.launch {
            client.room.getAll()
                .collectLatest {
                    it.values.forEach {
                        val room = it.first()
                        Log.d("Fetched room", room.toString())
                        scope.launch {
                            room?.lastRelevantEventId?.let { it1 ->
                                client.room.getTimelineEvent(it1, room.roomId)
                                    .collectLatest {
                                        rooms[room.roomId] = (Pair(it, room))
                                    }
                            }
                        }
                    }
                }
        }
    }

    fun getRoom(id: RoomId): ChatRoomState = ChatRoomState(id, client)
}