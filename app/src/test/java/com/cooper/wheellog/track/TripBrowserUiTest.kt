package com.cooper.wheellog.track

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Looper
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.cooper.wheellog.*
import com.cooper.wheellog.data.*
import com.cooper.wheellog.map.*
import com.cooper.wheellog.utils.SomeUtil.getColorEx
import com.cooper.wheellog.utils.ThemeManager
import com.cooper.wheellog.views.*
import io.mockk.mockk
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application=Application::class,sdk=[29])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TripBrowserUiTest {
    private lateinit var base: Context
    @Before fun setup() {
        base = ApplicationProvider.getApplicationContext()
        base.getSharedPreferences("trip_list",Context.MODE_PRIVATE).edit().clear().commit()
        base.getSharedPreferences("trip_titles",Context.MODE_PRIVATE).edit().clear().commit()
        val config = AppConfig(base)
        val dao = mockk<TripDao>(relaxed=true)
        startKoin { modules(module { single { config }; single { dao } }) }
    }
    @After fun finish() { ThemeManager.theme=R.style.OriginalTheme; stopKoin() }
    @Test fun searchFiltersOnEachCharacterAndClearingItKeepsTheDateFilter() {
        val context = ContextThemeWrapper(base,R.style.InfernoTheme)
        ThemeManager.theme = R.style.InfernoTheme
        base.getSharedPreferences("trip_titles",Context.MODE_PRIVATE).edit()
            .putString("2026_10_29_07_45_00.csv","Поездка толпой в Чебаркуль в пятницу").commit()
        val models = arrayListOf(
            TripModel("2026_10_29_07_45_00.csv","",Uri.parse("file:///one.csv")),
            TripModel("2026_10_29_08_45_00.csv","",Uri.parse("file:///two.csv")))
        val root = LayoutInflater.from(context).inflate(R.layout.main_view_trips,null)
        val adapter = TripAdapter(context,models)
        val recycler = root.findViewById<RecyclerView>(R.id.list_trips)
        recycler.adapter = adapter
        try {
            TripBrowserControls(context,root,adapter)
            val day = TripDay(2026,10,29)
            adapter.setQuery(adapter.query.copy(period=TripPeriod(day,day)))
            root.findViewById<EditText>(R.id.trip_search).setText("чебар")
            assertEquals(1,adapter.itemCount)
            root.findViewById<ImageButton>(R.id.trip_clear_search).performClick()
            assertEquals(2,adapter.itemCount)
            assertNotNull(adapter.query.period)
            root.findViewById<View>(R.id.trip_filter_status).performClick()
            assertNull(adapter.query.period)
        } finally { recycler.adapter=null }
    }
    @Test fun selectedQueryAndSortAreRestoredWithoutTouchingOtherAppPreferences() {
        val prefs = base.getSharedPreferences("trip_list",Context.MODE_PRIVATE)
        val query = TripListQuery("Чебар",TripPeriod.month(2026,10),TripSort.GPS_SPEED,false)
        TripListPreferences(prefs).save(query)
        assertEquals(query,TripListPreferences(prefs).load())
        TripListPreferences(prefs).save(query.copy(name=""))
        assertEquals(query.period,TripListPreferences(prefs).load().period)
    }
    @Test fun dateChoicesAndBothRangeCalendarsApplyTheirSelection() {
        val controller=org.robolectric.Robolectric.buildActivity(android.app.Activity::class.java)
        val activity=controller.get()
        activity.setTheme(R.style.InfernoTheme)
        controller.setup()
        ThemeManager.theme=R.style.InfernoTheme
        val root=LayoutInflater.from(activity).inflate(R.layout.main_view_trips,null)
        activity.setContentView(root)
        val adapter=TripAdapter(activity,arrayListOf())
        root.findViewById<RecyclerView>(R.id.list_trips).adapter=adapter
        try {
            TripBrowserControls(activity,root,adapter)
            val period=TripPeriod(TripDay(2026,10,1),TripDay(2026,10,3))
            for(mode in 1..4) {
                adapter.setQuery(adapter.query.copy(period=period))
                root.findViewById<View>(R.id.trip_date_filter).performClick()
                val menu=org.robolectric.shadows.ShadowDialog.getLatestDialog() as androidx.appcompat.app.AlertDialog
                val option=menu.listView.adapter.getView(mode,null,menu.listView)
                menu.listView.performItemClick(option,mode,mode.toLong())
                shadowOf(Looper.getMainLooper()).idle()
                if(mode==1 || mode==4) {
                    (org.robolectric.shadows.ShadowDialog.getLatestDialog() as android.app.DatePickerDialog)
                        .getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
                    shadowOf(Looper.getMainLooper()).idle()
                    if(mode==4) {
                        (org.robolectric.shadows.ShadowDialog.getLatestDialog() as android.app.DatePickerDialog)
                            .getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
                        shadowOf(Looper.getMainLooper()).idle()
                    }
                } else (org.robolectric.shadows.ShadowDialog.getLatestDialog() as androidx.appcompat.app.AlertDialog)
                    .getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
                shadowOf(Looper.getMainLooper()).idle()
                assertEquals(when(mode) {
                    1 -> TripPeriod(period.first,period.first)
                    2 -> TripPeriod.month(2026,10)
                    3 -> TripPeriod.year(2026)
                    else -> period
                },adapter.query.period)
            }
        } finally { root.findViewById<RecyclerView>(R.id.list_trips).adapter=null; controller.pause().stop().destroy() }
    }
    @Test fun controlsFitNarrowAndWideScreensWithReadableColorsAcrossEveryTheme() {
        val themes=listOf(R.style.OriginalTheme,R.style.AJDMTheme,R.style.ContrastTheme,R.style.InfernoTheme,
            R.style.PorcelainTheme,R.style.PolarTheme,R.style.JadeTheme,R.style.SteelTheme,R.style.HeatherTheme)
        for(theme in themes) for(scale in listOf(1f,1.5f)) for(width in listOf(320,800)) {
            ThemeManager.theme=theme
            val config=Configuration(base.resources.configuration).apply { fontScale=scale }
            val context=ContextThemeWrapper(base.createConfigurationContext(config),theme)
            val root=LayoutInflater.from(context).inflate(R.layout.main_view_trips,null)
            val adapter=TripAdapter(context,arrayListOf())
            val recycler=root.findViewById<RecyclerView>(R.id.list_trips)
            recycler.adapter=adapter
            TripBrowserControls(context,root,adapter)
            root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(640,View.MeasureSpec.EXACTLY))
            root.layout(0,0,width,640)
            val field=root.findViewById<EditText>(R.id.trip_search)
            assertTrue(field.width>0)
            assertTrue(ColorUtils.calculateContrast(field.currentTextColor,context.tripSurfaceColor())>=4.5)
            for(id in listOf(R.id.trip_clear_search,R.id.trip_date_filter,R.id.trip_sort)) {
                val button=root.findViewById<ImageButton>(id)
                assertTrue(button.width>=48)
                assertTrue(button.right<=width)
                assertTrue(ColorUtils.calculateContrast(button.imageTintList!!.defaultColor,context.getColorEx(R.color.background))>=3.0)
            }
            recycler.adapter=null
        }
    }
}
