package com.dfsek.dfchat

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsSession
import arrow.core.flatMap
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.MatrixClientConfiguration
import net.folivo.trixnity.client.loginWith
import net.folivo.trixnity.client.media.MediaStore
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.clientserverapi.model.authentication.LoginType
import net.folivo.trixnity.core.model.events.DecryptedOlmEvent
import net.folivo.trixnity.core.model.events.EventContent
import net.folivo.trixnity.core.model.events.RoomEventContent
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
        Toast.makeText(context, "No browser available!", Toast.LENGTH_LONG)
    }
}

suspend fun MatrixClient.Companion.login(
    baseUrl: Url,
    identifier: IdentifierType? = null,
    passwordOrToken: String,
    loginType: LoginType = LoginType.Password,
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
                passwordOrToken = passwordOrToken,
                type = loginType,
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
    return when(content) {
        is MemberEventContent -> content.displayName + " " + when(content.membership) {
            Membership.LEAVE  -> "left."
            Membership.BAN    -> "was banned."
            Membership.INVITE -> "was invited."
            Membership.JOIN   -> "joined."
            Membership.KNOCK  -> "knocked."
        }
        is RoomMessageEventContent -> content.body
        is EncryptedEventContent -> "Encrypted message"
        else -> content.toString()
    }
}

internal const val SSO_REDIRECT_PATH = "/_matrix/client/r0/login/sso/redirect"
internal const val SSO_REDIRECT_URL_PARAM = "redirectUrl"
internal const val SSO_REDIRECT_URL = "dfchat://login"