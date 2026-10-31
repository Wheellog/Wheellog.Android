package com.cooper.wheellog.track

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.Typeface
import android.net.Uri
import android.os.SystemClock
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.R
import com.cooper.wheellog.data.TripDao
import com.cooper.wheellog.data.TripDataDbEntry
import com.cooper.wheellog.databinding.ListTripItemBinding
import com.cooper.wheellog.utils.ThemeManager
import com.cooper.wheellog.views.TripAdapter
import com.cooper.wheellog.views.TripModel
import io.mockk.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29])
@LooperMode(LooperMode.Mode.PAUSED)
class TripCardTitleTest {
    private lateinit var base: Context
    private lateinit var config: AppConfig
    private lateinit var dao: TripDao
    private lateinit var originalLocale: Locale

    @Before fun setup() {
        base = ApplicationProvider.getApplicationContext()
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("ru"))
        SystemClock.setCurrentTimeMillis(System.currentTimeMillis())
        config = AppConfig(base)
        dao = mockk(relaxed = true)
        base.getSharedPreferences("trip_titles", Context.MODE_PRIVATE).edit().clear().commit()
        startKoin { modules(module { single { config }; single { dao } }) }
        ThemeManager.theme = R.style.OriginalTheme
    }
    @After fun cleanup() {
        Locale.setDefault(originalLocale)
        ThemeManager.theme = R.style.OriginalTheme
        unmockkAll(); stopKoin()
    }
    private fun context(language: String): Context = ContextThemeWrapper(base.createConfigurationContext(
        Configuration(base.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }), R.style.OriginalTheme)

    private fun date(daysAgo: Int): Date = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -daysAgo)
        set(Calendar.HOUR_OF_DAY, 7); set(Calendar.MINUTE, 45); set(Calendar.SECOND, 26)
        set(Calendar.MILLISECOND, 0)
    }.time
    private fun file(date: Date, millis: Boolean = false) = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US).format(date) +
        if (millis) "_484.csv" else ".csv"

    private fun bind(context: Context, file: String): Pair<String, TripModel> {
        val lookedUp = CountDownLatch(1)
        val record = TripDataDbEntry(fileName = file, duration = 10, additionalJson = "{\"tripStatsVersion\":${com.cooper.wheellog.data.TripParser.STATS_VERSION}}")
        every { dao.getTripByFileName(file) } answers { lookedUp.countDown(); record }
        val binding = ListTripItemBinding.inflate(LayoutInflater.from(context))
        val holder = TripAdapter.ViewHolder(binding, Typeface.DEFAULT)
        val model = TripModel(file, "", Uri.parse("content://trip-title-test/$file"))
        holder.bind(model, View.GONE, mockk(relaxed = true))
        assertTrue("background lookup did not finish", lookedUp.await(5, TimeUnit.SECONDS))
        return binding.name.text.toString() to model
    }

    @Test fun `binding restores weekday and time for CSV names in either language`() {
        val whenRecorded = date(1)
        for (language in listOf("ru", "en")) {
            val name = file(whenRecorded)
            val (title, model) = bind(context(language), name)
            assertEquals(SimpleDateFormat("EEEE, HH:mm", Locale.forLanguageTag(language)).format(whenRecorded), title)
            assertEquals(name, model.fileName)
            assertEquals(name, model.title)
        }
    }
    @Test fun `millisecond recording filenames have the same friendly title`() {
        val whenRecorded = date(1)
        assertEquals(bind(context("ru"), file(whenRecorded)).first,
            bind(context("ru"), file(whenRecorded, millis = true)).first)
    }
    @Test fun `today label and weekday use the visible language`() {
        val whenRecorded = date(0)
        for (language in listOf("ru", "en")) {
            val context = context(language)
            val expected = context.getString(R.string.today) + SimpleDateFormat(", EEE, HH:mm", Locale.forLanguageTag(language)).format(whenRecorded)
            assertEquals(expected, bind(context, file(whenRecorded)).first)
        }
    }
    @Test fun `saved custom titles override automatic dates even when they resemble filenames`() {
        val name = file(date(1))
        for (custom in listOf("Поездка в парк", "2025_01_01_09_30_00.csv")) {
            base.getSharedPreferences("trip_titles", Context.MODE_PRIVATE).edit().putString(name, custom).commit()
            val (title, model) = bind(context("ru"), name)
            assertEquals(custom, title)
            assertEquals(name, model.fileName)
        }
        assertEquals("Weekend ride.csv", bind(context("en"), "Weekend ride.csv").first)
    }
    @Test fun `older dates use activity locale rather than application default locale`() {
        val whenRecorded = Calendar.getInstance().apply { add(Calendar.YEAR, -1) }.time
        val title = bind(context("en"), file(whenRecorded)).first
        assertTrue(title.contains(SimpleDateFormat("MMMM", Locale.ENGLISH).format(whenRecorded)))
        assertFalse(title.endsWith(".csv"))
    }

    @Test fun `tile shows dashes for unavailable wheel telemetry while preserving GPS readings and real zeros`() {
        val binding = ListTripItemBinding.inflate(LayoutInflater.from(context("en")))
        val holder = TripAdapter.ViewHolder(binding, Typeface.DEFAULT)
        val record = TripDataDbEntry(fileName = "gps-only.csv", maxSpeed = 77f, maxSpeedGps = 72f,
            maxPwm = 0f, distance = 1000, consumptionTotal = 0f, consumptionByKm = 0f,
            additionalJson = "{\"hasWheelSpeed\":false,\"hasPwm\":false,\"hasEnergy\":false,\"hasGpsSpeed\":true}")
        org.robolectric.util.ReflectionHelpers.callInstanceMethod<Unit>(holder, "setDescFromDb",
            org.robolectric.util.ReflectionHelpers.ClassParameter.from(TripDataDbEntry::class.java, record))
        for (metric in listOf(binding.metricWheelSpeed, binding.metricPwm, binding.metricEnergy, binding.metricConsumption))
            assertTrue(metric.contentDescription.toString().endsWith(": —"))
        assertTrue(binding.metricGpsSpeed.contentDescription.toString().contains("72"))
        record.additionalJson = "{\"hasWheelSpeed\":true,\"hasPwm\":true,\"hasEnergy\":true}"
        org.robolectric.util.ReflectionHelpers.callInstanceMethod<Unit>(holder, "setDescFromDb",
            org.robolectric.util.ReflectionHelpers.ClassParameter.from(TripDataDbEntry::class.java, record))
        assertFalse(binding.metricPwm.contentDescription.toString().contains("—"))
        assertTrue(binding.metricPwm.contentDescription.toString().contains("0"))
    }
}
