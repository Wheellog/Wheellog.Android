package com.cooper.wheellog.voice

import android.app.Application
import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.cooper.wheellog.AppConfig
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29])
class VoiceCompatibilityTest {
    private lateinit var context: Context
    private val sample = VoiceSample("wheel", 0, 40.0, 80, 12.5, 100.5, 12.0, 1200.0, 45, 55, 72.0)
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("voice_announcements_v1", Context.MODE_PRIVATE).edit().clear().commit()
    }
    @Test fun `voice preferences preserve build149 preferences and existing trip bytes`() {
        val legacy = PreferenceManager.getDefaultSharedPreferences(context)
        AppConfig(context)
        legacy.edit().putString("last_mac", "AA:BB:CC:DD:EE:FF").putInt("speed_correction_half_steps_AA:BB:CC:DD:EE:FF", -13)
            .putString("view_blocks_ids", "distance;battery;pwm").commit()
        val before = legacy.all.toMap()
        val trip = File(context.filesDir, "existing149trip.csv")
        val bytes = "date,time,speed,totaldistance\n2026-10-28,12:00:00,25,100000\n".toByteArray()
        trip.writeBytes(bytes)
        VoiceSettings(context).apply {
            enabled = true; reports = true; intervalSeconds = 45; setVoice("ru", "ru-voice"); setVoice("en", "en-voice")
        }
        assertEquals(before, legacy.all)
        assertArrayEquals(bytes, trip.readBytes())
        // Reopening the original AppConfig still reads the same settings without migration.
        AppConfig(context)
        assertEquals(before, legacy.all)
    }
    @Test fun `Russian and English voices persist independently without a speech language setting`() {
        VoiceSettings(context).apply { setVoice("ru", "russian2"); setVoice("en", "english3"); enabled = true }
        val reopened = VoiceSettings(context)
        assertEquals("russian2", reopened.voice("ru")); assertEquals("english3", reopened.voice("en"))
        assertTrue(reopened.enabled)
        assertEquals("ru", VoiceText.language("ru")); assertEquals("en", VoiceText.language("en"))
        assertEquals("en", VoiceText.language("de"))
        assertFalse(context.getSharedPreferences("voice_announcements_v1", 0).contains("language"))
    }
    @Test fun `reports use the app locale selected fields and existing distance without correction`() {
        val ru = VoiceText.report(VoiceText.localized(context,"ru"), sample, setOf("speed","distance","battery"), false)
        val en = VoiceText.report(VoiceText.localized(context,"en"), sample, setOf("speed","distance","battery"), false)
        assertTrue(ru.contains("Скорость")); assertTrue(ru.contains("12,5")); assertFalse(ru.contains("Напряжение"))
        assertTrue(en.contains("Speed")); assertTrue(en.contains("12.5")); assertTrue(en.contains("80"))
        assertTrue(VoiceText.report(VoiceText.localized(context,"en"), sample, setOf("speed"), true).contains("25 miles"))
    }
    @Test fun `unavailable and nonfinite values are omitted instead of spoken as numbers`() {
        val invalid = sample.copy(voltage=0.0, speedKmh=Double.NaN, current=Double.POSITIVE_INFINITY, motorTemperature=0)
        assertEquals("", VoiceText.report(context, invalid, setOf("speed","current","voltage","motor_temperature"), false))
    }
    @Test fun `voice defaults and bounds do not modify main preferences`() {
        val prefs = VoiceSettings(context)
        assertFalse(prefs.enabled)
        assertEquals(setOf("speed","distance","battery"),prefs.fields)
        prefs.intervalSeconds=0; prefs.alarmRepeatSeconds=0; prefs.volumePercent=1000
        assertEquals(15,prefs.intervalSeconds); assertEquals(5,prefs.alarmRepeatSeconds); assertEquals(100,prefs.volumePercent)
    }
    @Test fun `distance origins and targets survive restart and remain separate for each wheel`() {
        val prefs = VoiceSettings(context)
        assertTrue(prefs.timedReports)
        assertFalse(prefs.distanceReports("a").periodic)
        prefs.initializeDistance("a", 10359070, true)
        assertEquals(10359070L, prefs.distanceReports("a").targetMetres)
        prefs.setTargetMetres("a", 10400001)
        prefs.setTargetDistance("a", true)
        prefs.setAfterDistance("a", true, 10359070)
        val reopened = VoiceSettings(context)
        reopened.initializeDistance("a", 10360000, true)
        assertEquals(10400001L, reopened.distanceReports("A").targetMetres)
        assertEquals(10359070L, reopened.distanceReports("a").originMetres)
        assertNull(reopened.distanceReports("b").targetMetres)
        assertFalse(reopened.distanceReports("b").after)
        reopened.finishDistanceReports("a", after = true, target = true)
        assertFalse(reopened.distanceReports("a").after || reopened.distanceReports("a").target)
        assertEquals(10400001L, reopened.distanceReports("a").targetMetres)
    }
    @Test fun `enabling a relative report offline waits for the first real odometer`() {
        val prefs = VoiceSettings(context)
        prefs.setAfterDistance("a", true, null)
        prefs.initializeDistance("a", 0, true)
        assertNull(prefs.distanceReports("a").originMetres)
        prefs.initializeDistance("a", 100123, true)
        prefs.initializeDistance("a", 100500, false)
        assertEquals(100123L, prefs.distanceReports("a").originMetres)
        prefs.setAfterDistance("a", false, 100500)
        prefs.setAfterDistance("a", true, 100600)
        assertEquals(100600L, prefs.distanceReports("a").originMetres)
    }
    @Test fun `average speeds and ride time are grouped and localized in the report`() {
        val data = sample.copy(averageMovingSpeedKmh = 30.5, averageSpeedKmh = 20.5, rideTime = "01:22:05")
        val fields = setOf("speed", "average_moving_speed", "average_speed", "distance", "ride_time", "battery")
        val ru = VoiceText.report(VoiceText.localized(context, "ru"), data, fields, false)
        assertTrue(ru.contains("Средняя скорость движения: 30,5"))
        assertTrue(ru.contains("Средняя скорость: 20,5"))
        assertTrue(ru.contains("1 час 22 минуты 5 секунд"))
        assertTrue(ru.indexOf("Скорость:") < ru.indexOf("Средняя скорость движения:"))
        assertTrue(ru.indexOf("Средняя скорость:") < ru.indexOf("Расстояние:"))
        assertTrue(ru.indexOf("Время поездки:") < ru.indexOf("Заряд:"))
        val en = VoiceText.report(VoiceText.localized(context, "en"), data, fields, true)
        assertTrue(en.contains("Average moving speed: 19 miles"))
        assertTrue(en.contains("1 hour 22 minutes 5 seconds"))
    }
}
