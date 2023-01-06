package com.dfsek.dfchat.state

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.LifecycleOwner
import com.dfsek.dfchat.getAvatarUrl
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.api.util.toMatrixItem

class ChatRoomState(
    val roomId: String,
    val client: Session,
    val lifecycleOwner: LifecycleOwner
) : Timeline.Listener {
    private var timeline: Timeline? = null
    var timelineEvents: List<TimelineEvent> by mutableStateOf(emptyList())
        private set


    override fun onTimelineUpdated(snapshot: List<TimelineEvent>) {
        timelineEvents = snapshot
    }


    fun startSync() {
        timeline = client.roomService().getRoom(roomId)?.timelineService()
            ?.createTimeline(null, TimelineSettings(initialSize = 25))
    }

    fun stopSync() {
        timeline?.let {
            it.removeAllListeners()
            it.dispose()
        }
    }

    fun splitEvents(): List<Pair<SenderInfo, List<TimelineEvent>>> {
        val list = mutableListOf<Pair<SenderInfo, MutableList<TimelineEvent>>>()

        var lastUserId: SenderInfo? = null

        timelineEvents.forEach {
            if (lastUserId != null && it.senderInfo == lastUserId) {
                list.last().second.add(it)
            } else {
                list.add(Pair(it.senderInfo, mutableListOf(it)))
            }
            lastUserId = it.senderInfo
        }

        return list.reversed()
    }

    suspend fun getName(consumer: suspend (String) -> Unit) {
        consumer(roomId)
    }

    fun getRoomAvatar(consume: (String) -> Unit) {
        client.roomService().getRoom(roomId)?.getRoomSummaryLive()?.observe(lifecycleOwner) { roomSummary ->
            val roomSummaryAsMatrixItem =
                roomSummary.map { it.toMatrixItem() }.getOrNull() ?: return@observe
            getAvatarUrl(roomSummaryAsMatrixItem.avatarUrl)?.let { consume(it) }
        }
    }

    fun getLastMessage(consumer: (TimelineEvent) -> Unit) {
        client.roomService().getRoom(roomId)?.getRoomSummaryLive()?.observe(lifecycleOwner) { maybe ->
            val roomSummary = maybe.getOrNull() ?: return@observe
            roomSummary.latestPreviewableEvent?.let {
                consumer(it)
            }
        }
    }

    fun sendTextMessage(message: String) {
        Log.d("Sending Message", message)

    }

    fun getUserAvatar(id: String): String? {
        return client.userService()
            .getUser(id)
            ?.avatarUrl?.let { getAvatarUrl(it) }
    }
}