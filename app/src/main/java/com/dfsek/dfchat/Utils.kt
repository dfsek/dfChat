package com.dfsek.dfchat

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
import arrow.core.flatMap
import com.dfsek.dfchat.ui.settings.SettingsActivity
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.MatrixClientConfiguration
import net.folivo.trixnity.client.key.DeviceTrustLevel
import net.folivo.trixnity.client.loginWith
import net.folivo.trixnity.client.media.MediaStore
import net.folivo.trixnity.client.store.Room
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.clientserverapi.model.authentication.LoginType
import net.folivo.trixnity.core.model.events.EventContent
import net.folivo.trixnity.core.model.events.m.room.EncryptedEventContent
import net.folivo.trixnity.core.model.events.m.room.MemberEventContent
import net.folivo.trixnity.core.model.events.m.room.Membership
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import org.koin.core.module.Module

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

suspend fun MatrixClient.Companion.login(
    baseUrl: Url,
    identifier: IdentifierType? = null,
    token: String,
    deviceId: String? = null,
    initialDeviceDisplayName: String? = null,
    repositoriesModule: Module,
    mediaStore: MediaStore,
    scope: CoroutineScope,
    configuration: MatrixClientConfiguration.() -> Unit = {}
): Result<MatrixClient> =
    MatrixClient.Companion.loginWith(
        baseUrl = baseUrl,
        repositoriesModule = repositoriesModule,
        mediaStore = mediaStore,
        scope = scope,
        getLoginInfo = { api ->
            api.authentication.login(
                identifier = identifier,
                token = token,
                type = LoginType.Token,
                deviceId = deviceId,
                initialDeviceDisplayName = initialDeviceDisplayName
            ).flatMap { login ->
                api.users.getProfile(login.userId).map { profile ->
                    MatrixClient.LoginInfo(
                        userId = login.userId,
                        accessToken = login.accessToken,
                        deviceId = login.deviceId,
                        displayName = profile.displayName,
                        avatarUrl = profile.avatarUrl
                    )
                }
            }
        },
        configuration = configuration
    )

fun parseEvent(content: EventContent): String {
    return when (content) {
        is MemberEventContent -> content.displayName + " " + when (content.membership) {
            Membership.LEAVE -> "left."
            Membership.BAN -> "was banned."
            Membership.INVITE -> "was invited."
            Membership.JOIN -> "joined."
            Membership.KNOCK -> "knocked."
        }

        is RoomMessageEventContent -> content.body
        is EncryptedEventContent -> "Encrypted message"
        else -> content.toString()
    }
}

fun Room.getHumanName(): String =
    if (name?.explicitName != null) {
        name!!.explicitName as String
    } else if (name?.heroes?.isNotEmpty() == true) {
        name!!.heroes[0].full
    } else {
        name.toString()
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

fun DeviceTrustLevel.userString(): String = when(this) {
    is DeviceTrustLevel.Verified -> "Verified"
    is DeviceTrustLevel.Blocked -> "Blocked"
    is DeviceTrustLevel.NotVerified -> "Unverified"
    is DeviceTrustLevel.NotCrossSigned -> "Not Cross-Signed"
    is DeviceTrustLevel.Invalid -> "Invalid: " + this.reason
}

internal const val SSO_REDIRECT_PATH = "/_matrix/client/r0/login/sso/redirect"
internal const val SSO_REDIRECT_URL_PARAM = "redirectUrl"
internal const val SSO_REDIRECT_URL = "dfchat://login"