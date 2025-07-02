package com.cooper.wheellog.views

import android.net.Uri

class TripModel(
    var title: String,
    var description: String,
    var uri: Uri,
    var pathLegacyAndroid: String? = null,
    var fileName: String = title)