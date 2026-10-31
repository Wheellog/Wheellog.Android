package com.cooper.wheellog.track

data class TrackStatus(val armed: Boolean = false, val recording: Boolean = false, val paused: Boolean = false,
    val id: String? = null, val points: Int = 0, val distance: Double = 0.0, val startedAt: Long = 0,
    val lastFixAt: Long = 0, val error: String? = null, val pauseReason: TrackPause = TrackPause.NONE)

fun formatTripTimeWindow(start: Long, end: Long?): String {
    val day = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
    val sameDay = end == null || day.format(java.util.Date(start)) == day.format(java.util.Date(end))
    val time = java.text.SimpleDateFormat(if (sameDay) "HH:mm" else "dd.MM HH:mm", java.util.Locale.getDefault())
    val prefix = if (sameDay) java.text.SimpleDateFormat("dd.MM.yyyy · ", java.util.Locale.getDefault()).format(java.util.Date(start)) else ""
    return prefix + time.format(java.util.Date(start)) + " — " + (end?.let { time.format(java.util.Date(it)) } ?: "…")
}
