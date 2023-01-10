package com.dfsek.dfchat.util.vector.multipicker

import android.util.Log
import im.vector.lib.multipicker.entity.*
import org.matrix.android.sdk.api.session.content.ContentAttachmentData
import org.matrix.android.sdk.api.util.MimeTypes.isMimeTypeAudio
import org.matrix.android.sdk.api.util.MimeTypes.isMimeTypeImage
import org.matrix.android.sdk.api.util.MimeTypes.isMimeTypeVideo

fun MultiPickerBaseType.toContentAttachmentData(): ContentAttachmentData {
    return when (this) {
        is MultiPickerImageType -> toContentAttachmentData()
        is MultiPickerVideoType -> toContentAttachmentData()
        is MultiPickerAudioType -> toContentAttachmentData(isVoiceMessage = false)
        is MultiPickerFileType -> toContentAttachmentData()
        else -> throw IllegalStateException("Unknown file type")
    }
}

fun MultiPickerAudioType.toContentAttachmentData(isVoiceMessage: Boolean): ContentAttachmentData {
    if (mimeType == null) Log.w("Attachment", "No mimeType")
    return ContentAttachmentData(
        mimeType = mimeType,
        type = if (isVoiceMessage) ContentAttachmentData.Type.VOICE_MESSAGE else mapType(),
        size = size,
        name = displayName,
        duration = duration,
        queryUri = contentUri,
        waveform = waveform
    )
}

fun MultiPickerBaseMediaType.toContentAttachmentData(): ContentAttachmentData {
    return when (this) {
        is MultiPickerImageType -> toContentAttachmentData()
        is MultiPickerVideoType -> toContentAttachmentData()
        else -> throw IllegalStateException("Unknown media type")
    }
}

fun MultiPickerImageType.toContentAttachmentData(): ContentAttachmentData {
    if (mimeType == null) Log.w("Attachment", "No mimeType")
    return ContentAttachmentData(
        mimeType = mimeType,
        type = mapType(),
        name = displayName,
        size = size,
        height = height.toLong(),
        width = width.toLong(),
        exifOrientation = orientation,
        queryUri = contentUri
    )
}

fun MultiPickerVideoType.toContentAttachmentData(): ContentAttachmentData {
    if (mimeType == null) Log.w("Attachment", "No mimeType")
    return ContentAttachmentData(
        mimeType = mimeType,
        type = ContentAttachmentData.Type.VIDEO,
        size = size,
        height = height.toLong(),
        width = width.toLong(),
        duration = duration,
        name = displayName,
        queryUri = contentUri
    )
}

fun MultiPickerFileType.toContentAttachmentData(): ContentAttachmentData {
    if (mimeType == null) Log.w("Attachment", "No mimeType")
    return ContentAttachmentData(
        mimeType = mimeType,
        type = mapType(),
        size = size,
        name = displayName,
        queryUri = contentUri
    )
}

private fun MultiPickerBaseType.mapType(): ContentAttachmentData.Type {
    return when {
        mimeType?.isMimeTypeImage() == true -> ContentAttachmentData.Type.IMAGE
        mimeType?.isMimeTypeVideo() == true -> ContentAttachmentData.Type.VIDEO
        mimeType?.isMimeTypeAudio() == true -> ContentAttachmentData.Type.AUDIO
        else -> ContentAttachmentData.Type.FILE
    }
}