package com.cooper.wheellog

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29])
class WheelSpeedCorrectionTest {
    private lateinit var context: Context

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        AppConfig(context)
    }

    @Test fun `existing integer percentages remain readable without a preference type mismatch`() {
        val config = AppConfig(context)
        config.lastMac = "AA:BB:01"
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putInt("speed_correction_percent_AA:BB:01", -5)
            .putBoolean("speed_correction_enabled_AA:BB:01", true).commit()
        assertEquals(-5.0, config.appliedSpeedCorrectionPercent, 0.0)
        config.speedCorrectionPercent = -6.5
        assertEquals(-6.5, AppConfig(context).appliedSpeedCorrectionPercent, 0.0)
        assertEquals(-5, prefs.getInt("speed_correction_percent_AA:BB:01", 0))
        assertEquals(-13, prefs.getInt("speed_correction_half_steps_AA:BB:01", 0))
        config.lastMac = "AA:BB:02"
        prefs.edit().putInt("speed_correction_percent_AA:BB:02", -20).commit()
        assertEquals(-10.0, config.speedCorrectionPercent, 0.0)
        prefs.edit().putInt("speed_correction_percent_AA:BB:02", 20).commit()
        assertEquals(10.0, config.speedCorrectionPercent, 0.0)
    }

    @Test fun `fractional settings survive reload language and wheel changes and disabling`() {
        val config = AppConfig(context)
        config.lastMac = "AA:BB:01"
        config.speedCorrectionPercent = -6.5
        config.useSpeedCorrection = true
        val russian = AppConfig(context.createConfigurationContext(
            Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag("ru")) }))
        assertEquals(-6.5, russian.appliedSpeedCorrectionPercent, 0.0)
        russian.lastMac = "AA:BB:02"
        assertEquals(0.0, russian.speedCorrectionPercent, 0.0)
        assertFalse(russian.useSpeedCorrection)
        russian.speedCorrectionPercent = 2.5
        russian.useSpeedCorrection = true
        assertEquals(2.5, AppConfig(context).appliedSpeedCorrectionPercent, 0.0)
        russian.lastMac = "AA:BB:01"
        russian.useSpeedCorrection = false
        assertEquals(0.0, russian.appliedSpeedCorrectionPercent, 0.0)
        assertEquals(-6.5, russian.speedCorrectionPercent, 0.0)
        russian.useSpeedCorrection = true
        assertEquals(-6.5, russian.appliedSpeedCorrectionPercent, 0.0)
    }

    @Test fun `every half percent is persisted exactly and inputs stay within the new bounds`() {
        val config = AppConfig(context)
        config.lastMac = "AA:BB:01"
        config.useSpeedCorrection = true
        for (halfSteps in -20..20) {
            config.speedCorrectionPercent = halfSteps / 2.0
            assertEquals(halfSteps / 2.0, AppConfig(context).appliedSpeedCorrectionPercent, 0.0)
        }
        for ((input, expected) in listOf(-99.0 to -10.0, 99.0 to 10.0,
            -6.37 to -6.5, -6.01 to -6.0, Double.NaN to 0.0)) {
            config.speedCorrectionPercent = input
            assertEquals(expected, config.speedCorrectionPercent, 0.0)
        }
    }

    @Test fun `percentage labels use the interface decimal separator without unnecessary zeroes`() {
        val russian = Locale.forLanguageTag("ru")
        assertEquals("-6,5%", WheelSpeedCorrection.formatPercent(-6.5, russian))
        assertEquals("+0,5%", WheelSpeedCorrection.formatPercent(.5, russian))
        assertEquals("-6.5%", WheelSpeedCorrection.formatPercent(-6.5, Locale.ENGLISH))
        assertEquals("+10%", WheelSpeedCorrection.formatPercent(10.0, Locale.ENGLISH))
        assertEquals("0%", WheelSpeedCorrection.formatPercent(0.0, russian))
    }
}
