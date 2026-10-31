package com.cooper.wheellog

import android.content.Context
import android.content.res.Configuration
import java.text.Collator
import java.util.Locale

/** Resource names are stable preference keys; translated titles are display-only. */
class InfoBlocks(private val context: Context) {
    val titles = listOf(
        R.string.pwm, R.string.max_pwm, R.string.voltage, R.string.battery,
        R.string.top_speed, R.string.average_riding_speed, R.string.average_speed,
        R.string.riding_time, R.string.ride_time, R.string.current, R.string.maxcurrent,
        R.string.phase_current, R.string.maxphasecurrent, R.string.power, R.string.maxpower,
        R.string.temperature, R.string.temperature2, R.string.maxtemperature,
        R.string.distance, R.string.total, R.string.wheel_distance, R.string.remaining_distance,
        R.string.battery_per_km, R.string.consumption, R.string.avg_cell_volt,
        R.string.user_distance, R.string.speed
    ).associateBy { context.resources.getResourceEntryName(it) }

    val defaults = listOf("pwm", "max_pwm", "voltage", "average_riding_speed",
        "riding_time", "top_speed", "distance", "total")

    private val legacyIds: Map<String, String> by lazy {
        val aliases = mutableMapOf<String, String>()
        // Old versions stored the translated label. Read every bundled language,
        // including the previous language after the user has already switched.
        val locales = (context.assets.locales.toList() + listOf("en", "ru"))
            .map { Locale.forLanguageTag(it.replace('_', '-')) }.distinct()
        for (locale in locales) {
            val config = Configuration(context.resources.configuration).apply { setLocale(locale) }
            val resources = context.createConfigurationContext(config).resources
            titles.forEach { (id, title) -> aliases.putIfAbsent(resources.getString(title), id) }
        }
        titles.keys.forEach { aliases[it] = it }
        aliases
    }

    /** Keep unrecognised old entries intact, rather than silently erasing user data. */
    fun normalize(values: List<String>): List<String> = values.filter { it.isNotBlank() }
        .map { if (it in titles) it else legacyIds[it] ?: it }.distinct()

    fun order(values: List<String>, selected: List<String>): List<String> =
        (normalize(values).filter { it in titles } + selected.filter { it in titles } + titles.keys).distinct()

    /** Only enabled blocks have a user-defined order. Other choices follow the UI language. */
    fun editorOrder(values: List<String>, selected: Set<String>): List<String> {
        val complete = order(values, selected.toList())
        val collator = Collator.getInstance(LocaleManager.getLocale(context.resources))
        return complete.filter { it in selected } + complete.filter { it !in selected }
            .sortedWith { left, right ->
                val result = collator.compare(context.getString(titles.getValue(left)), context.getString(titles.getValue(right)))
                if (result == 0) left.compareTo(right) else result
            }
    }

    fun toggleInEditor(values: List<String>, selected: Set<String>, key: String, enabled: Boolean): List<String> {
        if (key !in titles) return editorOrder(values, selected)
        val nextSelected = if (enabled) selected + key else selected - key
        val nextOrder = if (enabled && key !in selected)
            values.filter { it in selected } + key + values else values
        return editorOrder(nextOrder, nextSelected)
    }
}
