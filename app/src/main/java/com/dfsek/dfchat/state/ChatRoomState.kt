package com.dfsek.dfchat.state

import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.LifecycleOwner
import com.dfsek.dfchat.util.getAvatarUrl
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.api.util.toMatrixItem

class ChatRoomState(
    val room: Room,
    val client: Session
) : Timeline.Listener {
    private var timeline: Timeline? by mutableStateOf(null)
    var timelineEvents: List<TimelineEvent> by mutableStateOf(emptyList())
        private set

    var latestEvent: TimelineEvent? by mutableStateOf(null)
        private set


    override fun onTimelineUpdated(snapshot: List<TimelineEvent>) {
        timelineEvents = snapshot.reversed()
    }


    fun startSync() {
        timeline = room.timelineService()
            .createTimeline(null, TimelineSettings(initialSize = 25)).also {
                it.addListener(this)
                it.start()
            }
    }

    fun stopSync() {
        timeline?.let {
            it.removeAllListeners()
            it.dispose()
        }
        timeline = null
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

    val name = room.roomSummary()?.displayName

    fun getName(lifecycleOwner: LifecycleOwner, consume: (String) -> Unit) {
        room.getRoomSummaryLive().observe(lifecycleOwner) { roomSummary ->
            val summary = roomSummary.getOrNull() ?: return@observe
            consume(summary.displayName)
        }
    }

    fun sendTextMessage(message: String) {
        Log.d("Sending Message", message)
        room.sendService()
            .sendTextMessage(message)
    }

    fun getUserAvatar(id: String): String? {
        return client
            .userService()
            .getUser(id)
            ?.avatarUrl?.let { getAvatarUrl(it) }
    }
}