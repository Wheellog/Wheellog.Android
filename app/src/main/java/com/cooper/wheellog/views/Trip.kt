package com.cooper.wheellog.views

import android.net.Uri
import android.provider.MediaStore
import java.io.File

class Trip {
    var title = ""
    var description = ""
    var relalitivePath = ""

    constructor(title: String, description: String, relalitivePath: String) {
        this.title = title
        this.description = description
        this.relalitivePath = relalitivePath
    }

    var content: Uri
        get() {
            val downloads = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)
            return Uri.parse(downloads.toString() + File.separator + relalitivePath + title)
        }
        set(value) {}
}