package com.dfsek.dfchat.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsSession
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LiveData
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.request.ImageRequest
import com.dfsek.dfchat.SessionHolder
import com.dfsek.dfchat.ui.settings.SettingsActivity
import com.google.android.material.color.MaterialColors
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.RichText
import com.halilibo.richtext.ui.RichTextThemeIntegration
import org.matrix.android.sdk.api.session.content.ContentUrlResolver
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
import org.matrix.android.sdk.api.session.room.model.message.MessageImageContent
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent

fun openUrlInChromeCustomTab(
    context: Context,
    session: CustomTabsSession?,
    url: String
) {
    try {
        CustomTabsIntent.Builder()
            .setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK)
            .apply { session?.let { setSession(it) } }
            .build()
            .launchUrl(context, Uri.parse(url))
    } catch (activityNotFoundException: ActivityNotFoundException) {
        Toast.makeText(context, "No browser available!", Toast.LENGTH_LONG).show()
    }
}


@Composable
fun Activity.SettingsDropdown(modifier: Modifier = Modifier, refresh: () -> Unit = {}) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        var expanded by remember {
            mutableStateOf(false)
        }
        IconButton(onClick = {
            expanded = true
        }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Open Options"
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            DropdownMenuItem(
                onClick = {
                    startActivity(Intent(applicationContext, SettingsActivity::class.java))
                    expanded = false
                },
                enabled = true
            ) {
                Text("Settings")
            }
            DropdownMenuItem(
                onClick = {
                    refresh()
                    expanded = false
                },
                enabled = true
            ) {
                Text("Refresh")
            }
        }
    }
}

fun <E> List<E>.update(value: E, index: Int): List<E> {
    return mapIndexed { i, e -> if (i == index) value else e }
}

fun getAvatarUrl(avatarUrl: String?, thumbnailX: Int = 32, thumbnailY: Int = thumbnailX): String? {
    return SessionHolder.currentSession?.contentUrlResolver()
        ?.resolveThumbnail(avatarUrl, thumbnailX, thumbnailY, ContentUrlResolver.ThumbnailMethod.SCALE)
}

internal const val SSO_REDIRECT_URL = "dfchat://login"

fun TimelineEvent.getPreviewText(): String {
    val fullText = when (root.getClearType()) {
        EventType.MESSAGE -> formatMessage(this)
        EventType.ENCRYPTED -> "Encrypted message (haven't received keys!)"

        else -> "dfChat unimplemented event ${root.getClearType()}"
    }

    val beforeNewline = fullText.substringBefore("\n")

    return if(beforeNewline.length <= 50) beforeNewline else (beforeNewline.substring(0, 51).trim() + "...")
}

@Composable
fun TimelineEvent.RenderMessage(modifier: Modifier = Modifier): Unit = when (root.getClearType()) {
    EventType.MESSAGE -> RenderMessageEvent(modifier = modifier)
    EventType.ENCRYPTED -> Text("Encrypted message (haven't received keys!)", color = Color.Red)

    else -> Text("dfChat unimplemented event ${root.getClearType()}", color = Color.Red)
}

@Composable
private fun TimelineEvent.RenderMessageEvent(modifier: Modifier = Modifier) {
    val messageContent = root.getClearContent().toModel<MessageContent>() ?: return
    when(messageContent.msgType) {
        "m.text" -> RichTextThemeIntegration(contentColor = { MaterialTheme.colors.onBackground }) {
            RichText(modifier = modifier) {
                Markdown(content = formatMessage(this@RenderMessageEvent))
            }
        }
        "m.image" -> {
            val imageContent = root.getClearContent().toModel<MessageImageContent>() ?: return
            var imageUrl by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(imageContent) {
                imageUrl = SessionHolder.currentSession!!.contentUrlResolver().resolveFullSize(imageContent.url)
            }

            imageUrl?.let {
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

private fun formatMessage(timelineEvent: TimelineEvent): String {
    val messageContent = timelineEvent.root.getClearContent().toModel<MessageContent>() ?: return ""
    Log.d("Message type", messageContent.toString())
    return messageContent.body
}

@Composable
fun <T> DynamicContent(data: LiveData<T>, consume: @Composable (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var value: T? by remember { mutableStateOf(null) }
    data.observe(lifecycleOwner) { value = it }
    value?.let { consume(it) }
}
