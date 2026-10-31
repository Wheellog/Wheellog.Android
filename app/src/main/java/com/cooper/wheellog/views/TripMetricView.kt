package com.cooper.wheellog.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/** Captions start below the number; a large icon spans both text rows. */
class TripMetricView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.LEFT }
    private val captionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.LEFT }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private var number = "—"
    private var label = ""
    private var symbol = ""
    private data class TextLayout(val text: String, val paint: Paint, val x: Float, val baseline: Float, val bounds: RectF)

    init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES }

    fun bind(icon: String, number: String, label: String, color: Int, mutedColor: Int, font: Typeface?) {
        this.symbol = icon
        this.number = number
        this.label = label
        valuePaint.color = color
        iconPaint.color = color
        captionPaint.color = mutedColor
        valuePaint.typeface = font
        contentDescription = "$label: $number"
        invalidate()
    }

    private fun textLayouts(): List<TextLayout> {
        if (width <= 0 || height <= 0) return emptyList()
        val density = resources.displayMetrics.density
        val scaledDensity = resources.displayMetrics.scaledDensity
        val iconWidth = min(height.toFloat(), width * .34f)
        val left = iconWidth + 4 * density
        val right = (width - 2 * density).coerceAtLeast(left + 1)
        val split = height * .63f
        fun fit(text: String, paint: Paint, area: RectF, maximum: Float): TextLayout {
            paint.textSize = maximum
            repeat(3) {
                val fm = paint.fontMetrics
                val scale = min(area.width() / paint.measureText(text).coerceAtLeast(1f),
                    area.height() / (fm.descent - fm.ascent).coerceAtLeast(1f)).coerceAtMost(1f)
                paint.textSize *= scale
            }
            val fm = paint.fontMetrics
            val baseline = area.centerY() - (fm.ascent + fm.descent) / 2
            val halfWidth = paint.measureText(text) / 2
            val x = if (paint.textAlign == Paint.Align.LEFT) area.left else area.centerX()
            val start = if (paint.textAlign == Paint.Align.LEFT) x else x - halfWidth
            return TextLayout(text, paint, x, baseline,
                RectF(start, baseline + fm.ascent, start + halfWidth * 2, baseline + fm.descent))
        }
        return listOf(
            fit(symbol, iconPaint, RectF(0f, 0f, iconWidth, height.toFloat()), 32 * scaledDensity),
            fit(number, valuePaint, RectF(left, 0f, right, split), 20 * scaledDensity),
            fit(label, captionPaint, RectF(left, split, right, height.toFloat()), 11 * scaledDensity))
    }

    internal fun renderedTextBounds(): List<RectF> = textLayouts().map { RectF(it.bounds) }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        textLayouts().forEach { canvas.drawText(it.text, it.x, it.baseline, it.paint) }
    }
}
