package com.dfsek.dfchat.state

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.dfsek.dfchat.getHumanName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.getEventId
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.EventId
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.UserId
import net.folivo.trixnity.core.model.events.Event
import okhttp3.internal.toImmutableList

class ChatRoomState(
    val roomId: RoomId,
    val client: MatrixClient,
) {
    private val events: MutableList<TimelineEvent> = mutableStateListOf()
    private val mappedEvents: MutableMap<EventId, TimelineEvent> = mutableStateMapOf()
    private var oldestTimelineEvent: TimelineEvent? = null
    private var newestEvent: EventId? = null
    private val listener: suspend (Event<*>) -> Unit = {
        if(it is Event.RoomEvent<*> && it.roomId == roomId) {
            CoroutineScope(Dispatchers.Default).launch {
                client.room.getTimelineEvent(it.id, roomId)
                    .collectLatest {
                        if (it != null) {
                            Log.d("Received new event", it.toString())
                            mappedEvents[it.eventId] = it
                            rescanEvents()
                        }
                    }
            }
        }
    }

    private fun rescanEvents() {
        mappedEvents.values.stream().reduce { e, e2 ->
             if(e.event.originTimestamp > e2.event.originTimestamp) e else e2
        }.ifPresent {
            newestEvent = it.eventId
        }
    }

    private fun getEventList_(current: List<TimelineEvent>): List<TimelineEvent> {
        return current.first().previousEventId?.let { previous -> mappedEvents[previous]?.let { getEventList_(listOf(it)) }?.plus(current) } ?: current
    }

    fun getEventList(): List<TimelineEvent> {
        return newestEvent?.let { eventId -> mappedEvents[eventId]?.let { getEventList_(listOf(it)) } } ?: emptyList()
    }

    suspend fun fetchMessages() {
        if (events.isEmpty()) {
            client.room.getLastTimelineEvent(roomId = roomId)
                .collectLatest { flowFlow ->
                    flowFlow?.collectLatest { eventUnwrapped ->
                        Log.d("Event added", eventUnwrapped.toString())
                        events.add(eventUnwrapped)
                        mappedEvents[eventUnwrapped.eventId] = eventUnwrapped
                        rescanEvents()
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
                            mappedEvents[eventUnwrapped.eventId] = eventUnwrapped
                            rescanEvents()
                            if (count < 20) {
                                eventUnwrapped.previousEventId?.let { it1 -> getPrevious(it1, count + 1) }
                            }
                        }
                    }
            }
            getPrevious(events().last().eventId, 0)
        }
    }

    fun startSync() {
        client.api.sync.subscribeAllEvents(listener)
    }

    fun stopSync() {
        client.api.sync.unsubscribeAllEvents(listener)
    }

    fun events(): List<TimelineEvent> = events.toImmutableList()

    fun splitEvents(): List<Pair<UserId, List<TimelineEvent>>> {
        val list = mutableListOf<Pair<UserId, MutableList<TimelineEvent>>>()

        var lastUserId: UserId? = null

        getEventList().forEach {
            if (lastUserId != null && it.event.sender == lastUserId) {
                list.last().second.add(it)
            } else {
                list.add(Pair(it.event.sender, mutableListOf(it)))
            }
            lastUserId = it.event.sender
        }

        return list.reversed()
    }

    suspend fun getName(consumer: suspend (String) -> Unit) {
        client.room.getById(roomId)
            .collectLatest {
                if (it != null) {
                    consumer(it.getHumanName())
                }
            }
    }

    suspend fun getRoomAvatar(consume: suspend (ByteArray) -> Unit) {
        client.room.getById(roomId)
            .collectLatest { room ->
                if (room != null) {
                    room.avatarUrl?.let { url ->
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

    suspend fun getLastMessage(consumer: suspend (TimelineEvent) -> Unit) {
        client.room.getLastTimelineEvent(roomId)
            .collectLatest {
                it?.first()?.let { consumer(it) }
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