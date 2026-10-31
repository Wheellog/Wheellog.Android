package com.cooper.wheellog

/** Stable keys, not generated resource IDs, survive upgrades and temporarily missing BMS support. */
object DashboardPages {
    // Match upstream: Smart BMS is inserted after Values when supported.
    val layouts = linkedMapOf("main" to R.layout.main_view_main,
        "values" to R.layout.main_view_params_list, "bms" to R.layout.main_view_smart_bms,
        "graph" to R.layout.main_view_graph, "trips" to R.layout.main_view_trips,
        "events" to R.layout.main_view_events)
    val titles = mapOf("main" to R.string.dashboard_main, "trips" to R.string.dashboard_trips,
        "values" to R.string.dashboard_values, "bms" to R.string.dashboard_bms,
        "graph" to R.string.dashboard_graph, "events" to R.string.dashboard_events)
    fun normalizeOrder(keys: List<String>) = listOf("main") +
        (keys + layouts.keys).filter { it != "main" && it in layouts }.distinct()
    fun visible(order: List<String>, enabled: Set<String>, bmsAvailable: Boolean) = normalizeOrder(order)
        .filter { (it == "main" || it in enabled) && (it != "bms" || bmsAvailable) }.map { layouts.getValue(it) }
}
