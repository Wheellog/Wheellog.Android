package com.cooper.wheellog.track

import android.app.Application
import android.content.Context
import android.view.ContextThemeWrapper
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.cooper.wheellog.*
import com.cooper.wheellog.utils.ThemeManager
import com.cooper.wheellog.views.ViewBlockInfo
import com.cooper.wheellog.views.WheelView
import io.mockk.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29])
class DashboardSpeedCorrectionTest {
    @Test fun `actual dashboard tiles update with wheel correction preferences without a new wheel packet`() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        PreferenceManager.getDefaultSharedPreferences(base).edit().clear().commit()
        val config = AppConfig(base)
        config.lastMac = "AA:BB:CC:DD:EE:FF"
        config.useMph = false
        config.viewBlockIds = listOf("speed", "top_speed", "average_riding_speed", "average_speed", "distance")
        startKoin { modules(module { single { config } }) }
        val previousTheme = ThemeManager.theme
        ThemeManager.theme = R.style.InfernoTheme
        var adapter: MainPageAdapter? = null
        var recycler: RecyclerView? = null
        try {
            val wheel = WheelData()
            wheel.speed = 7_630
            wheel.setTopSpeed(7_630)
            ReflectionHelpers.setField(wheel, "mTotalDistance", 105_000L)
            ReflectionHelpers.setField(wheel, "mStartTotalDistance", 100_000L)
            ReflectionHelpers.setField(wheel, "mRidingTime", 600)
            ReflectionHelpers.setField(wheel, "mRideTime", 900)
            mockkStatic(WheelData::class)
            every { WheelData.getInstance() } returns wheel
            val context = ContextThemeWrapper(base, R.style.InfernoTheme)
            val view = WheelView(context, null)
            val activity = mockk<MainActivity>(relaxed = true)
            adapter = MainPageAdapter(mutableListOf(R.layout.main_view_main), activity).apply {
                position = 0
                wheelView = view
            }
            recycler = RecyclerView(context)
            adapter.onAttachedToRecyclerView(recycler)
            adapter.updateScreen(false)
            fun tile(key: String): String = ReflectionHelpers.getField<Array<ViewBlockInfo>>(view, "mViewBlocks")
                .single { it.key == key }.getValue()
            assertTrue(tile("top_speed").startsWith("76.3 "))
            assertTrue(tile("average_riding_speed").startsWith("30.0 "))
            assertTrue(tile("average_speed").startsWith("20.0 "))

            config.speedCorrectionPercent = -6.0
            config.useSpeedCorrection = true
            assertTrue(tile("speed").startsWith("71.7 "))
            assertTrue(tile("top_speed").startsWith("71.7 "))
            assertTrue(tile("average_riding_speed").startsWith("28.2 "))
            assertTrue(tile("average_speed").startsWith("18.8 "))
            assertEquals(717, ReflectionHelpers.getField<Int>(view, "mSpeed"))
            assertEquals(76.3, wheel.rawSpeedDouble, .00001)

            config.speedCorrectionPercent = -6.5
            assertTrue(tile("speed").startsWith("71.3 "))
            assertTrue(tile("top_speed").startsWith("71.3 "))
            assertEquals(28.05, wheel.averageRidingSpeedDouble, .00001)
            assertEquals(76.3, wheel.rawSpeedDouble, .00001)

            config.speedCorrectionPercent = 10.0
            assertTrue(tile("top_speed").startsWith("83.9 "))
            assertTrue(tile("average_riding_speed").startsWith("33.0 "))
            config.useSpeedCorrection = false
            assertTrue(tile("top_speed").startsWith("76.3 "))
            assertTrue(tile("average_riding_speed").startsWith("30.0 "))
            wheel.setRecordingDistanceStart(95_000L)
            adapter.updateScreen(false)
            assertTrue(tile("distance").startsWith("10.00 "))
            config.speedCorrectionPercent = -6.0
            config.useSpeedCorrection = true
            assertTrue(tile("distance").startsWith("10.00 "))
            assertTrue(tile("average_riding_speed").startsWith("28.2 "))
        } finally {
            if (adapter != null && recycler != null) adapter.onDetachedFromRecyclerView(recycler)
            ThemeManager.theme = previousTheme
            unmockkAll()
            stopKoin()
        }
    }
}
