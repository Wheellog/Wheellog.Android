package com.cooper.wheellog.map
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.text.Html
import android.widget.TextView
import com.cooper.wheellog.R
import com.cooper.wheellog.utils.SomeUtil.getColorEx
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

@SuppressLint("ViewConstructor")
class ChartMarkerView(context: Context, private val valueFormatter: ValueFormatter, private val stats: List<LineDataSet>)
    : MarkerView(context, R.layout.chart_makerview){

    private val tvData: TextView = findViewById(R.id.textView_Data)
    private val tvTitle: TextView = findViewById(R.id.textView_Title)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e != null) {
            tvTitle.text = valueFormatter.getFormattedValue(e.x)

            val dataText = stats.joinToString("<br>") { dataSet ->
                // Извлекаем RRGGBB, пропуская Alpha (первые 2 символа после #, если они есть)
                val colorHex = String.format("%06X", (0xFFFFFF and dataSet.color))
                "<font color='#$colorHex'>∎</font> ${dataSet.label}: ${dataSet.getEntriesForXValue(e.x).firstOrNull()?.y ?: 0}"
            }

            tvData.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(dataText, Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(dataText)
            }

            measure(
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
            layout(0, 0, measuredWidth, measuredHeight)
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffsetForDrawingAtPoint(posX: Float, posY: Float): MPPointF {
        val yPosition = if (posY < height) {
            10f
        } else {
            -height - 10f
        }
        return MPPointF(-width / 2f, yPosition)
    }
}