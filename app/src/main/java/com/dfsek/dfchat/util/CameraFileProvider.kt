package com.dfsek.dfchat.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.dfsek.dfchat.R
import java.io.File

class CameraFileProvider : FileProvider(R.xml.filepaths) {
    companion object{
        fun getImageUri(context: Context): Uri {
            val directory = File(context.cacheDir, "images")
            directory.mkdirs()
            val file = File.createTempFile(
                "image_upload",
                ".png",
                directory
            )
            val authority = context.packageName + ".uploadprovider"
            return getUriForFile(
                context,
                authority,
                file,
            )
        }
    }
}