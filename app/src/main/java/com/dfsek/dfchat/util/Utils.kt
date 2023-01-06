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
import com.dfsek.dfchat.SessionHolder
import com.dfsek.dfchat.ui.settings.SettingsActivity
import org.matrix.android.sdk.api.session.content.ContentUrlResolver

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
    return SessionHolder.currentSession?.contentUrlResolver()?.resolveThumbnail(avatarUrl, thumbnailX, thumbnailY, ContentUrlResolver.ThumbnailMethod.SCALE)
}

internal const val SSO_REDIRECT_PATH = "/_matrix/client/r0/login/sso/redirect"
internal const val SSO_REDIRECT_URL_PARAM = "redirectUrl"
internal const val SSO_REDIRECT_URL = "dfchat://login"