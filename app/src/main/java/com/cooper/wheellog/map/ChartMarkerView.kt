package com.cooper.wheellog.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import com.cooper.wheellog.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import java.text.DecimalFormat

@SuppressLint("ViewConstructor")
class ChartMarkerView(context: Context, private val valueFormatter: ValueFormatter, private val stats: List<LineDataSet>)
    : MarkerView(context, R.layout.chart_makerview) {
    private val tvData: TextView = findViewById(R.id.textView_Data)
    private val tvTitle: TextView = findViewById(R.id.textView_Title)
    init {
        val foreground = context.statsMarkerTextColor()
        background = GradientDrawable().apply {
            setColor(context.statsMarkerBackgroundColor())
            cornerRadius = 6 * resources.displayMetrics.density
            setStroke(resources.displayMetrics.density.toInt().coerceAtLeast(1), foreground)
        }
        listOf(tvData, tvTitle).forEach {
            it.setTextColor(foreground)
            it.textSize = 14f
            it.maxWidth = (resources.displayMetrics.widthPixels - 48 * resources.displayMetrics.density).toInt().coerceAtLeast(100)
        }
    }
    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e != null) {
            tvTitle.text = valueFormatter.getFormattedValue(e.x)
            val text = SpannableStringBuilder()
            val format = DecimalFormat("0.##")
            stats.groupBy { it.label }.values.forEachIndexed { index, segments ->
                val dataSet = segments.first()
                if (index > 0) text.append('\n')
                val start = text.length
                text.append("■ ")
                // The swatch is independently contrast-correct on the marker surface.
                text.setSpan(ForegroundColorSpan(statsHueColor(dataSet.color, context.statsMarkerBackgroundColor())),
                    start, start + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val value = segments.asSequence().flatMap { it.getEntriesForXValue(e.x).asSequence() }
                    .firstOrNull()?.y?.takeIf { it.isFinite() }
                text.append(dataSet.label).append(": ").append(value?.let { format.format(it) } ?: "—")
            }
            tvData.text = text
        }
        super.refreshContent(e, highlight)
    }
    override fun getOffsetForDrawingAtPoint(posX: Float, posY: Float): MPPointF {
        val bounds = chartView
        val left = (posX - width / 2f).coerceIn(0f, ((bounds?.width ?: resources.displayMetrics.widthPixels) - width).coerceAtLeast(0).toFloat())
        val top = if (posY > height + 10) posY - height - 10 else posY + 10
        return MPPointF(left - posX, top.coerceIn(0f, ((bounds?.height ?: resources.displayMetrics.heightPixels) - height).coerceAtLeast(0).toFloat()) - posY)
    }
}
