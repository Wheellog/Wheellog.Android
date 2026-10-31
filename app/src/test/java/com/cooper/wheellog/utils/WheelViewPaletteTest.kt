package com.cooper.wheellog.utils

import android.app.Application
import android.content.Context
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.R
import com.cooper.wheellog.views.WheelView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29])
class WheelViewPaletteTest {
    private data class TextPaint(val color: Int, val style: Paint.Style, val textSize: Float, val strokeWidth: Float)
    private class TextCanvas : Canvas() {
        val paints = mutableListOf<TextPaint>()
        override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
            paints.add(TextPaint(paint.color, paint.style, paint.textSize, paint.strokeWidth))
        }
    }

    @Test fun `Inferno labels have a black outline while preserving normal and warning text colors`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = AppConfig(context)
        startKoin { modules(module { single { config } }) }
        try {
            val method = WheelView::class.java.getDeclaredMethod("drawGaugeLabel", Canvas::class.java,
                String::class.java, Float::class.javaPrimitiveType, Float::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
            method.isAccessible = true
            for (style in listOf(R.style.InfernoTheme, R.style.ContrastTheme, R.style.OriginalTheme)) {
                ThemeManager.theme = style
                val view = WheelView(ContextThemeWrapper(context, style), null)
                val sizeField = WheelView::class.java.getDeclaredField("innerArcTextSize").apply { isAccessible = true }
                sizeField.setFloat(view, 20f)
                val source = WheelView::class.java.getDeclaredField("textPaint").apply { isAccessible = true }.get(view) as Paint
                source.textSize = 20f
                source.style = Paint.Style.FILL
                for (color in listOf(Color.parseColor("#FF3557"), Color.RED)) {
                    source.color = color
                    for (mode in listOf(false, true)) {
                        val canvas = TextCanvas()
                        method.invoke(view, canvas, if (mode) "true" else "96%", 50f, 50f, mode)
                        if (style == R.style.InfernoTheme) {
                            assertEquals(2, canvas.paints.size)
                            assertEquals(Color.BLACK, canvas.paints[0].color)
                            assertEquals(Paint.Style.STROKE, canvas.paints[0].style)
                            assertTrue(canvas.paints[0].strokeWidth > 0f)
                            assertEquals(if (mode) 23f else 30f, canvas.paints.last().textSize, 0.01f)
                        } else {
                            assertEquals(1, canvas.paints.size)
                            assertEquals(20f, canvas.paints.last().textSize, 0.01f)
                        }
                        assertEquals(color, canvas.paints.last().color)
                        assertEquals(Paint.Style.FILL, canvas.paints.last().style)
                        assertEquals(20f, source.textSize, 0.01f)
                    }
                }
            }
        } finally {
            ThemeManager.theme = R.style.OriginalTheme
            stopKoin()
        }
    }

    @Test fun `both palettes preserve real PWM colors and Original geometry`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = AppConfig(context)
        config.colorPwmStart = 78
        config.colorPwmEnd = 92
        startKoin { modules(module { single { config } }) }
        try {
            for (style in listOf(R.style.ContrastTheme, R.style.InfernoTheme)) {
                ThemeManager.theme = style
                assertEquals(R.style.OriginalTheme, ThemeManager.dialTheme)
                val view = WheelView(ContextThemeWrapper(context, style), null)
                assertEquals(0xAAFFFFFF.toInt(), view.getPwmColor(77))
                assertEquals(0xAAFFFFAA.toInt(), view.getPwmColor(78))
                assertEquals(Color.RED, view.getPwmColor(92))
                assertEquals(Color.RED, view.getPwmColor(100))
            }
        } finally {
            ThemeManager.theme = R.style.OriginalTheme
            stopKoin()
        }
    }
    @Test fun `dashboard keeps selection when activity language differs from application preferences`() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        fun localized(language: String) = base.createConfigurationContext(android.content.res.Configuration(base.resources.configuration).apply {
            setLocale(java.util.Locale.forLanguageTag(language))
        })
        val config = AppConfig(localized("ru"))
        val previous = config.viewBlockIds
        val selected = listOf("voltage", "temperature2", "pwm", "distance")
        config.viewBlockIds = selected
        startKoin { modules(module { single { config } }) }
        try {
            ThemeManager.theme = R.style.OriginalTheme
            for (language in listOf("en", "ru", "en")) {
                val context = ContextThemeWrapper(localized(language), R.style.OriginalTheme)
                val view = WheelView(context, null)
                view.updateViewBlocksVisibility()
                val field = WheelView::class.java.getDeclaredField("mViewBlocks").apply { isAccessible = true }
                val blocks = (field.get(view) as Array<*>).filterIsInstance<com.cooper.wheellog.views.ViewBlockInfo>()
                    .filter { it.enabled }.sortedBy { it.index }
                assertEquals(selected, blocks.map { it.key })
                assertEquals(context.getString(R.string.voltage), blocks.first().title)
                assertEquals(selected, config.viewBlockIds)
            }
        } finally {
            config.viewBlockIds = previous
            ThemeManager.theme = R.style.OriginalTheme
            stopKoin()
        }
    }

}
