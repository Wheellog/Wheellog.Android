package com.cooper.wheellog.views

import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi

class Trip {
    var mediaId = ""
    var title = ""
    var description = ""

    constructor(title: String, description: String, mediaId: String) {
        this.title = title
        this.description = description
        this.mediaId = mediaId
    }

    var uri: Uri
        @RequiresApi(Build.VERSION_CODES.Q)
        get() {
            val downloads = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
            return Uri.withAppendedPath(downloads, mediaId)
        }
        set(value) {}
}