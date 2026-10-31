package com.cooper.wheellog.voice

/** No Android clock or TTS dependency: one current utterance, no stale speech backlog. */
class VoicePolicy {
    private data class Alarm(var seen: Long, var spoken: Long?)
    private val alarms = mutableMapOf<String, Alarm>()
    var priority = -1
        private set
    var lastAlarmAt: Long? = null
        private set
    private var reportAt: Long? = null
    private var currentMac = ""

    fun alarm(key: String, severity: Int, now: Long, repeatMs: Long): Boolean {
        val state = alarms.getOrPut(key) { Alarm(now, null) }
        if (now - state.seen > 2500) state.spoken = null
        state.seen = now
        lastAlarmAt = now
        if (priority >= severity || state.spoken?.let { now - it < repeatMs } == true) return false
        state.spoken = now
        priority = severity
        return true
    }

    fun reportDue(mac: String, now: Long, intervalMs: Long, fresh: Boolean, moving: Boolean): Boolean {
        if (mac != currentMac || reportAt == null) {
            currentMac = mac
            reportAt = now
            return false
        }
        if (!fresh || !moving || priority >= 0 || lastAlarmAt?.let { now - it < 10000 } == true) return false
        if (now - reportAt!! < intervalMs) return false
        reportAt = now
        return true
    }

    fun postponeReport(now: Long) { reportAt = now }
    fun canReport(now: Long) = priority < 0 && lastAlarmAt?.let { now - it < 10000 } != true
    fun occupy(severity: Int) { priority = severity }
    fun complete() { priority = -1 }
    fun reset() { alarms.clear(); priority = -1; reportAt = null; currentMac = ""; lastAlarmAt = null }
}
