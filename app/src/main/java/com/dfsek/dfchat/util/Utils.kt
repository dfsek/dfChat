package com.dfsek.dfchat.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsSession
import androidx.compose.foundation.layout.Box
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LiveData
import com.dfsek.dfchat.SessionHolder
import com.dfsek.dfchat.ui.settings.SettingsActivity
import org.matrix.android.sdk.api.session.content.ContentUrlResolver
import org.matrix.android.sdk.api.session.events.model.EventType
import org.matrix.android.sdk.api.session.events.model.toModel
import org.matrix.android.sdk.api.session.room.model.message.MessageContent
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
fun SettingsDropdown(applicationContext: Context, current: Context, refresh: () -> Unit = {}) {
    Box(
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
                    current.startActivity(Intent(applicationContext, SettingsActivity::class.java))
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

fun TimelineEvent.getText() = when (root.getClearType()) {
    EventType.MESSAGE -> formatMessage(this)
    EventType.ENCRYPTED -> "Encrypted message (haven't received keys!)"

    else -> "dfChat unimplemented event ${root.getClearType()}"
}

private fun formatMessage(timelineEvent: TimelineEvent): String {
    val messageContent = timelineEvent.root.getClearContent().toModel<MessageContent>() ?: return ""
    return messageContent.body
}

@Composable
fun <T> DynamicContent(data: LiveData<T>, consume: @Composable (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var value: T? by remember { mutableStateOf(null) }
    data.observe(lifecycleOwner) { value = it }
    value?.let { consume(it) }
}
