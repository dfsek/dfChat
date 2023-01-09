package com.dfsek.dfchat.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsSession
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LiveData
import com.dfsek.dfchat.AppState
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


fun <E> List<E>.update(value: E, index: Int): List<E> {
    return mapIndexed { i, e -> if (i == index) value else e }
}

fun getAvatarUrl(avatarUrl: String?, thumbnailX: Int = 32, thumbnailY: Int = thumbnailX): String? {
    return AppState.session?.contentUrlResolver()
        ?.resolveThumbnail(avatarUrl, thumbnailX, thumbnailY, ContentUrlResolver.ThumbnailMethod.SCALE)
}

internal const val SSO_REDIRECT_URL = "dfchat://login"
internal const val THEME_PREFS = "theme"
internal const val THEME_KEY = "theme"

@Composable
fun <T> DynamicContent(data: LiveData<T>, consume: @Composable (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var value: T? by remember { mutableStateOf(null) }
    data.observe(lifecycleOwner) { value = it }
    value?.let { consume(it) }
}


fun Color.toHexString(): String {
    return String.format(
        "#%02x%02x%02x%02x", (this.alpha * 255).toInt(),
        (this.red * 255).toInt(), (this.green * 255).toInt(), (this.blue * 255).toInt()
    )
}

fun String.toColor(): Color {
    return Color(android.graphics.Color.parseColor(this))
}