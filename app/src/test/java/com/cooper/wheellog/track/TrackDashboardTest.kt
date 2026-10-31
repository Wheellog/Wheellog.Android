package com.cooper.wheellog.track

import android.app.Application
import android.content.*
import androidx.test.core.app.ApplicationProvider
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.cooper.wheellog.*
import com.cooper.wheellog.DashboardBluetoothBinding
import io.mockk.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29])
class TrackDashboardTest {
    private lateinit var context: Context
    private lateinit var config: AppConfig
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        config = AppConfig(context)
        startKoin { modules(module { single { config } }) }
    }
    @After fun cleanup() { DashboardBluetoothBinding.release(context); unmockkAll(); stopKoin() }
    @Test fun `default pages match upstream with optional graph events and BMS`() {
        for (graph in listOf(false, true)) for (events in listOf(false, true)) {
            config.pageGraph = graph
            config.pageEvents = events
            val pages = DashboardPages.visible(config.dashboardPageOrder, config.dashboardVisiblePages, false).toMutableList()
            val activity = mockk<MainActivity>(relaxed = true)
            every { activity.pager } returns ViewPager2(context)
            val adapter = MainPageAdapter(pages, activity)
            val original = mutableListOf(R.layout.main_view_main, R.layout.main_view_params_list).apply {
                if (graph) add(R.layout.main_view_graph)
                add(R.layout.main_view_trips)
                if (events) add(R.layout.main_view_events)
            }
            assertEquals(original, pages)
            adapter.addPage(R.layout.main_view_smart_bms)
            assertEquals(original.toMutableList().apply { add(2, R.layout.main_view_smart_bms) }, pages)
            adapter.removePage(R.layout.main_view_smart_bms)
            assertEquals(original, pages)
        }
    }
    @Test fun `application and existing launcher aliases use original WheelLog artwork`() {
        assertEquals(R.mipmap.ic_launcher, context.applicationInfo.icon)
        for ((alias, icon) in listOf("OriginalIconAlias" to R.mipmap.ic_launcher, "AjdmIconAlias" to R.drawable.ajdm_notification_icon)) {
            val component = ComponentName(context, "com.cooper.wheellog.$alias")
            val info = context.packageManager.getActivityInfo(component, android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS)
            assertEquals(icon, info.iconResource)
        }
    }
    @Test fun `existing upstream hidden pages seed visibility without re enabling trips`() {
        config.pageTrips = false
        config.pageGraph = false
        config.pageEvents = false
        assertEquals(listOf(R.layout.main_view_main, R.layout.main_view_params_list),
            DashboardPages.visible(config.dashboardPageOrder, config.dashboardVisiblePages, false))
        config.dashboardVisiblePages = setOf("main", "values", "trips")
        assertEquals(listOf(R.layout.main_view_main, R.layout.main_view_params_list, R.layout.main_view_trips),
            DashboardPages.visible(config.dashboardPageOrder, config.dashboardVisiblePages, false))
    }
    @Test fun `main stays first and visible while all other screens can be disabled`() {
        config.dashboardPageOrder = listOf("graph", "main", "trips", "graph", "bogus", "bms")
        config.dashboardVisiblePages = emptySet()
        val saved = AppConfig(context)
        assertEquals("main", saved.dashboardPageOrder.first())
        assertEquals(6, saved.dashboardPageOrder.size)
        assertEquals(listOf(R.layout.main_view_main), DashboardPages.visible(saved.dashboardPageOrder, saved.dashboardVisiblePages, true))
    }
    @Test fun `reordered and hidden screens survive BMS availability changes`() {
        config.dashboardPageOrder = listOf("values", "bms", "trips", "graph", "events")
        config.dashboardVisiblePages = setOf("values", "trips", "bms")
        val activity = mockk<MainActivity>(relaxed = true)
        every { activity.pager } returns ViewPager2(context)
        val pages = DashboardPages.visible(config.dashboardPageOrder, config.dashboardVisiblePages, false).toMutableList()
        val adapter = MainPageAdapter(pages, activity)
        adapter.addPage(R.layout.main_view_smart_bms)
        assertEquals(listOf(R.layout.main_view_main, R.layout.main_view_params_list, R.layout.main_view_smart_bms, R.layout.main_view_trips), pages)
        config.dashboardVisiblePages = setOf("values", "trips")
        adapter.removePage(R.layout.main_view_smart_bms)
        adapter.addPage(R.layout.main_view_smart_bms)
        assertEquals(-1, adapter.pageIndex(R.layout.main_view_smart_bms))
        assertEquals(listOf("main", "values", "bms", "trips", "graph", "events"), AppConfig(context).dashboardPageOrder)
    }
    @Test fun `record visibility migration runs once and respects later hiding`() {
        config.mainMenuButtons = arrayOf("watch", "light")
        config.migrateRecordButtonVisibility()
        assertEquals(setOf("watch", "light", "record"), config.mainMenuButtons.toSet())
        config.mainMenuButtons = arrayOf("watch")
        AppConfig(context).migrateRecordButtonVisibility()
        assertEquals(listOf("watch"), config.mainMenuButtons.toList())
    }
    @Test fun `theme recreation reuses application BLE binding until a real exit`() {
        mockkStatic(WheelData::class)
        every { WheelData.getInstance() } returns mockk<WheelData>(relaxed = true)
        val application = mockk<Context>(relaxed = true)
        every { application.applicationContext } returns application
        every { application.bindService(any(), any(), any<Int>()) } returns true
        val first = mockk<Context>(); every { first.applicationContext } returns application
        val recreated = mockk<Context>(); every { recreated.applicationContext } returns application
        DashboardBluetoothBinding.ensure(first, "AA")
        DashboardBluetoothBinding.ensure(recreated, "AA")
        verify(exactly = 1) { application.bindService(any(), any(), Context.BIND_AUTO_CREATE) }
        DashboardBluetoothBinding.release(recreated)
        verify(exactly = 1) { application.unbindService(any()) }
    }
}
