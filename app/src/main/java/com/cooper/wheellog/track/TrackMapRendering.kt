package com.cooper.wheellog.track

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.preference.PreferenceManager
import com.cooper.wheellog.BuildConfig
import com.cooper.wheellog.data.NativeTripFiles
import com.cooper.wheellog.views.TripModel
import com.cooper.wheellog.R
import com.cooper.wheellog.utils.SomeUtil.getColorEx
import kotlinx.coroutines.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import java.io.File
import kotlin.math.*

object TrackMapConfiguration {
    @Synchronized fun prepare(context: Context) {
        if (ready) return
        Configuration.getInstance().apply {
            load(context.applicationContext, PreferenceManager.getDefaultSharedPreferences(context))
            userAgentValue = BuildConfig.APPLICATION_ID
            osmdroidBasePath = File(context.filesDir, "osmdroid").apply { mkdirs() }
            osmdroidTileCache = File(osmdroidBasePath, "tiles").apply { mkdirs() }
        }
        ready = true
    }
    private var ready = false
}

class TrackRouteOverlay : Overlay() {
    var points: List<TrackPoint> = emptyList()
    var mode = TrackColorMode.NONE
    var solid = Color.RED
    var maximumSpeed = 1.0
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        val density = mapView.resources.displayMetrics.density
        val pixels = points.map { mapView.projection.toPixels(GeoPoint(it.latitude, it.longitude), null) }
        // Draw the entire casing first so adjacent colored segments have no dark seams.
        for (outline in listOf(true, false)) {
            paint.strokeWidth = (if (outline) 7.6f else 5.6f) * density
            for (index in 1 until points.size) {
                if (points[index].segmentStart) continue
                val from = pixels[index - 1]
                val to = pixels[index]
                paint.color = if (outline) Color.rgb(40, 42, 45) else TrackVisuals.color(points[index], mode, solid, maximumSpeed)
                canvas.drawLine(from.x.toFloat(), from.y.toFloat(), to.x.toFloat(), to.y.toFloat(), paint)
            }
        }
        paint.strokeWidth = 5.6f * density
        for (point in listOfNotNull(points.firstOrNull(), points.lastOrNull())) {
            val pixel = mapView.projection.toPixels(GeoPoint(point.latitude, point.longitude), null)
            paint.color = Color.WHITE
            canvas.drawCircle(pixel.x.toFloat(), pixel.y.toFloat(), paint.strokeWidth * 1.25f, paint)
            paint.color = solid
            canvas.drawCircle(pixel.x.toFloat(), pixel.y.toFloat(), paint.strokeWidth * .65f, paint)
        }
    }
}

/** A shared on-demand tile provider; releasing the last thumbnail closes its worker threads. */
private object ThumbnailTiles {
    private var provider: MapTileProviderBasic? = null
    private val owners = mutableSetOf<Handler>()
    fun acquire(context: Context, handler: Handler): MapTileProviderBasic {
        TrackMapConfiguration.prepare(context)
        val result = provider ?: MapTileProviderBasic(context.applicationContext, TileSourceFactory.MAPNIK).also { provider = it }
        if (owners.add(handler)) result.tileRequestCompleteHandlers.add(handler)
        return result
    }
    fun release(handler: Handler) {
        provider?.tileRequestCompleteHandlers?.remove(handler)
        owners.remove(handler)
        if (owners.isEmpty()) { provider?.detach(); provider = null }
    }
}

const val THUMBNAIL_ROUTE_COLOR: Int = 0xffff123d.toInt()

