package com.dfsek.dfchat.state

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import com.dfsek.dfchat.getHumanName
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import okhttp3.internal.toImmutableList

class ChatRoomState(
    val roomId: RoomId,
    val client: MatrixClient,
) {
    private val events: MutableList<TimelineEvent> = mutableStateListOf()

    suspend fun fetchMessages() {
        if (events.isEmpty()) {
            client.room.getLastTimelineEvent(roomId = roomId)
                .collectLatest { flowFlow ->
                    flowFlow?.collectLatest { eventUnwrapped ->
                        Log.d("Event added", eventUnwrapped.toString())
                        events.add(eventUnwrapped)
                        if(eventUnwrapped.gap != null) {
                            client.room.fillTimelineGaps(eventUnwrapped.eventId, roomId)
                        }
                        fetchMessages()
                    }
                }
        } else {
            suspend fun getPrevious(eventId: EventId, count: Int) {
                client.room.getTimelineEvent(eventId, roomId)
                    .collectLatest {
                        it?.let { eventUnwrapped ->
                            Log.d("Late Event added", eventUnwrapped.toString())
                            events.add(eventUnwrapped)
                            if (count < 20) {
                                eventUnwrapped.previousEventId?.let { it1 -> getPrevious(it1, count + 1) }
                            }
                        }
                    }
            }
            getPrevious(events().last().eventId, 0)
        }
    }

    fun events(): List<TimelineEvent> = events.toImmutableList()

    fun splitEvents(): List<Pair<UserId, List<TimelineEvent>>> {
        val list = mutableListOf<Pair<UserId, MutableList<TimelineEvent>>>()

        var lastUserId: UserId? = null

        events().forEach {
            if (lastUserId != null && it.event.sender == lastUserId) {
                list.last().second.add(it)
            } else {
                list.add(Pair(it.event.sender, mutableListOf(it)))
            }
            lastUserId = it.event.sender
        }

        return list
    }

    suspend fun getName(consumer: suspend (String) -> Unit) {
        client.room.getById(roomId)
            .collectLatest {
                if (it != null) {
                    consumer(it.getHumanName())
                }
            }
    }

    suspend fun sendTextMessage(message: String) {
        Log.d("Sending Message", message)
        client.room.sendMessage(roomId) {
            text(message)
        }

    }

    suspend fun getAvatar(id: UserId, consume: suspend (ByteArray) -> Unit) {
        client.api.users.getAvatarUrl(id)
            .onSuccess { url ->
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