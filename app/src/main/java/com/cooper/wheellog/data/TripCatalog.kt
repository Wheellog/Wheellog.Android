package com.cooper.wheellog.data

import android.content.Context
import android.text.format.DateUtils
import android.text.format.DateFormat
import com.cooper.wheellog.LocaleManager
import com.cooper.wheellog.R
import com.cooper.wheellog.views.TripModel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TripCatalog {
    fun filenameTime(name: String): Long? = runCatching {
        SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US).apply { isLenient = false }.parse(name)?.time
    }.getOrNull()
    fun title(context: Context, model: TripModel, startedAt: Long?): String {
        context.getSharedPreferences("trip_titles", Context.MODE_PRIVATE).getString(model.fileName, null)?.let { return it }
        val time = startedAt ?: filenameTime(model.fileName) ?: return model.title
        val date = Date(time)
        val locale = LocaleManager.getLocale(context.resources)
        val now = System.currentTimeMillis()
        if (DateUtils.isToday(time)) return context.getString(R.string.today) + SimpleDateFormat(", EEE, HH:mm", locale).format(date)
        if (time <= now && now - time < 604_800_000L) return SimpleDateFormat("EEEE, HH:mm", locale).format(date)
        val thisYear = Calendar.getInstance().get(Calendar.YEAR)
        val year = Calendar.getInstance().apply { timeInMillis = time }.get(Calendar.YEAR)
        return SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, if (year == thisYear) "MMMM dd, HH:mm" else "yyyy MMMM dd, HH:mm"), locale).format(date)
    }
    fun item(context: Context, model: TripModel, stats: TripDataDbEntry? = null): TripListItem {
        val json = runCatching { JSONObject(stats?.additionalJson ?: "{}") }.getOrDefault(JSONObject())
        val time = json.optLong("startedAt").takeIf { it > 0 } ?: filenameTime(model.fileName)
        return TripListItem(model.uri.toString(), title(context, model, time), model.fileName, time,
            stats?.distance?.toDouble(), stats?.maxSpeedGps?.takeIf { json.optBoolean("hasGpsSpeed", true) }?.toDouble(),
            stats?.let { json.optInt("activeDurationMinutes", it.duration).toDouble() })
    }
    suspend fun load(context: Context, models: List<TripModel>, dao: TripDao,
        publish: suspend (List<TripListItem>) -> Unit = {}): List<TripListItem> {
        val cached = dao.getAll().associateBy { it.fileName }
        // Cards can already display these summaries. Sorting must use them too,
        // even while older CSVs elsewhere in a large library are being upgraded.
        val items = models.map { item(context, it, cached[it.fileName]) }.toMutableList()
        publish(items.toList())
        var lastPublished = System.nanoTime()
        var dirty = false
        models.forEachIndexed { index, model ->
            currentCoroutineContext().ensureActive()
            val stats = cached[model.fileName]
            val json = runCatching { JSONObject(stats?.additionalJson ?: "{}") }.getOrDefault(JSONObject())
            if (json.optInt("tripStatsVersion") != TripParser.STATS_VERSION || json.optLong("startedAt") <= 0 ||
                model.fileName == com.cooper.wheellog.LoggingService.currentLogFileName) {
                val ready = try {
                    TripParser.parseFile(context, model.fileName, model.pathLegacyAndroid ?: "", model.uri).second ?: stats
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (error: Exception) {
                    Timber.w(error, "Trip summary could not be refreshed: %s", model.fileName)
                    stats
                }
                items[index] = item(context, model, ready)
                dirty = true
                // Publish progress without causing hundreds of RecyclerView redraws.
                if (System.nanoTime() - lastPublished >= 100_000_000L) {
                    publish(items.toList()); lastPublished = System.nanoTime(); dirty = false
                }
            }
        }
        if (dirty) publish(items.toList())
        return items.toList()
    }
}
