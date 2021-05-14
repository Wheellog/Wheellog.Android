package com.cooper.wheellog.views

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.math.MathUtils
import com.cooper.wheellog.R
import com.cooper.wheellog.WearData
import java.util.*
import kotlin.math.*

class MainView(context: Context, attrs: AttributeSet?, var wd: WearData) : View(context, attrs) {

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, WearData()) {
        if (isInEditMode) {
            wd.apply {
                timeString = "15:50"
                currentOnDial = false
                speed = 2.5
                maxSpeed = 50
                temperature = 33
                maxTemperature = 80
                battery = 90
                batteryLowest = 50
            }
        }
    }

    private var tfFont: Typeface =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.resources.getFont(R.font.prime)
        else ResourcesCompat.getFont(context, R.font.prime)!!
    private var outerArcPaint: Paint
    private var innerArcPaint: Paint
    private var textPaint = Paint().apply {
        textAlign = Paint.Align.CENTER
        typeface = tfFont
    }
    private val outerArcRect = RectF()
    private val innerArcRect = RectF()
    private var oaDiameter = 0f
    private val speedTextRect = RectF()
    private val batteryTextRect = RectF()
    private val temperatureTextRect = RectF()
    private var speedTextSize = 0f
    private var speedTextKPHSize = 0f
    private var speedTextKPHHeight = 0f
    private var innerArcTextSize = 0f
    private var outerStrokeWidth = 0f
    private var innerStrokeWidth = 0f
    private var innerOuterPadding = 0f
    private var innerTextPadding = 0f
    private var centerX = 0f
    private var centerY = 0f
    private val boundaryOfText = Rect()

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        keepScreenOn = true
        outerArcPaint = Paint().apply {
            isAntiAlias = true
            strokeWidth = outerStrokeWidth
            style = Paint.Style.STROKE
        }
        innerArcPaint = Paint().apply {
            isAntiAlias = true
            strokeWidth = outerStrokeWidth
            style = Paint.Style.STROKE
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // Account for padding
        val xPad = (paddingLeft + paddingRight).toFloat()
        val ww = w.toFloat() - xPad
        outerStrokeWidth = ww / 8
        innerOuterPadding = ww / 50
        innerStrokeWidth = (outerStrokeWidth * 0.4).roundToInt().toFloat()
        oaDiameter = ww - outerStrokeWidth
        val oaRadius = oaDiameter / 2
        centerX = w / 2f
        centerY = ww / 2 + paddingTop
        val orLeft = centerX - oaRadius
        val orTop = centerY - oaRadius
        val orRight = centerX + oaRadius
        val orBottom = centerY + oaRadius
        outerArcRect[orLeft, orTop, orRight] = orBottom
        outerArcPaint.strokeWidth = outerStrokeWidth

        val iaDiameter = oaDiameter - outerStrokeWidth - innerStrokeWidth - innerOuterPadding * 2

        val iaRadius = iaDiameter / 2
        val left = centerX - iaRadius
        val top = centerY - iaRadius
        val right = centerX + iaRadius
        val bottom = centerY + iaRadius
        innerArcRect[left, top, right] = bottom
        val innerArcHypot = (innerArcRect.right - innerArcRect.left - innerStrokeWidth - innerTextPadding).roundToInt()
        val speedTextRectSize = sqrt(2 * (innerArcHypot / 2f).toDouble().pow(2.0)).roundToInt()
        speedTextRect[centerX - speedTextRectSize / 2f, centerY - speedTextRectSize / 2f, centerX + speedTextRectSize / 2f] = centerY + speedTextRectSize / 2f
        speedTextSize = calculateFontSize(boundaryOfText, speedTextRect, "00", textPaint)
        speedTextRect.set(boundaryOfText)
        speedTextRect.top = (centerY - boundaryOfText.height() / 2f - boundaryOfText.height() / 10f).roundToInt().toFloat()
        speedTextRect.bottom = (speedTextRect.top + boundaryOfText.height()).roundToInt().toFloat()
        val speedTextKPHRectSize = speedTextRectSize / 3
        val speedTextKPHRect = RectF(
            centerX - speedTextKPHRectSize / 2f,
            centerY - speedTextKPHRectSize / 2f,
            centerX + speedTextKPHRectSize / 2f,
            centerY + speedTextKPHRectSize / 2f)
        speedTextKPHSize = calculateFontSize(
            boundaryOfText,
            speedTextKPHRect,
            wd.mainUnit,
            textPaint)
        speedTextKPHHeight = boundaryOfText.height().toFloat()
        batteryTextRect[
                centerX / 2f,
                speedTextRect.bottom + innerStrokeWidth,
                centerX] = speedTextRect.bottom + innerStrokeWidth * 3
        temperatureTextRect[
                centerX,
                batteryTextRect.top,
                centerX + centerX / 2f] =
            batteryTextRect.bottom
        innerArcTextSize = ww / 18f
        innerArcPaint.strokeWidth = innerStrokeWidth
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        var currentDial: Int = if (wd.currentOnDial) {
            (112 * wd.current / MathUtils.clamp(wd.maxCurrent, 1.0, 100.0)).roundToInt()
        } else {
            (112 * wd.speed / MathUtils.clamp(wd.maxSpeed, 1, 100)).roundToInt()
        }

        //####################################################
        //################# DRAW OUTER ARC ###################
        //####################################################
        outerArcPaint.color = context.getColor(R.color.arc_dim)
        canvas.drawArc(outerArcRect, 144f, 252f, false, outerArcPaint)
        outerArcPaint.color =
            if (currentDial >= 0) context.getColor(R.color.positive_dial)
            else context.getColor(R.color.negative_dial)
        currentDial = abs(currentDial)

        canvas.drawArc(outerArcRect, 144f, currentDial * 2.25f, false, outerArcPaint)

        //####################################################
        //################# DRAW INNER ARC ###################
        //####################################################
        innerArcPaint.color = context.getColor(R.color.arc_dim)
        canvas.drawArc(innerArcRect, 144f, 90f, false, innerArcPaint)
        canvas.drawArc(innerArcRect, 306f, 90f, false, innerArcPaint)

        val batteryValue = MathUtils.clamp(wd.battery, 0, 100) * 90f / 100f
        val batteryLowestValue = MathUtils.clamp(wd.batteryLowest, 0, wd.battery) * 90f / 100f
        innerArcPaint.color = context.getColor(R.color.battery_dial)
        canvas.drawArc(innerArcRect, 144f, batteryValue, false, innerArcPaint)
        innerArcPaint.color = context.getColor(R.color.battery_low_dial)
        canvas.drawArc(innerArcRect, 144f + batteryValue, batteryLowestValue - batteryValue, false, innerArcPaint)

        // 0 - max | -90 - min
        val value = MathUtils.clamp(wd.temperature, 0, 80) / 2f * 2.25f - 90f
        if (wd.temperature > 0) {
            innerArcPaint.color = context.getColor(R.color.temperature_dial)
            canvas.drawArc(innerArcRect, 306 - value, 90 + value, false, innerArcPaint)
        } else {
            innerArcPaint.color = context.getColor(R.color.arc_dim)
            canvas.drawArc(innerArcRect, 306f, 90f, false, innerArcPaint)
        }

        //####################################################
        //################# DRAW SPEED TEXT ##################
        //####################################################
        val speed = wd.speed
        val speedString: String =
            if (speed < 10) String.format(Locale.US, "%.1f", speed)
            else String.format(Locale.US, "%02d", speed.toInt())
        textPaint.color = if (wd.alarm) context.getColor(R.color.accent) else context.getColor(R.color.speed_text)
        textPaint.textSize = speedTextSize
        canvas.drawText(speedString, outerArcRect.centerX(), speedTextRect.centerY() + speedTextRect.height() / 2, textPaint)

        textPaint.textSize = speedTextKPHSize
        textPaint.color = context.getColor(R.color.teal_200)
        canvas.drawText(wd.timeString, outerArcRect.centerX(), speedTextRect.top - speedTextRect.height() / 3f, textPaint)

        //####################################################
        //######## DRAW BATTERY AND TEMPERATURE TEXT #########
        //####################################################
        if (wd.battery > -1) {
            textPaint.textSize = innerArcTextSize
            val bestBatteryString = java.lang.String.format(Locale.US, "%02d%%", wd.battery)
            canvas.drawText(bestBatteryString, batteryTextRect.centerX(), batteryTextRect.centerY(), textPaint)
            val temperatureString = java.lang.String.format(Locale.US, "%02d℃", wd.temperature)
            canvas.drawText(temperatureString, temperatureTextRect.centerX(), temperatureTextRect.centerY(), textPaint)
        }
    }

    private fun calculateFontSize(textBounds: Rect, textContainer: RectF, text: String, textPaint: Paint): Float {
        textPaint.textSize = 100f
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val h = textBounds.height()
        val w = textPaint.measureText(text)
        val targetH = textContainer.height()
        val targetW = textContainer.width()
        val sizeH = targetH / h * 100f
        val sizeW = targetW / w * 100f
        val result = min(sizeH, sizeW)
        textPaint.textSize = result
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        return result
    }
}