package com.cooper.wheellog.voice

import android.content.Context
import android.content.SharedPreferences

/** Separate storage: build 149 preferences and trip files are never migrated. */
class VoiceSettings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("voice_announcements_v1", Context.MODE_PRIVATE)
    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(value) { prefs.edit().putBoolean("enabled", value).apply() }
    var alarms: Boolean
        get() = prefs.getBoolean("alarms", true)
        set(value) { prefs.edit().putBoolean("alarms", value).apply() }
    var reports: Boolean
        get() = prefs.getBoolean("reports", true)
        set(value) { prefs.edit().putBoolean("reports", value).apply() }
    var connection: Boolean
        get() = prefs.getBoolean("connection", true)
        set(value) { prefs.edit().putBoolean("connection", value).apply() }
    var movingOnly: Boolean
        get() = prefs.getBoolean("moving_only", true)
        set(value) { prefs.edit().putBoolean("moving_only", value).apply() }
    var intervalSeconds: Int
        get() = prefs.getInt("interval_seconds", 60).coerceIn(15, 1800)
        set(value) { prefs.edit().putInt("interval_seconds", value.coerceIn(15, 1800)).apply() }
    var timedReports: Boolean
        get() = prefs.getBoolean("timed_reports", true)
        set(value) { prefs.edit().putBoolean("timed_reports", value).apply() }

    private fun distanceKey(mac: String, name: String) = "distance_${mac.uppercase(java.util.Locale.ROOT)}_$name"
    fun distanceReports(mac: String) = DistanceReports(
        periodic = prefs.getBoolean(distanceKey(mac, "periodic"), false),
        intervalMetres = prefs.getLong(distanceKey(mac, "interval"), 5000).coerceAtLeast(1),
        after = prefs.getBoolean(distanceKey(mac, "after"), false),
        afterMetres = prefs.getLong(distanceKey(mac, "after_metres"), 5000).coerceAtLeast(1),
        originMetres = prefs.getLong(distanceKey(mac, "origin"), -1).takeIf { it >= 0 },
        target = prefs.getBoolean(distanceKey(mac, "target"), false),
        targetMetres = prefs.getLong(distanceKey(mac, "target_metres"), -1).takeIf { it >= 0 })

    /** Observe this wheel's settings, including one-shot completion by the service. */
    fun observeDistanceReports(mac: String, onChange: (DistanceReports) -> Unit): () -> Unit {
        val prefix = distanceKey(mac, "")
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null || key.startsWith(prefix)) onChange(distanceReports(mac))
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onChange(distanceReports(mac))
        // This closure also retains the listener until the screen disposes it.
        return { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    fun setPeriodicDistance(mac: String, enabled: Boolean) {
        prefs.edit().putBoolean(distanceKey(mac, "periodic"), enabled).apply()
    }
    fun setDistanceInterval(mac: String, metres: Long) {
        prefs.edit().putLong(distanceKey(mac, "interval"), metres.coerceAtLeast(1)).apply()
    }
    fun setAfterDistance(mac: String, enabled: Boolean, liveOdometer: Long?) {
        prefs.edit().putBoolean(distanceKey(mac, "after"), enabled)
            .putLong(distanceKey(mac, "origin"), if (enabled) liveOdometer ?: -1 else -1).apply()
    }
    fun setAfterMetres(mac: String, metres: Long, liveOdometer: Long?) {
        prefs.edit().putLong(distanceKey(mac, "after_metres"), metres.coerceAtLeast(1))
            .putLong(distanceKey(mac, "origin"), liveOdometer ?: -1).apply()
    }
    fun setTargetDistance(mac: String, enabled: Boolean) {
        prefs.edit().putBoolean(distanceKey(mac, "target"), enabled).apply()
    }
    fun setTargetMetres(mac: String, metres: Long) {
        prefs.edit().putLong(distanceKey(mac, "target_metres"), metres.coerceAtLeast(0))
            .putBoolean(distanceKey(mac, "target_edited"), true).apply()
    }
    fun initializeDistance(mac: String, liveOdometer: Long, initializeDefault: Boolean) {
        if (liveOdometer <= 0) return
        val current = distanceReports(mac)
        val needsOrigin = current.after && current.originMetres == null
        val needsDefault = initializeDefault && !prefs.getBoolean(distanceKey(mac, "target_edited"), false)
        if (!needsOrigin && !needsDefault) return
        val edit = prefs.edit()
        if (needsOrigin)
            edit.putLong(distanceKey(mac, "origin"), liveOdometer)
        if (needsDefault)
            edit.putLong(distanceKey(mac, "target_metres"), liveOdometer)
        edit.apply()
    }
    fun finishDistanceReports(mac: String, after: Boolean, target: Boolean) {
        if (!after && !target) return
        val edit = prefs.edit()
        if (after) edit.putBoolean(distanceKey(mac, "after"), false)
        if (target) edit.putBoolean(distanceKey(mac, "target"), false)
        edit.apply()
    }
    var alarmRepeatSeconds: Int
        get() = prefs.getInt("alarm_repeat_seconds", 10).coerceIn(5, 60)
        set(value) { prefs.edit().putInt("alarm_repeat_seconds", value.coerceIn(5, 60)).apply() }
    var ratePercent: Int
        get() = prefs.getInt("rate_percent", 100).coerceIn(50, 175)
        set(value) { prefs.edit().putInt("rate_percent", value.coerceIn(50, 175)).apply() }
    var volumePercent: Int
        get() = prefs.getInt("volume_percent", 100).coerceIn(10, 100)
        set(value) { prefs.edit().putInt("volume_percent", value.coerceIn(10, 100)).apply() }
    var duckMusic: Boolean
        get() = prefs.getBoolean("duck_music", true)
        set(value) { prefs.edit().putBoolean("duck_music", value).remove("music_mode").apply() }
    var musicMode: String
        get() = prefs.getString("music_mode", null)?.takeIf { it in setOf("mix", "duck", "pause") }
            ?: if (duckMusic) "duck" else "mix"
        set(value) {
            val mode = value.takeIf { it in setOf("mix", "duck", "pause") } ?: "duck"
            prefs.edit().putString("music_mode", mode).putBoolean("duck_music", mode != "mix").apply()
        }
    var engine: String
        get() = prefs.getString("engine", "") ?: ""
        set(value) { prefs.edit().putString("engine", value).apply() }
    var fields: Set<String>
        get() = (prefs.getStringSet("fields", setOf("speed", "battery", "distance")) ?: emptySet()).toSet()
        set(value) { prefs.edit().putStringSet("fields", value.toSet()).apply() }
    fun voice(language: String): String = prefs.getString("voice_$language", "") ?: ""
    fun setVoice(language: String, name: String) { prefs.edit().putString("voice_$language", name).apply() }
}
