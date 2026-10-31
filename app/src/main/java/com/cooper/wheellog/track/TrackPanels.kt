package com.cooper.wheellog.track

import android.content.*
import android.graphics.Color
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.graphics.drawable.RippleDrawable
import android.os.*
import android.text.TextUtils
import android.util.AttributeSet
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.R
import com.cooper.wheellog.utils.Constants
import com.cooper.wheellog.utils.MathsUtil
import com.cooper.wheellog.utils.SomeUtil.getColorEx
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.CopyrightOverlay
import java.text.SimpleDateFormat
import java.util.*

import com.cooper.wheellog.map.TripData
import com.cooper.wheellog.map.tripBodyTextColor
import com.cooper.wheellog.map.tripBodyMutedColor
import com.cooper.wheellog.map.tripTextColor
import com.cooper.wheellog.map.tripMutedColor
import com.cooper.wheellog.map.tripIconColor
import com.cooper.wheellog.map.tripSurfaceColor
import androidx.core.graphics.ColorUtils

fun Context.trackDp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()
fun trackDuration(milliseconds: Long): String {
    val seconds = milliseconds.coerceAtLeast(0) / 1_000
    return if (seconds >= 3_600) String.format(Locale.getDefault(), "%d:%02d:%02d", seconds / 3_600, seconds / 60 % 60, seconds % 60)
        else String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)
}

abstract class TrackPanel(context: Context) : LinearLayout(context), KoinComponent {
    protected val config: AppConfig by inject()
    init { orientation = VERTICAL; setBackgroundColor(context.getColorEx(R.color.background)) }
    fun setPageActive(active: Boolean) { onActiveChanged(active) }
    protected open fun onActiveChanged(active: Boolean) {}
    protected open fun release() {}
    fun dispose() { release() }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); onActiveChanged(true) }
    override fun onDetachedFromWindow() { onActiveChanged(false); super.onDetachedFromWindow() }
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (isAttachedToWindow) onActiveChanged(visibility == VISIBLE)
    }
    protected fun label(text: CharSequence = "", size: Float = 14f, muted: Boolean = false) = TextView(context).apply {
        this.text = text; textSize = size; setTextColor(if (muted) context.tripBodyMutedColor() else context.tripBodyTextColor())
    }
    protected fun button(text: Int, action: () -> Unit) = Button(context).apply {
        setText(text); textSize = 14f; minimumHeight = context.trackDp(48); minHeight = context.trackDp(48)
        minimumWidth = 0; minWidth = 0
        setPadding(context.trackDp(8), context.trackDp(8), context.trackDp(8), context.trackDp(8))
        val icon = context.tripIconColor()
        val foreground = context.tripTextColor()
        val surface = context.tripSurfaceColor()
        val muted = context.tripMutedColor()
        val states = arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf())
        setTextColor(ColorStateList(states, intArrayOf(muted, foreground)))
        backgroundTintList = null
        val palette = com.cooper.wheellog.utils.ThemeManager.palette
        val neutralButton = palette != null && palette != com.cooper.wheellog.utils.ColorPalette.Inferno
        // Neutral surface keeps dark lettering readable in light and medium palettes.
        val fill = GradientDrawable().apply {
            cornerRadius = context.trackDp(8).toFloat()
            setColor(ColorStateList(states, intArrayOf(surface, if (icon == foreground || neutralButton) surface else icon)))
            setStroke(context.trackDp(2), ColorStateList(states, intArrayOf(ColorUtils.setAlphaComponent(muted, 96), icon)))
        }
        background = RippleDrawable(ColorStateList.valueOf(ColorUtils.setAlphaComponent(foreground, 64)), fill, null)
        isAllCaps = false; setOnClickListener { action() }
    }
    protected fun distance(meters: Double): String {
        val value = if (config.useMph) MathsUtil.kmToMiles((meters / 1_000).toFloat()).toDouble() else meters / 1_000
        return String.format(Locale.getDefault(), "%.2f %s", value, context.getString(if (config.useMph) R.string.miles else R.string.km))
    }
    protected fun speed(value: Double): String = String.format(Locale.getDefault(), "%.1f", if (config.useMph) MathsUtil.kmToMiles(value.toFloat()).toDouble() else value)
}

