package com.cooper.wheellog

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class InfoBlockPreset(val id: String, val name: String, val order: List<String>, val enabled: Set<String>)

class InfoBlockPresets(private val preferences: SharedPreferences, private val blocks: InfoBlocks) {
    companion object {
        const val LIMIT = 10
        const val NAME_LIMIT = 60
        private const val KEY = "info_block_presets_v1"
    }

    fun read(): List<InfoBlockPreset> {
        val json = runCatching { JSONObject(preferences.getString(KEY, "{}") ?: "{}") }.getOrNull() ?: return emptyList()
        val rows = json.optJSONArray("presets") ?: return emptyList()
        fun JSONObject.strings(key: String): List<String> {
            val values = optJSONArray(key) ?: return emptyList()
            return (0 until values.length()).mapNotNull { values.optString(it).takeIf { value -> value.isNotBlank() } }
        }
        return (0 until rows.length()).mapNotNull { index ->
            val row = rows.optJSONObject(index) ?: return@mapNotNull null
            val id = row.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val name = row.optString("name").trim().takeIf { it.isNotEmpty() && it.length <= NAME_LIMIT } ?: return@mapNotNull null
            val enabled = blocks.normalize(row.strings("enabled")).filter { it in blocks.titles }.toSet()
            InfoBlockPreset(id, name, blocks.order(row.strings("order"), enabled.toList()), enabled)
        }.distinctBy { it.id }.take(LIMIT)
    }

    /** An existing ID updates that preset; a new preset never overwrites another. */
    fun save(name: String, order: List<String>, enabled: Set<String>, id: String? = null): InfoBlockPreset {
        val rows = read().toMutableList()
        val title = name.trim()
        require(title.isNotEmpty() && title.length <= NAME_LIMIT)
        require(rows.none { it.id != id && it.name.equals(title, ignoreCase = true) })
        val index = if (id == null) -1 else rows.indexOfFirst { it.id == id }.also { require(it >= 0) }
        require(index >= 0 || rows.size < LIMIT)
        val selected = blocks.normalize(enabled.toList()).filter { it in blocks.titles }.toSet()
        val preset = InfoBlockPreset(id ?: UUID.randomUUID().toString(), title, blocks.order(order, selected.toList()), selected)
        if (index < 0) rows.add(preset) else rows[index] = preset
        write(rows)
        return preset
    }

    fun delete(id: String) = write(read().filterNot { it.id == id })

    private fun write(rows: List<InfoBlockPreset>) {
        val json = JSONObject().put("version", 1).put("presets", JSONArray().apply {
            rows.forEach { row -> put(JSONObject().put("id", row.id).put("name", row.name)
                .put("order", JSONArray(row.order)).put("enabled", JSONArray(row.enabled.toList()))) }
        })
        preferences.edit().putString(KEY, json.toString()).apply()
    }
}