class TrackThumbnailView @JvmOverloads constructor(context: Context, attrs: android.util.AttributeSet? = null) : View(context, attrs) {
    init {
        clipToOutline = true
        outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(view: View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, 8 * resources.displayMetrics.density)
            }
        }
    }

    private var job: Job? = null
    private var bindingKey: String? = null
    private var source: TripModel? = null
    private var revision = 0
    private var points = emptyList<TrackWorldPoint>()
    var maximumSpeed = 1.0
    private var tiles: MapTileProviderBasic? = null
    private val complete = Handler(Looper.getMainLooper()) { invalidate(); true }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    fun bind(trip: TripModel, count: Int = 0) {
        source = trip
        revision = count
        val key = "${trip.uri}:$count"
        if (bindingKey == key) { invalidate(); return }
        bindingKey = key
        points = emptyList()
        job?.cancel()
        job = scope.launch {
            val loaded = withContext(Dispatchers.IO) { runCatching { TrackVisuals.displayPoints(NativeTripFiles.points(context, trip), 800) }.getOrDefault(emptyList()) }
            if (bindingKey != key) return@launch
            points = TrackProjection.project(loaded)
            invalidate()
        }
        contentDescription = context.getString(R.string.track_thumbnail)
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        tiles = ThumbnailTiles.acquire(context, complete)
        source?.let { if (points.isEmpty() && job?.isActive != true) { bindingKey = null; bind(it, revision) } }
    }
    override fun onDetachedFromWindow() {
        job?.cancel(); ThumbnailTiles.release(complete); tiles = null
        super.onDetachedFromWindow()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(context.getColorEx(R.color.background))
        if (points.isEmpty()) {
            paint.color = context.getColorEx(R.color.secondary_text); paint.textSize = 11 * resources.displayMetrics.scaledDensity
            canvas.drawText(context.getString(R.string.track_no_points), 8f, height / 2f, paint)
            return
        }
        val padding = 16 * resources.displayMetrics.density
        val minX = points.minOf { it.x }; val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }; val maxY = points.maxOf { it.y }
        val fit = min((width - padding * 2).coerceAtLeast(1f) / (maxX - minX).coerceAtLeast(.00001),
            (height - padding * 2).coerceAtLeast(1f) / (maxY - minY).coerceAtLeast(.00001))
        val zoom = floor(log2(fit / 256)).toInt().coerceIn(0, 17)
        val scale = 256.0 * (1 shl zoom)
        val left = (minX + maxX) / 2 * scale - width / 2
        val top = (minY + maxY) / 2 * scale - height / 2
        for (x in floor(left / 256).toInt()..floor((left + width) / 256).toInt()) {
            for (y in floor(top / 256).toInt()..floor((top + height) / 256).toInt()) {
                if (y !in 0 until (1 shl zoom)) continue
                val wrapped = ((x % (1 shl zoom)) + (1 shl zoom)) % (1 shl zoom)
                val drawable = tiles?.getMapTile(MapTileIndex.getTileIndex(zoom, wrapped, y)) ?: continue
                val tileLeft = (x * 256 - left).roundToInt(); val tileTop = (y * 256 - top).roundToInt()
                drawable.setBounds(tileLeft, tileTop, tileLeft + 256, tileTop + 256)
                drawable.draw(canvas)
            }
        }
        // Improve route contrast without replacing real map data with a decorative map.
        canvas.drawColor(0x33000000)
        val solid = THUMBNAIL_ROUTE_COLOR
        paint.strokeWidth = 3 * resources.displayMetrics.density
        var previous: TrackWorldPoint? = null
        points.forEach { point ->
            if (!point.point.segmentStart) previous?.let {
                paint.color = solid
                canvas.drawLine((it.x * scale - left).toFloat(), (it.y * scale - top).toFloat(),
                    (point.x * scale - left).toFloat(), (point.y * scale - top).toFloat(), paint)
            }
            previous = point
        }
        for (point in listOf(points.first(), points.last())) {
            paint.color = Color.WHITE
            canvas.drawCircle((point.x * scale - left).toFloat(), (point.y * scale - top).toFloat(), paint.strokeWidth * 1.4f, paint)
            paint.color = solid
            canvas.drawCircle((point.x * scale - left).toFloat(), (point.y * scale - top).toFloat(), paint.strokeWidth * .8f, paint)
        }
        // Small route thumbnails are exempt from individual attribution; the full map retains it.
    }
}