/** Replay preserves recorded timing and skips gaps with no recorded position. */
class TrackMapPanel @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, private val fixedId: String? = null) : TrackPanel(context) {
    private val subtitle = label(size = 18f)
    private val appearance = TrackAppearance(context)
    val colorSelector = Spinner(context)
    val timeline = SeekBar(context).apply { max = TrackTimeline.STEPS; contentDescription = context.getString(R.string.track_timeline) }
    private val selectedText = label(size = 13f)
    private val legend = label(size = 12f, muted = true)
    private val map: MapView
    private val route = TrackRouteOverlay()
    private val cursor: Marker
    private var track: TripData? = null
    private var points = emptyList<TrackPoint>()
    private var recordedTimeline = TrackTimeline(points)
    private var shownId: String? = null
    private var shownCount = -1
    private var follow = true
    private var playing = false
    val play = button(R.string.track_play) { playing = !playing; updatePlay() }
    private val playback = TrackPlayback()
    private val playHandler = Handler(Looper.getMainLooper())
    private val player = object : Runnable {
        override fun run() {
            if (!playing) return
            if (track == null) return
            val end = recordedTimeline.durationMillis
            val position = playback.advance(SystemClock.elapsedRealtime(), end)
            timeline.progress = (position.toDouble() / end.coerceAtLeast(1) * TrackTimeline.STEPS).toInt().coerceIn(0, TrackTimeline.STEPS)
            if (position >= end) { playing = false; updatePlay() } else playHandler.postDelayed(this, 200)
        }
    }
    init {
        TrackMapConfiguration.prepare(context)
        map = MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK); setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            controller.setZoom(14.0); controller.setCenter(GeoPoint(config.lastLocationLaltitude, config.lastLocationLongitude))
            overlays.add(route); overlays.add(ScaleBarOverlay(this)); overlays.add(CopyrightOverlay(context))
            setOnTouchListener { _, event -> parent?.requestDisallowInterceptTouchEvent(event.action != MotionEvent.ACTION_UP && event.action != MotionEvent.ACTION_CANCEL); false }
        }
        cursor = Marker(map).apply { title = context.getString(R.string.track_selected_point); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM); isEnabled = false }
        map.overlays.add(cursor)
        val scroll = ScrollView(context).apply { isFillViewport = true }
        val column = LinearLayout(context).apply { orientation = VERTICAL; setPadding(context.trackDp(16), 0, context.trackDp(16), context.trackDp(24)) }
        addView(scroll, LayoutParams(-1, -1)); scroll.addView(column, FrameLayout.LayoutParams(-1, -2))
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val radius = if (Build.VERSION.SDK_INT >= 31) {
                val native = insets.toWindowInsets()
                maxOf(native?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_LEFT)?.radius ?: 0,
                    native?.getRoundedCorner(RoundedCorner.POSITION_BOTTOM_RIGHT)?.radius ?: 0)
            } else 0
            val corner = kotlin.math.ceil(radius * (1 - 1 / kotlin.math.sqrt(2.0))).toInt()
            val side = maxOf(context.trackDp(16), corner)
            column.setPadding(side + bars.left, 0, side + bars.right, maxOf(context.trackDp(24), corner) + bars.bottom)
            insets
        }
        colorSelector.backgroundTintList = ColorStateList.valueOf(context.tripBodyTextColor())
        colorSelector.setPopupBackgroundDrawable(ColorDrawable(context.tripSurfaceColor()))
        column.addView(subtitle)
        colorSelector.adapter = selectorAdapter(listOf(context.getString(R.string.track_color_none), context.getString(R.string.track_color_speed)))
        colorSelector.setSelection(appearance.mode.ordinal)
        colorSelector.contentDescription = context.getString(R.string.track_line_color)
        colorSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val mode = TrackColorMode.entries[position]
                appearance.mode = mode; route.mode = mode; updateLegend(); map.invalidate()
            }
        }
        column.addView(label(context.getString(R.string.track_line_color), 12f, true).apply {
            includeFontPadding = false
        }, LayoutParams(-1, -2).apply { topMargin = context.trackDp(14) })
        column.addView(colorSelector, LayoutParams(-1, context.trackDp(44))); column.addView(legend)
        column.addView(map, LayoutParams(-1, context.trackDp(320)))
        column.addView(selectedText)
        column.addView(timeline, LayoutParams(-1, context.trackDp(48)))
        val replay = LinearLayout(context)
        replay.addView(play, LayoutParams(0, -2, 1f).apply { marginEnd = context.trackDp(4) })
        replay.addView(button(R.string.track_follow_end) { follow = true; playing = false; updatePlay(); timeline.progress = timeline.max; selectPoint(true) }, LayoutParams(0, -2, 1f))
        column.addView(replay)
        timeline.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) { follow = false; playing = false; updatePlay() }
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) { follow = false; playing = false; updatePlay() }
                selectPoint(fromUser || playing)
            }
        })
    }
    private fun selectorAdapter(items: List<String>) = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, items) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
            super.getView(position, convertView, parent).also {
                (it as TextView).setTextColor(context.tripBodyTextColor()); it.textSize = 16f
                it.minHeight = context.trackDp(32); it.minimumHeight = context.trackDp(32)
                it.gravity = Gravity.CENTER_VERTICAL
                it.setPadding(context.trackDp(12), 0, context.trackDp(12), 0)
            }
        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
            super.getDropDownView(position, convertView, parent).also {
                (it as TextView).setTextColor(context.tripTextColor()); it.setBackgroundColor(context.tripSurfaceColor())
                it.minimumHeight = context.trackDp(48)
            }
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val column = map.parent as ViewGroup
        val controlsHeight = (0 until column.childCount).sumOf { index ->
            column.getChildAt(index).let {
                val margins = it.layoutParams as? ViewGroup.MarginLayoutParams
                if (it === map || it.visibility == GONE) 0
                else it.measuredHeight + (margins?.topMargin ?: 0) + (margins?.bottomMargin ?: 0)
            }
        } + column.paddingTop + column.paddingBottom
        val height = (MeasureSpec.getSize(heightMeasureSpec) - controlsHeight).coerceAtLeast(context.trackDp(180))
        if (map.layoutParams.height != height) {
            map.layoutParams.height = height
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
    private fun updatePlay() {
        play.setText(if (playing) R.string.track_pause_replay else R.string.track_play)
        playHandler.removeCallbacks(player)
        if (playing) {
            follow = false
            if (timeline.progress == timeline.max) timeline.progress = 0
            playback.start(recordedTimeline.positionForProgress(timeline.progress), SystemClock.elapsedRealtime())
            playHandler.postDelayed(player, 200)
        }
    }
    fun showTrip(loaded: TripData) {
        if (colorSelector.selectedItemPosition != appearance.mode.ordinal) colorSelector.setSelection(appearance.mode.ordinal)
        track = loaded
        subtitle.text = "${loaded.title} · ${distance(loaded.distanceMeters)}"
        val changed = shownId != loaded.fileName
        val changedPoints = changed || points != loaded.routePoints
        if (changedPoints) {
            if (changed) { playing = false; updatePlay() }
            points = loaded.routePoints
            recordedTimeline = TrackTimeline(points)
            shownId = loaded.fileName
            route.points = TrackVisuals.displayPoints(points)
            route.solid = context.getColorEx(R.color.accent)
            route.maximumSpeed = (points.maxOfOrNull { TrackVisuals.speed(it) } ?: 0.0).coerceAtLeast(1.0)
            route.mode = appearance.mode
            if (changed && points.isNotEmpty()) {
                map.post { if (isAttachedToWindow) {
                    if (points.size == 1) { map.controller.setZoom(16.0); map.controller.setCenter(GeoPoint(points.first().latitude, points.first().longitude)) }
                    else map.zoomToBoundingBox(BoundingBox.fromGeoPoints(points.map { GeoPoint(it.latitude, it.longitude) }), false, context.trackDp(24), 18.0, 0L)
                } }
                follow = true
            }
            if (follow) timeline.progress = timeline.max
        }
        timeline.isEnabled = points.size > 1; play.isEnabled = recordedTimeline.durationMillis > 0
        updateLegend()
        if (points.isEmpty()) {
            selectedText.setText(R.string.track_no_points)
            cursor.isEnabled = false; timeline.contentDescription = context.getString(R.string.track_no_points)
        }
        else selectPoint(false)
        map.invalidate()
    }
    private fun updateLegend() {
        legend.text = when (appearance.mode) {
            TrackColorMode.NONE -> context.getString(R.string.track_color_none)
            TrackColorMode.SPEED -> context.getString(R.string.track_speed_legend, speed(route.maximumSpeed), context.getString(if (config.useMph) R.string.mph else R.string.kmh))
        }
    }
    private fun selectPoint(center: Boolean) {
        if (track == null) return
        val selection = recordedTimeline.select(timeline.progress) ?: run {
            selectedText.setText(R.string.track_no_points)
            cursor.isEnabled = false; timeline.contentDescription = context.getString(R.string.track_no_points); return
        }
        val point = points[selection.index]
        val time = SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault()).format(Date(selection.requestedTime))
        val details = if (selection.inGap) "$time · ${context.getString(R.string.track_timeline_gap)}" else {
            val pwm = point.telemetry?.takeIf { it.pwmAvailable }?.let { String.format(Locale.getDefault(), "%.1f%%", it.pwm) } ?: "—"
            "$time · ${speed(TrackVisuals.speed(point))} ${context.getString(if (config.useMph) R.string.mph else R.string.kmh)} · PWM $pwm"
        }
        selectedText.text = details
        cursor.position = GeoPoint(point.latitude, point.longitude)
        val refreshPopup = cursor.isInfoWindowShown
        cursor.snippet = details; cursor.isEnabled = !selection.inGap
        if (refreshPopup) { cursor.closeInfoWindow(); if (!selection.inGap) cursor.showInfoWindow() }
        timeline.contentDescription = "${context.getString(R.string.track_timeline)}. $details"
        if (center && !selection.inGap) {
            if (playing) map.controller.setCenter(cursor.position) else map.controller.animateTo(cursor.position)
        }
        map.invalidate()
    }
    override fun onActiveChanged(active: Boolean) { if (active) map.onResume() else { map.onPause(); playing = false; updatePlay() } }
    override fun release() { playing = false; playHandler.removeCallbacksAndMessages(null); map.onPause(); map.onDetach() }
}
