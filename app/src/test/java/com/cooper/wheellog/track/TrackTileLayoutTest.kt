package com.cooper.wheellog.track

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.cooper.wheellog.R
import com.cooper.wheellog.databinding.ListTripItemBinding
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [29])
class TrackTileLayoutTest {
    @Test fun `trip statistics and accessible menu remain within narrow wide and enlarged text tiles`() {
        val base = ApplicationProvider.getApplicationContext<Context>()
        for (theme in listOf(R.style.OriginalTheme, R.style.ContrastTheme, R.style.InfernoTheme)) {
            for (scale in listOf(1f, 1.5f)) {
                val configured = base.createConfigurationContext(Configuration(base.resources.configuration).apply { fontScale = scale })
                val context = ContextThemeWrapper(configured, theme)
                for (widthDp in listOf(320, 600, 1000)) {
                    val tile = ListTripItemBinding.inflate(LayoutInflater.from(context))
                    tile.name.text = "Длинное пользовательское название вечерней поездки в парк"
                    val metrics = listOf(tile.metricWheelSpeed, tile.metricGpsSpeed, tile.metricAverage, tile.metricPwm,
                        tile.metricDuration, tile.metricDistance, tile.metricEnergy, tile.metricConsumption)
                    metrics.forEach { it.bind("⚡", "724,91", "Втч/км", android.graphics.Color.WHITE,
                        android.graphics.Color.LTGRAY, androidx.core.content.res.ResourcesCompat.getFont(context, R.font.prime)) }
                    val density = context.resources.displayMetrics.density
                    val width = (widthDp * density).toInt()
                    tile.root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                    tile.root.layout(0, 0, tile.root.measuredWidth, tile.root.measuredHeight)
                    for (view in (listOf(tile.name, tile.thumbnail, tile.popupButton, tile.metricGrid) + metrics)) {
                        val owner = view.parent as View
                        assertTrue("${view.id} extends outside width $widthDp at font $scale", view.left >= 0 && view.right <= owner.width)
                        assertTrue("${view.id} extends outside height", view.top >= 0 && view.bottom <= owner.height)
                        if (view is TextView && view.lineCount > 0) {
                            assertTrue("text is vertically clipped", view.layout.getLineBottom(view.lineCount - 1) <= view.height - view.totalPaddingTop - view.totalPaddingBottom)
                        }
                    }
                    assertTrue(tile.popupButton.width >= 48 * density - 1)
                    assertTrue(tile.thumbnail.width > 80 * density)
                    assertEquals("thumbnail must remain square", tile.thumbnail.width, tile.thumbnail.height)
                    assertEquals("metrics start above/below thumbnail", tile.thumbnail.top, tile.metricGrid.top)
                    assertEquals("metrics including unit captions extend below thumbnail", tile.thumbnail.bottom, tile.metricGrid.bottom)
                    assertTrue(tile.thumbnail.clipToOutline)
                    metrics.forEach { metric ->
                        val bounds = metric.renderedTextBounds()
                        assertEquals(3, bounds.size)
                        bounds.forEach { text ->
                            assertTrue("metric text exceeds width at $widthDp and $scale: $text", text.left >= -.1f && text.right <= metric.width + .1f)
                            assertTrue("metric text exceeds row height at $widthDp and $scale: $text", text.top >= -.1f && text.bottom <= metric.height + .1f)
                        }
                        assertEquals("caption does not start below the first digit", bounds[1].left, bounds[2].left, .1f)
                        assertTrue("large icon must span both text rows", bounds[0].height() >= bounds[1].height())
                    }
                    assertNull(tile.root.findViewById<View>(base.resources.getIdentifier("timeWindow", "id", base.packageName)))
                    assertNull(tile.root.findViewById<View>(base.resources.getIdentifier("speedSummary", "id", base.packageName)))
                }
            }
        }
    }
}
