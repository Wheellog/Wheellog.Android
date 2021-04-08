package com.cooper.wheellog.views

import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File

class Trip(var title: String, var description: String, var mediaId: String) {

    var uri: Uri
        get() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return Uri.fromFile(File(mediaId))
            }
            val downloads = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
            return Uri.withAppendedPath(downloads, mediaId)
        }
        set(_) {}
}