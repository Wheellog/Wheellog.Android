package com.cooper.wheellog.track

enum class TrackPause { NONE, MANUAL, AUTOMATIC }

/** Only fresh motion evidence counts as a stop. Missing GPS/Bluetooth is not a stop. */
class TrackPauseState(initial: TrackPause = TrackPause.NONE) {
    var pause = initial
        private set
    private var stationarySince: Long? = null
    private var lastEvidence: Long? = null
    fun manualPause() { pause = TrackPause.MANUAL; stationarySince = null }
    fun resume() { pause = TrackPause.NONE; stationarySince = null; lastEvidence = null }
    fun motion(moving: Boolean, now: Long): Boolean {
        val before = pause
        if (pause == TrackPause.MANUAL) return false
        if (moving) {
            stationarySince = null
            if (pause == TrackPause.AUTOMATIC) pause = TrackPause.NONE
        } else {
            if (lastEvidence == null || now - lastEvidence!! > 15_000 || now < lastEvidence!!) stationarySince = now
            if (stationarySince == null) stationarySince = now
            if (now - stationarySince!! >= 5 * 60_000) pause = TrackPause.AUTOMATIC
        }
        lastEvidence = now
        return pause != before
    }
}
