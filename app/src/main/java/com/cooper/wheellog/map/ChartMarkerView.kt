package com.cooper.wheellog.map
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.text.Html
import android.widget.TextView
import com.cooper.wheellog.R
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import okhttp3.internal.toHexString

@SuppressLint("ViewConstructor")
class ChartMarkerView(context: Context, private val valueFormatter: ValueFormatter, private val stats: List<LineDataSet>)
    : MarkerView(context, R.layout.chart_makerview){

    private val tvData: TextView = findViewById(R.id.textView_Data)
    private val tvTitle: TextView = findViewById(R.id.textView_Title)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e != null) {
            tvTitle.text = valueFormatter.getFormattedValue(e.x)

            val dataText = mutableListOf<String>()
            stats.forEach {
                dataText.add(
                    "<font color=#${it.color.toHexString().substring(2)}>∎</font> ${it.label}: " +
                            "${it.getEntriesForXValue(e.x).first().y}"
                )
            }
            tvData.text = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                Html.fromHtml(dataText.joinToString("<br>"), Html.FROM_HTML_MODE_LEGACY)
            } else {
                Html.fromHtml(dataText.joinToString("<br>"))
            }
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