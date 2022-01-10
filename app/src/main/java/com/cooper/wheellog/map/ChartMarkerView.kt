package com.cooper.wheellog.map
import com.github.mikephil.charting.utils.MPPointF
import android.content.Context
import android.widget.TextView
import com.cooper.wheellog.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight

class ChartMarkerView(context: Context, private val valueFormatter: ValueFormatter, private val tripData: TripData)
    : MarkerView(context, R.layout.chart_makerview){

    val tvData: TextView = findViewById(R.id.textView_Data)
    val tvTitle: TextView = findViewById(R.id.textView_Title)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e != null && tripData.stats != null) {
            tvTitle.text = valueFormatter.getFormattedValue(e.x)
            val stringBuffer = StringBuffer()
            tripData.stats!!.forEach {
                stringBuffer.append(it.label + ": ${it.getEntriesForXValue(e.x).first().y}\n")
            }
            tvData.text = stringBuffer
        }

        super.refreshContent(e, highlight)
    }

    override fun getOffsetForDrawingAtPoint(posX: Float, posY: Float): MPPointF {
        val yPosition = -height - 100f
        return if (posX < 300) MPPointF(-width / 2f + 180, yPosition)
        else MPPointF(-width / 2f - 180, yPosition)
    }
}