package com.dfsek.dfchat.util

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest
import com.dfsek.dfchat.AppState
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.RichText
import com.halilibo.richtext.ui.RichTextThemeIntegration
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageImageContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import java.io.File

@Composable
fun TimelineEvent.RenderMessage(modifier: Modifier = Modifier): Unit = when (root.getClearType()) {
    EventType.MESSAGE -> RenderMessageEvent(modifier = modifier)
    EventType.ENCRYPTED -> Text("Encrypted message (haven't received keys!)", color = MaterialTheme.colors.error)

    else -> Text("dfChat unimplemented event ${root.getClearType()}", color = MaterialTheme.colors.error)
}

@Composable
private fun TimelineEvent.RenderMessageEvent(modifier: Modifier = Modifier) {
    val messageContent = root.getClearContent().toModel<MessageContent>() ?: return
    when (messageContent.msgType) {
        "m.text" -> RichTextThemeIntegration(contentColor = { MaterialTheme.colors.onBackground }) {
            RichText(modifier = modifier) {
                Markdown(content = formatMessage(this@RenderMessageEvent))
            }
        }

        "m.image" -> {
            val imageContent = root.getClearContent().toModel<MessageImageContent>() ?: return
            Log.d("IMAGE CONTENT", imageContent.toString())

            var image by remember { mutableStateOf<File?>(null) }

            LaunchedEffect(imageContent) {
                image = AppState.session!!.fileService().downloadFile(imageContent)
            }

            image?.let {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(it)
                        .crossfade(true)
                        .decoderFactory(BitmapFactoryDecoder.Factory())
                        .build(),
                    contentScale = ContentScale.Fit,
                    contentDescription = null,
                    modifier = modifier.fillMaxWidth()
                )
            } ?: Text("Rendering image...")
        }

        else -> Text(formatMessage(this))
    }
}

fun TimelineEvent.getPreviewText(cut: Boolean = true): String {
    val fullText = when (root.getClearType()) {
        EventType.MESSAGE -> formatMessage(this)
        EventType.ENCRYPTED -> "Encrypted message (haven't received keys!)"

        else -> "dfChat unimplemented event ${root.getClearType()}"
    }
    if (!cut) return fullText

    val beforeNewline = fullText.substringBefore("\n")

    return if (beforeNewline.length <= 50) beforeNewline else (beforeNewline.substring(0, 51).trim() + "...")
}

private fun formatMessage(timelineEvent: TimelineEvent): String {
    val messageContent = timelineEvent.root.getClearContent().toModel<MessageContent>() ?: return ""
    Log.d("Message type", messageContent.toString())
    return messageContent.body
}