package com.dfsek.dfchat.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsSession
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LiveData
import com.dfsek.dfchat.AppState
import im.vector.lib.multipicker.entity.MultiPickerBaseType
import im.vector.lib.multipicker.entity.MultiPickerFileType
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.session.content.ContentUrlResolver
import org.matrix.android.sdk.api.util.MimeTypes.isMimeTypeAudio
import org.matrix.android.sdk.api.util.MimeTypes.isMimeTypeImage
import org.matrix.android.sdk.api.util.MimeTypes.isMimeTypeVideo

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

internal const val GENERAL_PREFS = "general"

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
    if(this.isEmpty()) throw IllegalArgumentException("Empty color")
    return Color(android.graphics.Color.parseColor(this))
}

