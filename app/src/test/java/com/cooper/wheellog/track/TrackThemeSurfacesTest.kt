package com.cooper.wheellog.track

import android.app.Application
import android.content.*
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.*
import android.view.*
import android.widget.*
import androidx.core.graphics.ColorUtils
import androidx.test.core.app.ApplicationProvider
import com.cooper.wheellog.*
import com.cooper.wheellog.data.TripDao
import com.cooper.wheellog.databinding.ActivityMapBinding
import com.cooper.wheellog.map.*
import com.cooper.wheellog.utils.ThemeEnum
import com.cooper.wheellog.utils.ThemeManager
import com.cooper.wheellog.utils.SomeUtil.getColorEx
import com.google.android.material.snackbar.Snackbar
import io.mockk.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29])
class TrackThemeSurfacesTest {
    private lateinit var base: Context
    private lateinit var config: AppConfig
    @Before fun setup() {
        base = ApplicationProvider.getApplicationContext()
        config = AppConfig(base)
        startKoin { modules(module { single { config }; single { mockk<TripDao>(relaxed = true) } }) }
    }
    @After fun cleanup() { ThemeManager.theme = R.style.OriginalTheme; unmockkAll(); stopKoin() }
    private fun contexts(check: (Context, ThemeEnum) -> Unit) {
        for (theme in ThemeEnum.entries) for (night in listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES)) {
            config.appThemeInt = theme.value; ThemeManager.theme = config.appTheme
            val mode = Configuration(base.resources.configuration).apply { uiMode = night }
            check(ContextThemeWrapper(base.createConfigurationContext(mode), config.appTheme), theme)
        }
    }
    @Test fun `actual XML tabs use a readable surface without black backgrounds on individual tabs`() = contexts { context, theme ->
        val tabs = ActivityMapBinding.inflate(LayoutInflater.from(context)).tabs
        for (title in listOf("КАРТА", "СТАТИСТИКА", "ЗНАЧЕНИЯ")) tabs.addTab(tabs.newTab().setText(title))
        styleTripTabs(context, tabs)
        val surface = (tabs.background as ColorDrawable).color
        for (state in listOf(intArrayOf(android.R.attr.state_selected), intArrayOf())) {
            val text = tabs.tabTextColors!!.getColorForState(state, 0)
            assertTrue("tab text ${theme.name}", ColorUtils.calculateContrast(text, surface) >= 4.5)
        }
        // Inspect the XML background actually used to create every TabView. A strip
        // background alone cannot fix an opaque black tab layered above that strip.
        val resource = ReflectionHelpers.getField<Int>(tabs, "tabBackgroundResId")
        assertEquals(Color.TRANSPARENT, context.getColor(resource))
    }
    @Test fun `exit message and its action use contrasting colors on actual Snackbar surface in all themes`() = contexts { context, theme ->
        val parent = FrameLayout(context).apply { id = android.R.id.content }
        val bar = Snackbar.make(parent, R.string.back_to_exit, Snackbar.LENGTH_LONG).setAction(android.R.string.ok) { }
        styleTripMessage(context, bar)
        val surface = (bar.view.background as ColorDrawable).color
        val text = bar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        val action = bar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_action)
        assertTrue("exit text ${theme.name}", ColorUtils.calculateContrast(text.currentTextColor, surface) >= 4.5)
        assertTrue("exit action ${theme.name}", ColorUtils.calculateContrast(action.currentTextColor, surface) >= 4.5)
    }
    @Test fun `small map captions selected point and gradient selector remain readable in all themes`() = contexts { context, theme ->
        val panel = TrackMapPanel(context)
        try {
            val point = TrackPoint(1_000, 56.8, 60.6, null, 5f, 10f, true)
            panel.showTrip(TripData("Тестовая поездка", routePoints = listOf(point), startedAt = 1_000, endedAt = 1_000))
            val column = panel.timeline.parent as ViewGroup
            val background = context.getColorEx(R.color.background)
            for (index in 0 until column.childCount) {
                val label = column.getChildAt(index) as? TextView ?: continue
                assertTrue("map label ${theme.name}: ${label.text}", ColorUtils.calculateContrast(label.currentTextColor, background) >= 4.5)
            }
            val selected = panel.colorSelector.adapter.getView(0, null, panel.colorSelector) as TextView
            assertTrue("selector ${theme.name}", ColorUtils.calculateContrast(selected.currentTextColor, background) >= 4.5)
            assertTrue("arrow ${theme.name}", ColorUtils.calculateContrast(panel.colorSelector.backgroundTintList!!.defaultColor, background) >= 4.5)
            if (theme.value >= 4) {
                val fill = ((panel.play.background as RippleDrawable).getDrawable(0) as GradientDrawable).color!!.defaultColor
                assertTrue("replay button ${theme.name}", ColorUtils.calculateContrast(panel.play.currentTextColor, fill) >= 4.5)
            }
        } finally { panel.dispose() }
    }
    @Test fun `opening a trip does not apply a pending theme before a manual app restart`() {
        ThemeManager.theme = R.style.SteelTheme
        config.appThemeInt = ThemeEnum.Polar.value
        val controller = Robolectric.buildActivity(MapActivity::class.java).create()
        try {
            assertEquals(R.style.SteelTheme, ThemeManager.theme)
            val activity = controller.get()
            val value = android.util.TypedValue()
            assertTrue(activity.theme.resolveAttribute(android.R.attr.colorBackground, value, true))
            assertEquals(activity.getColor(R.color.steel_background), value.data)
            assertEquals(R.style.PolarTheme, AppConfig(base).appTheme)
        } finally { controller.destroy() }
    }

    @Test fun `trip cards use their actual readable popup surface in every day night theme`() = contexts { context, theme ->
        val adapter = com.cooper.wheellog.views.TripAdapter(context, arrayListOf())
        val holder = adapter.onCreateViewHolder(FrameLayout(context), 0)
        val card = com.cooper.wheellog.databinding.ListTripItemBinding.bind(holder.itemView).root
        assertTrue("card text ${theme.name}", ColorUtils.calculateContrast(context.tripTextColor(), card.cardBackgroundColor.defaultColor) >= 4.5)
    }
    @Test fun `native rename and confirmation dialogs have readable OK Cancel and neutral actions in all themes`() = contexts { context, theme ->
        val field = EditText(context).apply { setText("Название поездки") }
        val dialog = androidx.appcompat.app.AlertDialog.Builder(context).setTitle(R.string.track_rename).setView(field)
            .setPositiveButton(android.R.string.ok, null).setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton("Другое", null).create()
        try {
            dialog.show()
            val background = android.util.TypedValue()
            assertTrue(dialog.context.theme.resolveAttribute(android.R.attr.windowBackground, background, true))
            assertTrue("dialog surface must resolve to color", background.type in android.util.TypedValue.TYPE_FIRST_COLOR_INT..android.util.TypedValue.TYPE_LAST_COLOR_INT)
            for (id in listOf(android.content.DialogInterface.BUTTON_POSITIVE, android.content.DialogInterface.BUTTON_NEGATIVE, android.content.DialogInterface.BUTTON_NEUTRAL)) {
                val text = dialog.getButton(id).currentTextColor
                assertTrue("dialog button ${theme.name}: ${Integer.toHexString(text)} on ${Integer.toHexString(background.data)}",
                    ColorUtils.calculateContrast(text, background.data) >= 4.5)
            }
        } finally { dialog.dismiss() }
    }
}
