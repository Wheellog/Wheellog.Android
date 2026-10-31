package com.cooper.wheellog.track

import android.app.Application
import android.content.*
import android.view.*
import android.widget.*
import androidx.test.core.app.ApplicationProvider
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.graphics.toArgb
import com.cooper.wheellog.*
import com.cooper.wheellog.data.*
import com.cooper.wheellog.map.*
import com.cooper.wheellog.utils.ThemeManager
import com.cooper.wheellog.utils.NotificationUtil
import io.mockk.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.*
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import java.io.File
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29])
class TrackScreenTest {
    private lateinit var base: Context
    private lateinit var config: AppConfig
    @Before fun setup() {
        base = ApplicationProvider.getApplicationContext()
        config = AppConfig(base).apply { appThemeInt = com.cooper.wheellog.utils.ThemeEnum.Inferno.value }
        val dao = mockk<TripDao>(relaxed = true)
        every { dao.getTripByFileName(any()) } returns null
        startKoin { modules(module { single { config }; single { dao } }) }
        ThemeManager.theme = R.style.InfernoTheme
        base.getSharedPreferences("track_appearance", Context.MODE_PRIVATE).edit().clear().commit()
    }
    @After fun cleanup() { ThemeManager.theme = R.style.OriginalTheme; unmockkAll(); stopKoin() }
    private fun trip(): TripData {
        val first = TrackPoint(1_000, 56.8, 60.6, null, 5f, 10f, true)
        return TripData("Поездка к реке", routePoints = listOf(first,
            first.copy(time = 31_000, latitude = 56.801, segmentStart = false),
            first.copy(time = 61_000, latitude = 56.802, segmentStart = false)),
            startedAt = 1_000, endedAt = 61_000, distanceMeters = 200.0, fileName = "native.csv")
    }
    private fun texts(view: View): List<TextView> = if (view is ViewGroup)
        (0 until view.childCount).flatMap { texts(view.getChildAt(it)) } else if (view is TextView) listOf(view) else emptyList()
    @Test fun `appearance defaults to solid and retired PWM setting falls back to solid`() {
        assertEquals(TrackColorMode.NONE, TrackAppearance(base).mode)
        TrackAppearance(base).mode = TrackColorMode.SPEED
        assertEquals(TrackColorMode.SPEED, TrackAppearance(base).mode)
        base.getSharedPreferences("track_appearance", Context.MODE_PRIVATE).edit().putString("line_color", "pwm").commit()
        assertEquals(TrackColorMode.NONE, TrackAppearance(base).mode)
    }
    @Test fun `trip menu and replay buttons follow main menu colors in every theme and day night mode`() {
        fun color(item: android.view.MenuItem): Int {
            val text = item.title as android.text.Spanned
            return text.getSpans(0, text.length, android.text.style.ForegroundColorSpan::class.java).single().foregroundColor
        }
        for (theme in listOf(R.style.OriginalTheme, R.style.AJDMTheme, R.style.ContrastTheme, R.style.InfernoTheme, R.style.PorcelainTheme, R.style.PolarTheme, R.style.JadeTheme, R.style.SteelTheme, R.style.HeatherTheme)) {
            for (night in listOf(false, true)) {
                ThemeManager.theme = theme
                val configuration = android.content.res.Configuration(base.resources.configuration).apply {
                    uiMode = uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv() or
                        if (night) android.content.res.Configuration.UI_MODE_NIGHT_YES else android.content.res.Configuration.UI_MODE_NIGHT_NO
                }
                val context = ContextThemeWrapper(base.createConfigurationContext(configuration), theme)
                val menu = MenuBuilder(context)
                val entry = menu.add(0, 3, 0, R.string.track_share_csv).apply {
                    icon = context.getDrawable(ThemeManager.getId(com.cooper.wheellog.utils.ThemeIconEnum.TripsShare))
                }
                menu.add(0, 6, 1, R.string.track_rename).icon = context.getDrawable(R.drawable.ic_trip_rename)
                menu.add(0, 7, 2, R.string.track_share).icon = context.getDrawable(R.drawable.ic_trip_share_gpx)
                val blocked = menu.add(0, 4, 3, R.string.trip_menu_delete_file).apply { isEnabled = false }
                com.cooper.wheellog.views.styleTripMenu(context, menu)
                assertTrue(entry.title.toString().endsWith("CSV"))
                val expectedText = when (theme) {
                    R.style.InfernoTheme -> context.getColor(R.color.inferno_battery)
                    R.style.ContrastTheme -> context.getColor(R.color.contrast_accent)
                    else -> ThemeManager.palette?.let { context.getColor(it.foreground) } ?: (if (night) com.cooper.wheellog.ui.theme.md_theme_dark_primary else com.cooper.wheellog.ui.theme.md_theme_light_primary).toArgb()
                }
                val accent = when (theme) {
                    R.style.InfernoTheme -> context.getColor(R.color.inferno_accent)
                    R.style.ContrastTheme -> context.getColor(R.color.contrast_accent)
                    else -> ThemeManager.palette?.let { context.getColor(it.accent) } ?: expectedText
                }
                for (id in listOf(3, 6, 7)) {
                    val item = menu.findItem(id)
                    assertEquals(expectedText, color(item))
                    val tint = ReflectionHelpers.getField<android.content.res.ColorStateList>(item.icon!!.constantState, "mTint")
                    assertEquals(accent, tint.defaultColor)
                    assertEquals(context.trackDp(24), item.icon!!.intrinsicWidth)
                }
                assertEquals(context.tripMutedColor(), color(blocked))
                val background = ThemeManager.palette?.let { context.getColor(it.surface) } ?: context.getColor(when (theme) {
                    R.style.InfernoTheme -> R.color.inferno_surface
                    R.style.ContrastTheme -> R.color.contrast_surface
                    else -> R.color.trip_popup_background
                })
                assertTrue("Menu text must remain readable: theme=$theme, night=$night",
                    androidx.core.graphics.ColorUtils.calculateContrast(expectedText, background) >= 4.5)
                val panel = TrackMapPanel(context)
                try {
                    panel.showTrip(trip())
                    assertEquals(context.tripBodyTextColor(), panel.colorSelector.backgroundTintList!!.defaultColor)
                    val selected = panel.colorSelector.adapter.getView(0, null, panel.colorSelector) as TextView
                    assertEquals(context.tripBodyTextColor(), selected.currentTextColor)
                    for (id in listOf(R.string.track_play, R.string.track_follow_end)) {
                        val button = texts(panel).single { it.text == base.getString(id) } as Button
                        assertEquals(expectedText, button.textColors.defaultColor)
                        val fill = (button.background as android.graphics.drawable.RippleDrawable).getDrawable(0) as android.graphics.drawable.GradientDrawable
                        assertEquals(if (theme == R.style.InfernoTheme) accent else background, fill.color!!.defaultColor)
                        val stroke = ReflectionHelpers.getField<android.content.res.ColorStateList>(fill.constantState, "mStrokeColors")
                        assertEquals(accent, stroke.defaultColor)
                    }
                } finally { panel.dispose() }
            }
        }
    }
    @Test fun `native CSV viewer shows selected point under map and omits time range above replay buttons`() {
        val context = ContextThemeWrapper(base, R.style.InfernoTheme)
        val panel = TrackMapPanel(context)
        try {
            panel.showTrip(trip())
            val labels = texts(panel)
            assertTrue(labels.any { it.text.startsWith("Поездка к реке · ") })
            assertFalse(labels.any { it.text == "←" || it.text == base.getString(R.string.track_map_title) || it.text.startsWith(base.getString(R.string.track_saved_trip)) })
            assertEquals(2, panel.colorSelector.adapter.count)
            assertFalse(labels.any { it.text == base.getString(R.string.track_playback_speed) })
            val pointReadout = labels.single { it.text.contains("PWM") }
            val column = panel.timeline.parent as ViewGroup
            val timelineIndex = column.indexOfChild(panel.timeline)
            assertSame(pointReadout, column.getChildAt(timelineIndex - 1))
            val replayRow = column.getChildAt(timelineIndex + 1) as LinearLayout
            assertEquals(2, replayRow.childCount)
            assertTrue((0..1).all { replayRow.getChildAt(it) is Button })
            val lastPoint = pointReadout.text.toString()
            panel.timeline.progress = 0
            assertNotEquals(lastPoint, pointReadout.text.toString())
            val caption = labels.single { it.text == base.getString(R.string.track_line_color) }
            val title = labels.single { it.text.startsWith("Поездка к реке · ") }
            for (id in listOf(R.string.track_play, R.string.track_follow_end)) {
                val button = labels.single { it.text == base.getString(id) }
                assertEquals(base.getColor(R.color.inferno_battery), button.textColors.defaultColor)
                val background = button.background as android.graphics.drawable.RippleDrawable
                assertEquals(base.getColor(R.color.inferno_accent), (background.getDrawable(0) as android.graphics.drawable.GradientDrawable).color!!.defaultColor)
            }
            for ((width, height) in listOf(320 to 640, 640 to 320, 800 to 1_000)) {
                panel.measure(View.MeasureSpec.makeMeasureSpec(base.trackDp(width), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(base.trackDp(height), View.MeasureSpec.EXACTLY))
                panel.layout(0, 0, panel.measuredWidth, panel.measuredHeight)
                assertTrue(caption.top - title.bottom >= context.trackDp(12))
                assertEquals(caption.bottom, panel.colorSelector.top)
                if (height >= 640) {
                    val column = caption.parent as View
                    assertTrue("Portrait controls must fit without clipping: ${column.height} > ${panel.height}", column.height <= panel.height)
                }
                for (button in labels.filterIsInstance<Button>()) {
                    assertTrue(button.width >= base.trackDp(100))
                    assertTrue(button.layout.getLineBottom(button.lineCount - 1) <= button.height - button.totalPaddingTop - button.totalPaddingBottom)
                }
            }
        } finally { panel.dispose() }
    }
    @Test fun `replay pauses and resumes at its current position then returns to Play on completion`() {
        val panel = TrackMapPanel(ContextThemeWrapper(base, R.style.InfernoTheme))
        try {
            panel.showTrip(trip())
            panel.play.performClick()
            assertEquals(base.getString(R.string.track_pause_replay), panel.play.text.toString())
            shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(200))
            assertTrue(panel.timeline.progress in 1_900..2_100)
            panel.play.performClick()
            val paused = panel.timeline.progress
            shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
            assertEquals(paused, panel.timeline.progress)
            panel.play.performClick()
            shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(6))
            assertEquals(panel.timeline.max, panel.timeline.progress)
            assertEquals(base.getString(R.string.track_play), panel.play.text.toString())
        } finally { panel.dispose() }
    }
    @Test fun `replay controls skip an unrecorded daily gap and retain the selected real timestamp`() {
        val panel = TrackMapPanel(ContextThemeWrapper(base, R.style.InfernoTheme))
        try {
            val route = trip().routePoints
            val lunch = 4 * 3_600_000L
            panel.showTrip(trip().copy(routePoints = listOf(route[0], route[1],
                route[0].copy(time = lunch, segmentStart = true), route[1].copy(time = lunch + 30_000)), endedAt = lunch + 30_000))
            panel.play.performClick()
            shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
            assertTrue(panel.timeline.progress in 5_900..6_100)
            val timestamp = java.text.SimpleDateFormat("dd.MM HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(lunch + 6_000))
            assertTrue(texts(panel).single { it.text.contains("PWM") }.text.startsWith(timestamp))
            shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(400))
            assertEquals(panel.timeline.max, panel.timeline.progress)
            assertEquals(base.getString(R.string.track_play), panel.play.text.toString())
        } finally { panel.dispose() }
    }
    @Test fun `only the native CSV logging service is registered`() {
        val info = base.packageManager.getPackageInfo(base.packageName, android.content.pm.PackageManager.GET_SERVICES)
        assertFalse(info.services!!.any { it.name.endsWith(".track.TrackService") })
        assertEquals(1, info.services!!.count { it.name.endsWith(".LoggingService") })
    }
    @Test fun `record menu exposes three commands with unavailable items disabled and muted`() {
        val controller = Robolectric.buildActivity(android.app.Activity::class.java).setup().visible()
        val activity = controller.get().apply { setTheme(R.style.InfernoTheme) }
        val anchor = Button(activity); activity.setContentView(anchor)
        try {
            for (state in listOf(TrackStatus(), TrackStatus(recording = true), TrackStatus(recording = true, paused = true))) {
                val actions = mutableListOf<String>()
                val popup = TrackRecordingControl.showMenu(activity, anchor, state, true) { actions.add(it) }
                try {
                    val enabled = TrackRecordingControl.availability(state, true)
                    assertEquals(listOf(enabled.start, enabled.stop, enabled.pause), (0..2).map { popup.menu.getItem(it).isEnabled })
                    for (index in 0..2) if (!popup.menu.getItem(index).isEnabled) {
                        val label = popup.menu.getItem(index).title as android.text.Spanned
                        assertEquals(0xff69777b.toInt(), label.getSpans(0, label.length, android.text.style.ForegroundColorSpan::class.java).single().foregroundColor)
                    }
                    for (index in 0..2) popup.menu.performIdentifierAction(index + 1, 0)
                    assertEquals(when {
                        !state.recording -> listOf(LoggingService.START)
                        state.paused -> listOf(LoggingService.RESUME, LoggingService.STOP)
                        else -> listOf(LoggingService.STOP, LoggingService.PAUSE)
                    }, actions)
                } finally { popup.dismiss() }
            }
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun `main toolbar keeps its record indicator and notification removes both recording buttons`() {
        val context = ContextThemeWrapper(base, R.style.InfernoTheme)
        val menu = MenuBuilder(context)
        SupportMenuInflater(context).inflate(R.menu.main, menu)
        assertNotNull(menu.findItem(R.id.miTrackRecord))
        for (index in 0 until menu.size()) assertTrue((menu.getItem(index) as MenuItemImpl).requiresActionButton())
        val data = mockk<WheelData>(relaxed = true)
        mockkStatic(WheelData::class)
        every { WheelData.getInstance() } returns data
        every { data.bluetoothService } returns null
        val notifications = NotificationUtil(context)
        try {
            notifications.update()
            val root = notifications.notification!!.contentView.apply(context, FrameLayout(context))
            assertEquals(View.GONE, root.findViewById<View>(R.id.ib_track).visibility)
            assertEquals(View.GONE, root.findViewById<View>(R.id.ib_logging).visibility)
        } finally { notifications.close() }
    }
    @Test @Config(sdk = [29, 35]) fun `original trip activity retains Map Statistics Values tabs with themed labels`() {
        // Native cards on Android 10+ use MediaStore, not file:// links to the Windows test directory.
        val provider = NativeMediaProvider(File(base.cacheDir, "viewer-media").apply { mkdirs() })
        provider.attachInfo(base, android.content.pm.ProviderInfo().apply { authority = "media" })
        org.robolectric.shadows.ShadowContentResolver.registerProviderInternal("media", provider)
        val name = "viewer.csv"
        val uri = provider.insert(android.provider.MediaStore.Downloads.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY),
            ContentValues().apply { put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name) })
        provider.files.values.single().first.writeText(NativeCsv.HEADER + "\n" + NativeCsv.row(1_700_000_000_000,
            TrackPoint(1_700_000_000_000, 56.8, 60.6, 200.0, 5f, 10f, true), null, 0.0, true) + "\n")
        val extras = android.os.Bundle().apply { putString("uri", uri.toString()); putString("file_name", name); putString("title", "Native ride") }
        val controller = Robolectric.buildActivity(MapActivity::class.java, Intent(base, MapActivity::class.java).putExtras(extras)).setup().visible()
        try {
            val activity = controller.get()
            val parsed = ReflectionHelpers.callInstanceMethod<TripData>(activity, "parseFile", ReflectionHelpers.ClassParameter.from(android.os.Bundle::class.java, extras))
            assertEquals("Native viewer error: ${parsed.errorMessage}; $parsed", 1, parsed.routePoints.size)
            assertEquals(listOf(activity.getString(R.string.speed)), parsed.stats1.map { it.label })
            assertTrue("GPS-only files must not create artificial wheel series", parsed.stats2.isEmpty())
            ViewModelProvider(activity)[MapViewModel::class.java].selectItem(parsed)
            val tabs = activity.findViewById<com.google.android.material.tabs.TabLayout>(R.id.tabs)
            assertEquals(listOf(R.string.map_map_tab_name, R.string.map_statistics_tab_name, R.string.map_values_tab_name).map(base::getString), (0..2).map { tabs.getTabAt(it)!!.text.toString() })
            assertEquals(activity.tripTextColor(), tabs.tabTextColors!!.getColorForState(intArrayOf(android.R.attr.state_selected), 0))
        } finally { controller.pause().stop().destroy() }
    }
    @Test fun `trip text follows settings menu while Values distance is displayed in kilometres`() {
        ThemeManager.theme = R.style.InfernoTheme
        assertEquals(base.getColor(R.color.inferno_battery), base.tripTextColor())
        ThemeManager.theme = R.style.ContrastTheme
        assertEquals(base.getColor(R.color.contrast_accent), base.tripTextColor())
        assertEquals("5.28 км", formatTripDistanceKm(5_280).replace(',', '.'))
    }

    @Test @Config(sdk = [35]) fun `route controls reserve safe space above navigation and rounded display edges`() {
        val panel = TrackMapPanel(ContextThemeWrapper(base, R.style.InfernoTheme))
        try {
            panel.showTrip(trip())
            val insets = androidx.core.view.WindowInsetsCompat.Builder()
                .setInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars(), androidx.core.graphics.Insets.of(10, 0, 12, 30)).build()
            androidx.core.view.ViewCompat.dispatchApplyWindowInsets(panel, insets)
            val column = panel.timeline.parent as ViewGroup
            assertTrue(column.paddingBottom >= base.trackDp(24) + 30)
            assertTrue(column.paddingLeft >= base.trackDp(16) + 10)
            assertTrue(column.paddingRight >= base.trackDp(16) + 12)
        } finally { panel.dispose() }
    }
    @Test fun `thumbnail always draws a red line in every palette regardless of full map coloring`() {
        class Capture : android.graphics.Canvas() {
            val lines = mutableListOf<Int>()
            override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, paint: android.graphics.Paint) { lines.add(paint.color) }
        }
        val first = TrackPoint(1_000, 56.8, 60.6, null, 5f, 1f, true)
        val route = listOf(first, first.copy(time = 2_000, latitude = 56.801, segmentStart = false))
        for (style in listOf(R.style.OriginalTheme, R.style.AJDMTheme, R.style.ContrastTheme, R.style.InfernoTheme,
                R.style.PorcelainTheme, R.style.PolarTheme, R.style.JadeTheme, R.style.SteelTheme, R.style.HeatherTheme)) {
            ThemeManager.theme = style
            for (mode in TrackColorMode.entries) {
                TrackAppearance(base).mode = mode
                val view = TrackThumbnailView(ContextThemeWrapper(base, style))
                ReflectionHelpers.setField(view, "points", TrackProjection.project(route))
                view.layout(0, 0, 160, 160)
                val canvas = Capture()
                view.draw(canvas)
                assertEquals(listOf(THUMBNAIL_ROUTE_COLOR), canvas.lines)
            }
        }
    }
}
