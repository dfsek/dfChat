package com.dfsek.dfchat

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.widget.ThemeUtils
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsSession
import android.widget.Toast
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flatMap
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.MatrixClientConfiguration
import net.folivo.trixnity.client.loginWith
import net.folivo.trixnity.client.media.MediaStore
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType
import net.folivo.trixnity.clientserverapi.model.authentication.LoginType
import org.koin.core.module.Module
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.internal.*
import kotlinx.coroutines.internal.*
import kotlin.jvm.*
import arrow.core.flatMap

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

internal const val SSO_REDIRECT_PATH = "/_matrix/client/r0/login/sso/redirect"
internal const val SSO_REDIRECT_URL_PARAM = "redirectUrl"
internal const val SSO_REDIRECT_URL = "dfchat://login"