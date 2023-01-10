package com.dfsek.dfchat.state

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.LifecycleOwner
import com.dfsek.dfchat.util.TimelineEventWrapper
import com.dfsek.dfchat.util.getAvatarUrl
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.sender.SenderInfo
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings

class ChatRoomState(
    val room: Room,
    val session: Session
) : Timeline.Listener {
    private var timeline: Timeline? by mutableStateOf(null)
    var replyTo: TimelineEvent? by mutableStateOf(null)
    var selectedImageEvent: TimelineEvent? by mutableStateOf(null)
    private var timelineEvents: List<TimelineEvent> by mutableStateOf(emptyList())
        private set


    override fun onTimelineUpdated(snapshot: List<TimelineEvent>) {
        timelineEvents = snapshot.reversed()
    }


    fun startSync() {
        timeline = room.timelineService()
            .createTimeline(null, TimelineSettings(initialSize = 35)).also {
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

    fun splitEvents(): List<Pair<SenderInfo, List<TimelineEventWrapper>>> {
        val list = mutableListOf<Pair<SenderInfo, MutableList<TimelineEventWrapper>>>()

        val redactionEventIDs = mutableSetOf<String>()
        val redactedEventIDs = mutableSetOf<String>()

        val redactedBy = mutableMapOf<String, TimelineEvent>()

        timelineEvents.forEach {
            if (it.root.getClearType() == "m.room.redaction") {
                redactedEventIDs.add(it.root.redacts!!)
                redactionEventIDs.add(it.eventId)
                redactedBy[it.root.redacts!!] = it
            }
        }

        fun createWrapper(event: TimelineEvent): TimelineEventWrapper =
            if (redactedEventIDs.contains(event.eventId)) TimelineEventWrapper.Redacted(event, redactedBy[event.eventId]!!)
            else TimelineEventWrapper.Default(event)

        var lastUserId: SenderInfo? = null
        timelineEvents.filter { !redactionEventIDs.contains(it.eventId) }.forEach {
            if (lastUserId != null && it.senderInfo == lastUserId) {
                list.last().second.add(createWrapper(it))
            } else {
                list.add(Pair(it.senderInfo, mutableListOf(createWrapper(it))))
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
        replyTo?.let {
            replyTo = null
            room.relationService()
                .replyToMessage(eventReplied = it, replyText = message, autoMarkdown = true)
        } ?: room.sendService()
            .sendTextMessage(text = message, autoMarkdown = true)
    }

    fun uploadImage(context: Context, uri: Uri) {
        val options = Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri), null, options)

        room.sendService().sendMedia(
            attachment = ContentAttachmentData(
                queryUri = uri,
                width = options.outWidth.toLong(),
                height = options.outHeight.toLong(),
                mimeType = options.outMimeType,
                type = ContentAttachmentData.Type.IMAGE),
            compressBeforeSending = true,
            roomIds = emptySet()
        )
    }

    fun redact(event: TimelineEvent) {
        room.sendService().redactEvent(event = event.root, reason = null)
    }

    fun getUserAvatar(id: String): String? {
        return session
            .userService()
            .getUser(id)
            ?.avatarUrl?.let { getAvatarUrl(it) }
    }

    fun loadMore() {
        timeline?.paginate(Timeline.Direction.BACKWARDS, 30)
    }
}