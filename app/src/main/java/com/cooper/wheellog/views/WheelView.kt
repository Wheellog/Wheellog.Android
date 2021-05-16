package com.cooper.wheellog.views

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.math.MathUtils
import com.cooper.wheellog.*
import com.cooper.wheellog.presentation.preferences.MultiSelectPreference.Companion.separator
import com.cooper.wheellog.utils.MathsUtil.dpToPx
import com.cooper.wheellog.utils.MathsUtil.kmToMiles
import com.cooper.wheellog.utils.ReflectUtil
import com.cooper.wheellog.utils.SomeUtil
import com.cooper.wheellog.utils.SomeUtil.Companion.getColorEx
import timber.log.Timber
import java.util.*
import kotlin.math.*


@SuppressLint("ClickableViewAccessibility")
class WheelView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var currentTheme = R.style.OriginalTheme
    private var outerArcPaint = Paint()
    private var innerArcPaint = Paint()
    private var textPaint = Paint()
    private val outerArcRect = RectF()
    private val innerArcRect = RectF()
    private val middleArcRect = RectF()
    private val voltArcRect = RectF()
    private lateinit var mViewBlocks: Array<ViewBlockInfo>
    private var oaDiameter = 0f
    private val speedTextRect = RectF()
    private val batteryTextRect = RectF()
    private val temperatureTextRect = RectF()
    private var modelTextPath: Path? = null
    private var modelTextPaint = Paint()
    private var versionPaint = Paint()
    private var middleArcPaint = Paint()
    private var voltArcPaint = Paint()
    private var speedTextSize = 0f
    private var speedTextKPHSize = 0f
    private var speedTextKPHHeight = 0f
    private var innerArcTextSize = 0f
    private var boxTextSize = 0f
    private var boxTextHeight = 0f
    private var mSpeed = 0
    private var mBattery = 0
    private var mBatteryLowest = 101
    private var mTemperature = 0
    private var mMaxTemperature = 0
    private var mCurrentTime = "00:00:00"
    private var mDistance = 0.0
    private var mTotalDistance = 0.0
    private var mTopSpeed = 0.0
    private var mVoltage = 0.0
    private var mCurrent = 0.0
    private var mPwm = 0.0
    private var mMaxPwm = 0.0
    private var mAverageSpeed = 0.0
    private var useMph: Boolean
    private var mWheelModel = ""
    private val versionString = String.format("ver %s %s", BuildConfig.VERSION_NAME, BuildConfig.BUILD_DATE)
    private var outerStrokeWidth = 0f
    private var innerStrokeWidth = 0f
    private var middleStrokeWidth = 0f
    private var voltStrokeWidth = 0f
    private var mediumInnerPadding = 0f
    private var outerMediumPadding = 0f
    private var boxMiddlePadding = 0f
    private var innerOuterPadding = 0f
    private var innerTextPadding = 0f
    private var boxTopPadding = 0f
    private var boxOuterPadding = 0f
    private var boxInnerPadding = 0f
    private var centerX = 0f
    private var centerY = 0f
    private val boundaryOfText = Rect()
    var refreshDisplay = false
    private var targetSpeed = 0
    private var targetCurrent = 0
    private var currentSpeed = 0
    private var currentCurrent = 0
    private var targetTemperature = 112
    private var currentTemperature = 112
    private var targetBattery = 0
    private var targetBatteryLowest = 0
    private var currentBattery = 0
    private var mTextBoxesBitmap: Bitmap? = null
    private var mCanvas: Canvas? = null
    private val refreshHandler = Handler()
    private var boxRects = arrayOf<RectF?>()
    private val refreshRunner: Runnable = object : Runnable {
        override fun run() {
            if (refreshDisplay) {
                invalidate()
                refreshHandler.postDelayed(this, 30)
            }
        }
    }

    private val viewBlockInfo: Array<ViewBlockInfo>
        get() = arrayOf(
                ViewBlockInfo(resources.getString(R.string.pwm)) { String.format(Locale.US, "%.2f%%", mPwm) },
                ViewBlockInfo(resources.getString(R.string.max_pwm)) { String.format(Locale.US, "%.2f%%", mMaxPwm) },
                ViewBlockInfo(resources.getString(R.string.voltage)) { String.format(Locale.US, "%.2f " + resources.getString(R.string.volt), mVoltage) },
                ViewBlockInfo(resources.getString(R.string.average_riding_speed))
                {
                    if (useMph) {
                        String.format(Locale.US, "%.1f " + resources.getString(R.string.mph), kmToMiles(mAverageSpeed))
                    } else {
                        String.format(Locale.US, "%.1f " + resources.getString(R.string.kmh), mAverageSpeed)
                    }
                },
                ViewBlockInfo(resources.getString(R.string.riding_time)) { mCurrentTime },
                ViewBlockInfo(resources.getString(R.string.top_speed))
                {
                    if (useMph) {
                        String.format(Locale.US, "%.1f " + resources.getString(R.string.mph), kmToMiles(mTopSpeed))
                    } else {
                        String.format(Locale.US, "%.1f " + resources.getString(R.string.kmh), mTopSpeed)
                    }
                },
                ViewBlockInfo(resources.getString(R.string.distance))
                {
                    if (useMph) {
                        String.format(Locale.US, "%.2f " + resources.getString(R.string.milli), kmToMiles(mDistance))
                    } else {
                        if (mDistance < 1) {
                            String.format(Locale.US, "%.0f " + resources.getString(R.string.metre), mDistance * 1000)
                        } else {
                            String.format(Locale.US, "%.2f " + resources.getString(R.string.km), mDistance)
                        }
                    }
                },
                ViewBlockInfo(resources.getString(R.string.total))
                {
                    if (useMph) {
                        String.format(Locale.US, "%.0f " + resources.getString(R.string.milli), kmToMiles(mTotalDistance))
                    } else {
                        String.format(Locale.US, "%.0f " + resources.getString(R.string.km), mTotalDistance)
                    }
                },
                ViewBlockInfo(resources.getString(R.string.battery)) { String.format(Locale.US, "%d %%", mBattery) },
                ViewBlockInfo(resources.getString(R.string.current)) { String.format(Locale.US, "%.2f " + resources.getString(R.string.amp), mCurrent) },
                ViewBlockInfo(resources.getString(R.string.maxcurrent)) { String.format(Locale.US, "%.2f " + resources.getString(R.string.amp), WheelData.getInstance().maxCurrent) },
                ViewBlockInfo(resources.getString(R.string.power),
                        { String.format(Locale.US, "%.2f " + resources.getString(R.string.watt), WheelData.getInstance().powerDouble) }, false),
                ViewBlockInfo(resources.getString(R.string.maxpower),
                        { String.format(Locale.US, "%.0f " + resources.getString(R.string.watt), WheelData.getInstance().maxPower) }, false),
                ViewBlockInfo(resources.getString(R.string.temperature),
                        { String.format(Locale.US, "%d ℃", WheelData.getInstance().temperature) }, false),
                ViewBlockInfo(resources.getString(R.string.temperature2),
                        { String.format(Locale.US, "%d ℃", WheelData.getInstance().temperature2) }, false),
                ViewBlockInfo(resources.getString(R.string.maxtemperature),
                        { String.format(Locale.US, "%d ℃", mMaxTemperature) }, false),
                ViewBlockInfo(resources.getString(R.string.average_speed),
                        {
                            if (useMph) {
                                String.format(Locale.US, "%.1f " + resources.getString(R.string.mph), kmToMiles(WheelData.getInstance().averageSpeedDouble))
                            } else {
                                String.format(Locale.US, "%.1f " + resources.getString(R.string.kmh), WheelData.getInstance().averageSpeedDouble)
                            }
                        }, false),
                ViewBlockInfo(resources.getString(R.string.ride_time),
                        { WheelData.getInstance().rideTimeString }, false),
                ViewBlockInfo(resources.getString(R.string.wheel_distance),
                        {
                            if (useMph) {
                                String.format(Locale.US, "%.2f " + resources.getString(R.string.milli), kmToMiles(WheelData.getInstance().wheelDistanceDouble))
                            } else {
                                String.format(Locale.US, "%.3f " + resources.getString(R.string.km), WheelData.getInstance().wheelDistanceDouble)
                            }
                        }, false)
        )

    fun setWheelModel(mWheelModel: String) {
        if (this.mWheelModel != mWheelModel) {
            this.mWheelModel = mWheelModel
            calcModelTextSize()
        }
    }

    fun updateViewBlocksVisibility() {
        val viewBlocksString = WheelLog.AppConfig.viewBlocksString
        val viewBlocks = viewBlocksString?.split(separator)?.toTypedArray()
                ?: arrayOf(
                        resources.getString(R.string.voltage),
                        resources.getString(R.string.average_riding_speed),
                        resources.getString(R.string.riding_time),
                        resources.getString(R.string.top_speed),
                        resources.getString(R.string.distance),
                        resources.getString(R.string.total)
                )
        for (block in mViewBlocks) {
            block.enabled = false
            block.index = -1
        }
        var index = 0
        for (title in viewBlocks) {
            for (block in mViewBlocks) {
                if (block.title == title) {
                    block.index = index++
                    block.enabled = true
                    break
                }
            }
        }
        useMph = WheelLog.AppConfig.useMph
        Arrays.sort(mViewBlocks)
    }

    fun resetBatteryLowest() {
        mBatteryLowest = 101
        refresh()
    }

    fun setSpeed(value: Int) {
        var speed = value
        if (mSpeed == speed) return
        mSpeed = speed
        val maxSpeed = WheelLog.AppConfig.maxSpeed * 10
        speed = if (speed > maxSpeed) maxSpeed else speed
        targetSpeed = (abs(speed).toFloat() / maxSpeed * 112).roundToInt()
        refresh()
    }

    fun setBattery(battery: Int) {
        if (mBattery == battery) return
        mBattery = MathUtils.clamp(battery, 0, 100)
        targetBattery = (40f / 100f * mBattery).roundToInt()
        mBatteryLowest = if (mBattery > 0) {
            min(mBatteryLowest, mBattery)
        } else {
            if (mBatteryLowest > 100) mBatteryLowest else mBattery
        }
        targetBatteryLowest = (40f / 100f * mBatteryLowest).roundToInt()
        refresh()
    }

    fun setTemperature(temperature: Int) {
        if (mTemperature == temperature) return
        mTemperature = MathUtils.clamp(temperature, -100, 100)
        targetTemperature = 112 - (40f / 80f * MathUtils.clamp(mTemperature, 0, 80)).roundToInt()
        refresh()
    }

    fun setMaxTemperature(temperature: Int) {
        if (mMaxTemperature == temperature) return
        mMaxTemperature = temperature
        refresh()
    }

    fun setMaxPwm(pwm: Double) {
        if (mMaxPwm == pwm) return
        mMaxPwm = pwm
        refresh()
    }

    fun setPwm(pwm: Double) {
        if (mPwm == pwm) return
        mPwm = pwm
        refresh()
    }

    fun setRideTime(currentTime: String) {
        if (mCurrentTime == currentTime) return
        mCurrentTime = currentTime
        refresh()
    }

    fun setDistance(distance: Double) {
        if (mDistance == distance) return
        mDistance = distance
        refresh()
    }

    fun setTotalDistance(totalDistance: Double) {
        if (mTotalDistance == totalDistance) return
        mTotalDistance = totalDistance
        refresh()
    }

    fun setTopSpeed(topSpeed: Double) {
        if (mTopSpeed == topSpeed) return
        mTopSpeed = topSpeed
        refresh()
    }

    fun setVoltage(voltage: Double) {
        if (mVoltage == voltage) return
        mVoltage = voltage
        refresh()
    }

    fun setAverageSpeed(avg_speed: Double) {
        if (mAverageSpeed == avg_speed) return
        mAverageSpeed = avg_speed
        refresh()
    }

    fun setCurrent(value: Double) {
        var current = value
        if (mCurrent == current) return
        mCurrent = current
        val maxSpeed = WheelLog.AppConfig.maxSpeed
        current = if (abs(current) > maxSpeed) maxSpeed.toDouble() else current
        targetCurrent = (current / maxSpeed.toDouble() * 112).roundToInt()
        refresh()
    }

    private fun refresh() {
        if (!refreshDisplay) {
            refreshDisplay = true
            refreshHandler.postDelayed(refreshRunner, 30)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val landscape = w > h

        // Account for padding
        val xPad = (paddingLeft + paddingRight).toFloat()
        val ww = if (landscape) {
            h.toFloat() - xPad
        } else {
            w.toFloat() - xPad
        }
        outerStrokeWidth = ww / 8
        innerStrokeWidth = (outerStrokeWidth * 0.6).roundToInt().toFloat()
        oaDiameter = ww - outerStrokeWidth
        val oaRadius = oaDiameter / 2
        centerX = w / 2f
        centerY = if (landscape) h / 2f else ww / 2 + paddingTop
        val orLeft = centerX - oaRadius
        val orTop = centerY - oaRadius
        val orRight = centerX + oaRadius
        val orBottom = centerY + oaRadius
        outerArcRect[orLeft, orTop, orRight] = orBottom
        outerArcPaint.strokeWidth = outerStrokeWidth

        val imDiameter = oaDiameter - outerStrokeWidth - middleStrokeWidth - outerMediumPadding * 2
        var iaDiameter = 0f
        when (currentTheme) {
            R.style.OriginalTheme -> {
                iaDiameter = oaDiameter - outerStrokeWidth - innerStrokeWidth - innerOuterPadding * 2
            }
            R.style.AJDMTheme -> {
                val imRadius = imDiameter / 2
                val midLeft: Float = centerX - imRadius
                val midTop: Float = centerY - imRadius
                val midRight: Float = centerX + imRadius
                val midBottom: Float = centerY + imRadius
                iaDiameter = imDiameter - middleStrokeWidth - innerStrokeWidth - mediumInnerPadding * 2
                middleArcRect.set(midLeft, midTop, midRight, midBottom)
            }
        }

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
                resources.getString(R.string.kmh),
                textPaint)
        speedTextKPHHeight = boundaryOfText.height().toFloat()
        val innerTextRectWidth = innerStrokeWidth.roundToInt()
        batteryTextRect[centerX - iaDiameter / 2 - innerTextRectWidth / 2f, centerY - innerTextRectWidth / 2f, centerX - iaDiameter / 2 + innerTextRectWidth / 2f] = centerY + innerTextRectWidth / 2f
        temperatureTextRect[centerX + iaDiameter / 2 - innerTextRectWidth / 2f, centerY - innerTextRectWidth / 2f, centerX + iaDiameter / 2 + innerTextRectWidth / 2f] = centerY + innerTextRectWidth / 2f
        innerArcTextSize = calculateFontSize(boundaryOfText, batteryTextRect, "88%", textPaint)
        innerArcPaint.strokeWidth = innerStrokeWidth
        calcModelTextSize()
        mTextBoxesBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mTextBoxesBitmap!!)
        if (landscape && w.toFloat() / h > 1.4 || !landscape && h.toFloat() / w > 1.1) {
            redrawTextBoxes()
        }
        versionPaint.textSize = (height / 50.0).roundToInt().toFloat()
        refresh()
    }

    private fun calcModelTextSize() {
        val modelTextRect = RectF(
                innerArcRect.left + innerOuterPadding * 2,
                innerArcRect.top + innerOuterPadding * 2,
                innerArcRect.right - innerOuterPadding * 2,
                innerArcRect.bottom - innerOuterPadding * 2)
        modelTextPath = Path()
        modelTextPath!!.addArc(modelTextRect, 190f, 160f)
        modelTextRect.bottom = modelTextRect.top + innerStrokeWidth * 1.2f
        modelTextPaint.textSize = calculateFontSize(boundaryOfText, modelTextRect, mWheelModel, modelTextPaint) / 2
    }

    private fun drawTextBox(header: String, value: String, canvas: Canvas, rect: RectF, paint: Paint, paintDescription: Paint) {
        val x = rect.centerX()
        val y = rect.centerY() - boxInnerPadding
        canvas.drawText(value, x, y, paint)
        canvas.drawText(header, x, y + boxTextSize * 0.7f, paintDescription)
    }

    fun redrawTextBoxes() {
        if (mTextBoxesBitmap == null || mCanvas == null) {
            return
        }
        mTextBoxesBitmap!!.eraseColor(Color.TRANSPARENT)
        val w = width
        val h = height
        val landscape = w > h
        var countBlocks = 0
        for (block in mViewBlocks) {
            if (block.enabled) {
                countBlocks++
            }
        }
        if (countBlocks == 0) {
            return
        }
        var cols = 2
        var rows = (countBlocks / cols.toFloat() + 0.499f).roundToInt()
        boxRects = arrayOfNulls((cols + 1) * rows)
        if (landscape) {
            var boxTop = paddingTop.toFloat()
            val boxH = (h - boxTop - paddingBottom) / rows.toFloat() - boxInnerPadding
            val boxW = (w - oaDiameter - paddingRight) / cols.toFloat() - outerStrokeWidth
            var i = 0
            val boxLeft = paddingLeft.toFloat()
            val boxLeft2 = w - boxW - paddingRight
            for (row in 0 until rows) {
                boxRects[i++] = RectF(boxLeft, boxTop, boxLeft + boxW, boxTop + boxH)
                boxRects[i++] = RectF(boxLeft2, boxTop, boxLeft2 + boxW, boxTop + boxH)
                boxTop += boxH + boxInnerPadding
            }
        } else {
            var boxTop = boxTopPadding + outerArcRect.top + oaDiameter / 2 + (cos(Math.toRadians(54.0)) * (oaDiameter + outerStrokeWidth) / 2).toFloat()
            if (countBlocks == 1) {
                cols = 1
            } else {
                val hh = h - boxTop - paddingBottom - boxInnerPadding
                val ratio = w / hh
                rows = sqrt((countBlocks.toFloat() / ratio * 3).toDouble()).toInt()
                cols = (countBlocks / rows.toFloat() + 0.499f).roundToInt()
                // packing
                if (countBlocks != cols * rows) {
                    while (true) {
                        if (countBlocks / rows.toFloat() <= cols) {
                            rows--
                        } else {
                            rows++
                            break
                        }
                    }
                }
            }
            val boxH = (h - boxTop - paddingBottom) / rows.toFloat() - boxInnerPadding
            val boxW = (w - paddingRight) / cols.toFloat() - boxInnerPadding
            var i = 0
            for (row in 0 until rows) {
                var boxLeft = paddingLeft.toFloat()
                var col = 0
                while (col < cols && i < countBlocks) {
                    boxRects[i++] = RectF(boxLeft, boxTop, boxLeft + boxW, boxTop + boxH)
                    boxLeft += boxW + boxInnerPadding
                    col++
                }
                boxTop += boxH + boxInnerPadding
            }
        }
        boxTextSize = calculateFontSize(boundaryOfText, boxRects[0]!!, "10000 km/h", textPaint, 2) * 1.2f
        boxTextHeight = boundaryOfText.height().toFloat()
        val paint = Paint(textPaint)
        paint.textSize = boxTextSize * 0.8f
        paint.color = getColorEx(R.color.wheelview_text)
        val paintDescription = Paint(paint)
        paintDescription.textSize = boxTextSize / 2f
        paintDescription.alpha = 150
        try {
            var i = 0
            for (block in mViewBlocks) {
                if (block.enabled) {
                    drawTextBox(block.title, block.getValue(), mCanvas!!, boxRects[i++]!!, paint, paintDescription)
                }
            }
        } catch (e: Exception) {
            Timber.i("Draw exception: %s", e.message)
            e.printStackTrace()
        }
    }

    private fun getBlockIndexBy(x: Float, y: Float): Int {
        for ((i, box) in boxRects.withIndex()) {
            if (box != null && box.contains(x, y))
                return i
        }
        return -1
    }

    private fun drawOriginal(canvas: Canvas) {
        var currentDial: Int
        if (WheelLog.AppConfig.currentOnDial) {
            currentCurrent = updateCurrentValue2(targetCurrent, currentCurrent)
            currentDial = currentCurrent
        } else {
            currentSpeed = updateCurrentValue(targetSpeed, currentSpeed)
            currentDial = currentSpeed
        }
        currentTemperature = updateCurrentValue(targetTemperature, currentTemperature)
        currentBattery = updateCurrentValue(targetBattery, currentBattery)

        //####################################################
        //################# DRAW OUTER ARC ###################
        //####################################################
        outerArcPaint.color = getColorEx(R.color.wheelview_arc_dim)
        canvas.drawArc(outerArcRect, 144f, 252f, false, outerArcPaint)
        if (currentDial >= 0) {
            outerArcPaint.color = getColorEx(R.color.wheelview_main_positive_dial)
        } else {
            outerArcPaint.color = getColorEx(R.color.wheelview_main_negative_dial)
        }
        currentDial = abs(currentDial)
        //###########TEST purp
        //currentSpeed = (int) Math.round(( mCurrent /(10*mMaxSpeed)) * 112);
        //########### <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<,
        for (i in 0 until currentDial) {
            val value = (144 + i * 2.25).toFloat()
            canvas.drawArc(outerArcRect, value, 1.5f, false, outerArcPaint)
        }

        //####################################################
        //################# DRAW INNER ARC ###################
        //####################################################
        innerArcPaint.color = getColorEx(R.color.wheelview_arc_dim)
        canvas.drawArc(innerArcRect, 144f, 90f, false, innerArcPaint)
        canvas.drawArc(innerArcRect, 306f, 90f, false, innerArcPaint)
        innerArcPaint.color = getColorEx(R.color.wheelview_battery_dial)
        for (i in 0..111) {
            if (i == targetBatteryLowest) innerArcPaint.color = getColorEx(R.color.wheelview_battery_low_dial)
            if (i == currentTemperature) innerArcPaint.color = getColorEx(R.color.wheelview_temperature_dial)
            if (i < currentBattery || i >= currentTemperature) {
                val value = 144 + i * 2.25f
                canvas.drawArc(innerArcRect, value, 1.5f, false, innerArcPaint)
            }
        }

        //####################################################
        //################# DRAW SPEED TEXT ##################
        //####################################################
        val speed = if (WheelLog.AppConfig.useMph) kmToMiles(mSpeed.toFloat()).roundToInt() else mSpeed
        val speedString: String = if (speed < 100) String.format(Locale.US, "%.1f", speed / 10.0) else String.format(Locale.US, "%02d", (speed / 10.0).roundToInt())
        val alarm1Speed = WheelLog.AppConfig.alarm1Speed.toDouble()
        if (!WheelLog.AppConfig.alteredAlarms && alarm1Speed * 10 > 0 && mSpeed >= alarm1Speed * 10) textPaint.color = getColorEx(R.color.accent) else textPaint.color = getColorEx(R.color.wheelview_speed_text)
        textPaint.textSize = speedTextSize
        canvas.drawText(speedString, outerArcRect.centerX(), speedTextRect.centerY() + speedTextRect.height() / 2, textPaint)
        textPaint.textSize = speedTextKPHSize
        textPaint.color = getColorEx(R.color.wheelview_text)
        if (WheelLog.AppConfig.useShortPwm || isInEditMode) {
            val pwm = String.format("%02.0f%% / %02.0f%%",
                    WheelData.getInstance().calculatedPwm,
                    WheelData.getInstance().maxPwm)
            textPaint.textSize = speedTextKPHSize * 1.2f
            canvas.drawText(pwm, outerArcRect.centerX(), speedTextRect.bottom + speedTextKPHHeight * 3.3f, textPaint)
        } else {
            val metric = if (WheelLog.AppConfig.useMph) resources.getString(R.string.mph) else resources.getString(R.string.kmh)
            canvas.drawText(metric, outerArcRect.centerX(), speedTextRect.bottom + speedTextKPHHeight * 1.1f, textPaint)
        }

        //####################################################
        //######## DRAW BATTERY AND TEMPERATURE TEXT #########
        //####################################################
        if (WheelData.getInstance().isConnected) {
            textPaint.textSize = innerArcTextSize
            canvas.save()
            if (width > height) canvas.rotate(144 + currentBattery * 2.25f - 180, innerArcRect.centerX(), innerArcRect.centerY()) else canvas.rotate(144 + currentBattery * 2.25f - 180, innerArcRect.centerY(), innerArcRect.centerX())
            val bestBatteryString = String.format(Locale.US, "%02d%%", mBattery)
            canvas.drawText(bestBatteryString, batteryTextRect.centerX(), batteryTextRect.centerY(), textPaint)
            canvas.restore()
            canvas.save()
            /// true battery
            val fixedPercents = WheelLog.AppConfig.fixedPercents
            if (WheelLog.AppConfig.useBetterPercents || fixedPercents) {
                if (width > height) canvas.rotate(144 + -3.3f * 2.25f - 180, innerArcRect.centerX(), innerArcRect.centerY()) else canvas.rotate(144 + -2 * 2.25f - 180, innerArcRect.centerY(), innerArcRect.centerX())
                var batteryCalculateType = "true"
                if (fixedPercents && !WheelData.getInstance().isVoltageTiltbackUnsupported) batteryCalculateType = "fixed"
                val batteryString = String.format(Locale.US, "%s", batteryCalculateType)
                canvas.drawText(batteryString, batteryTextRect.centerX(), batteryTextRect.centerY(), textPaint)
                canvas.restore()
                canvas.save()
            }
            if (width > height) canvas.rotate(143.5f + currentTemperature * 2.25f, innerArcRect.centerX(), innerArcRect.centerY()) else canvas.rotate(143.5f + currentTemperature * 2.25f, innerArcRect.centerY(), innerArcRect.centerX())
            val temperatureString = String.format(Locale.US, "%02d℃", mTemperature)
            canvas.drawText(temperatureString, temperatureTextRect.centerX(), temperatureTextRect.centerY(), textPaint)
            canvas.restore()
            canvas.save()

            // Max temperature
            if (width > height) canvas.rotate(-50f, innerArcRect.centerX(), innerArcRect.centerY()) else canvas.rotate(-50f, innerArcRect.centerY(), innerArcRect.centerX())
            val maxTemperatureString = String.format(Locale.US, "%02d℃", mMaxTemperature)
            canvas.drawText(maxTemperatureString, temperatureTextRect.centerX(), temperatureTextRect.centerY(), textPaint)
            canvas.restore()
            canvas.save()
        }

        // Wheel name
        canvas.drawTextOnPath(mWheelModel, modelTextPath!!, 0f, 0f, modelTextPaint)

        // Draw text blocks bitmap
        canvas.drawBitmap(mTextBoxesBitmap!!, 0f, 0f, textPaint)
        refreshDisplay = currentSpeed != targetSpeed || currentCurrent != targetCurrent || currentBattery != targetBattery || currentTemperature != targetTemperature
        if (width * 1.2 < height) {
            canvas.drawText(versionString, (
                    width - paddingRight).toFloat(), (
                    height - paddingBottom).toFloat(),
                    versionPaint)
        }
    }

    private fun drawAJDM(canvas: Canvas) {
        var currentDial: Int
        var currentDial2: Int
        if (WheelLog.AppConfig.currentOnDial) {
            currentSpeed = updateCurrentValue(targetSpeed, currentSpeed)
            currentCurrent = updateCurrentValue2(targetCurrent, currentCurrent)
            currentDial = currentCurrent
            currentDial2 = currentSpeed
        } else {
            currentSpeed = updateCurrentValue(targetSpeed, currentSpeed)
            currentCurrent = updateCurrentValue2(targetCurrent, currentCurrent)
            currentDial = currentSpeed
            currentDial2 = currentCurrent
        }
        currentTemperature = updateCurrentValue(targetTemperature, currentTemperature)
        currentBattery = updateCurrentValue(targetBattery, currentBattery)

        //####################################################
        //################# DRAW OUTER ARC ###################
        //####################################################
        outerArcPaint.color = getColorEx(R.color.ajdm_wheelview_arc_dim)
        canvas.drawArc(outerArcRect, 144f, 252f, false, outerArcPaint)
        if (currentDial >= 0) {
            outerArcPaint.color = getColorEx(R.color.ajdm_wheelview_main_positive_dial)
        } else {
            outerArcPaint.color = getColorEx(R.color.ajdm_wheelview_main_negative_dial)
        }
        currentDial = abs(currentDial)

        if (currentDial < 113) {
            canvas.drawArc(outerArcRect, 144f, currentDial * 2.25f, false, outerArcPaint)
        }
        if (currentDial > 112) {
            canvas.drawArc(outerArcRect, 144f, 112 * 2.25f, false, outerArcPaint)
        }

        //####################################################
        //################# DRAW MIDDLE ARC ##################
        //####################################################
        middleArcPaint.color = getColorEx(R.color.ajdm_wheelview_arc_dim)
        canvas.drawArc(middleArcRect, 144f, 252f, false, middleArcPaint)

        if (currentDial2 >= 0) {
            middleArcPaint.color = getColorEx(R.color.ajdm_wheelview_max_speed_dial)
        } else {
            middleArcPaint.color = getColorEx(R.color.ajdm_wheelview_avg_speed_dial)
        }
        currentDial2 = abs(currentDial2)
        if (currentDial2 < 113) {
            canvas.drawArc(middleArcRect, 144f, currentDial2 * 2.25f, false, middleArcPaint)
        }
        if (currentDial2 > 112) {
            canvas.drawArc(middleArcRect, 144f, 112 * 2.25f, false, middleArcPaint)
        }
        //####################################################
        //################# DRAW INNER ARC ###################
        //####################################################
        innerArcPaint.color = getColorEx(R.color.ajdm_wheelview_arc_dim)
        canvas.drawArc(innerArcRect, 144f, 90f, false, innerArcPaint)
        canvas.drawArc(innerArcRect, 306f, 90f, false, innerArcPaint)

        innerArcPaint.color = getColorEx(R.color.ajdm_wheelview_battery_dial)
        canvas.drawArc(innerArcRect, 144f, currentBattery * 2.25f, false, innerArcPaint)
        innerArcPaint.color = getColorEx(R.color.ajdm_wheelview_battery_low_dial)
        canvas.drawArc(innerArcRect, 144f, targetBatteryLowest * 2.25f, false, innerArcPaint)

        val value = (currentTemperature - 112) * 2.25f * 100 / 80

        if (mTemperature > 0) {
            innerArcPaint.color = getColorEx(R.color.ajdm_wheelview_temperature_dial)
            canvas.drawArc(innerArcRect, 306 - value, 90 + value, false, innerArcPaint)
        } else {
            innerArcPaint.color = getColorEx(R.color.ajdm_wheelview_arc_dim)
            canvas.drawArc(innerArcRect, 306f, 90f, false, innerArcPaint)
        }
        //####################################################
        //################# DRAW VOLT ARC TEST ###############
        //####################################################
        voltArcPaint.color = getColorEx(R.color.ajdm_wheelview_arc_dim)
        canvas.drawArc(voltArcRect, 144f, 252f, false, voltArcPaint)

        if (currentDial2 >= 0) {
            voltArcPaint.color = getColorEx(R.color.ajdm_wheelview_max_speed_dial)
        } else {
            voltArcPaint.color = getColorEx(R.color.ajdm_wheelview_avg_speed_dial)
        }
        currentDial2 = abs(currentDial2)
        if (currentDial2 < 113) {
            canvas.drawArc(voltArcRect, 144f, currentDial2 * 2.25f, false, voltArcPaint)
        }
        if (currentDial2 > 112) {
            canvas.drawArc(voltArcRect, 144f, 112 * 2.25f, false, voltArcPaint)
        }

        //####################################################
        //################# DRAW SPEED TEXT ##################
        //####################################################
        val speed = if (WheelLog.AppConfig.useMph) kmToMiles(mSpeed.toFloat()).roundToInt() else mSpeed
        val speedString: String = if (speed < 100) String.format(Locale.US, "%.1f", speed / 10.0) else String.format(Locale.US, "%02d", (speed / 10.0).roundToInt())
        val alarm1Speed = WheelLog.AppConfig.alarm1Speed.toDouble()
        if (!WheelLog.AppConfig.alteredAlarms && alarm1Speed * 10 > 0 && mSpeed >= alarm1Speed * 10)
            textPaint.color = getColorEx(R.color.ajdm_accent)
        else
            textPaint.color = getColorEx(R.color.ajdm_wheelview_speed_text)
        textPaint.textSize = speedTextSize
        canvas.drawText(speedString, outerArcRect.centerX(), speedTextRect.centerY() + speedTextRect.height() / 2, textPaint)
        textPaint.textSize = speedTextKPHSize
        textPaint.color = getColorEx(R.color.ajdm_wheelview_text)
        if (WheelLog.AppConfig.useShortPwm || isInEditMode) {
            val pwm = String.format("%02.0f%% | %02.0f%%",
                    WheelData.getInstance().calculatedPwm,
                    WheelData.getInstance().maxPwm)
            textPaint.textSize = speedTextKPHSize * 1.2f
            canvas.drawText(pwm, outerArcRect.centerX(), speedTextRect.bottom + speedTextKPHHeight * 3.3f, textPaint)
        } else {
            val metric = if (WheelLog.AppConfig.useMph) resources.getString(R.string.mph) else resources.getString(R.string.kmh)
            canvas.drawText(metric, outerArcRect.centerX(), speedTextRect.bottom + speedTextKPHHeight * 1.1f, textPaint)
        }

        //####################################################
        //######## DRAW BATTERY AND TEMPERATURE TEXT #########
        //####################################################
        if (mTemperature > 0 && mBattery > -1) {
            textPaint.textSize = innerArcTextSize
            canvas.save()
            if (width > height) canvas.rotate(140 + -3.3f * 2.25f - 180, innerArcRect.centerX(), innerArcRect.centerY()) else canvas.rotate(140 + -2 * 2.25f - 180, innerArcRect.centerY(), innerArcRect.centerX())
            val bestbatteryString = java.lang.String.format(Locale.US, "%02d%%", mBattery)
            canvas.drawText(bestbatteryString, batteryTextRect.centerX(), batteryTextRect.centerY(), textPaint)
            canvas.restore()
            canvas.save()
            /// true battery
            val fixedPercents = WheelLog.AppConfig.fixedPercents
            if (WheelLog.AppConfig.useBetterPercents || fixedPercents) {
                if (width > height) canvas.rotate(147 + currentBattery * 2.25f - 180, innerArcRect.centerX(), innerArcRect.centerY()) else canvas.rotate(146 + currentBattery * 2.25f - 180, innerArcRect.centerY(), innerArcRect.centerX())
                var batteryCalculateType = "true"
                if (fixedPercents && !WheelData.getInstance().isVoltageTiltbackUnsupported) batteryCalculateType = "fixed"
                val batteryString = java.lang.String.format(Locale.US, "%s", batteryCalculateType)
                canvas.drawText(batteryString, batteryTextRect.centerX(), batteryTextRect.centerY(), textPaint)
                canvas.restore()
                canvas.save()
            }
            if (width > height) canvas.rotate(138f + 120 * 2.25f, innerArcRect.centerX(), innerArcRect.centerY()) else canvas.rotate(135f + 120 * 2.25f, innerArcRect.centerY(), innerArcRect.centerX())
            val temperatureString = java.lang.String.format(Locale.US, "%02d℃", mTemperature)
            canvas.drawText(temperatureString, temperatureTextRect.centerX(), temperatureTextRect.centerY(), textPaint)
            canvas.restore()
            canvas.save()
        }

        // Wheel name
        canvas.drawTextOnPath(mWheelModel, modelTextPath!!, 0f, 0f, modelTextPaint)

        // Draw text blocks bitmap
        canvas.drawBitmap(mTextBoxesBitmap!!, 0f, 0f, textPaint)
        refreshDisplay = currentSpeed != targetSpeed || currentCurrent != targetCurrent || currentBattery != targetBattery || currentTemperature != targetTemperature
        if (width * 1.2 < height) {
            canvas.drawText(versionString, (
                    width - paddingRight).toFloat(), (
                    height - paddingBottom).toFloat(),
                    versionPaint)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        when (currentTheme) {
            R.style.OriginalTheme -> drawOriginal(canvas)
            R.style.AJDMTheme -> drawAJDM(canvas)
        }
    }

    private fun updateCurrentValue(target: Int, current: Int): Int {
        return if (target > current) current + 1 else if (current > target) current - 1 else target
    }

    private fun updateCurrentValue2(target: Int, current: Int): Int {
        return if (target > 0) {
            if (target > current) target else if (current > target) current - 1 else target
        } else {
            if (target < current) target else if (current > target) current + 1 else target
        }
    }

    private fun calculateFontSize(textBounds: Rect, textContainer: RectF, text: String, textPaint: Paint, lines: Int = 1): Float {
        textPaint.textSize = 100f
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        val h = textBounds.height()
        val w = textPaint.measureText(text)
        var targetH = textContainer.height()
        if (lines != 1) {
            targetH /= (lines.toFloat() * 1.2).toFloat()
        }
        val targetW = textContainer.width()
        val sizeH = targetH / h * 100f
        val sizeW = targetW / w * 100f
        val result = min(sizeH, sizeW)
        textPaint.textSize = result
        textPaint.getTextBounds(text, 0, text.length, textBounds)
        return result
    }

    private fun onChangeTheme() {
        val tfTest = if (isInEditMode) null else WheelLog.ThemeManager.getTypeface(context)
        when (currentTheme) {
            R.style.OriginalTheme -> {
                outerStrokeWidth = dpToPx(context, 40).toFloat()
                innerStrokeWidth = dpToPx(context, 25).toFloat()
                innerOuterPadding = dpToPx(context, 5).toFloat()
                innerTextPadding = 0f
                boxTopPadding = dpToPx(context, 10).toFloat()
                boxOuterPadding = dpToPx(context, 10).toFloat()
                boxInnerPadding = dpToPx(context, 10).toFloat()
                outerArcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                outerArcPaint.isAntiAlias = true
                outerArcPaint.strokeWidth = outerStrokeWidth
                outerArcPaint.style = Paint.Style.STROKE
                innerArcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                innerArcPaint.isAntiAlias = true
                innerArcPaint.strokeWidth = innerStrokeWidth
                innerArcPaint.style = Paint.Style.STROKE
                textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.typeface = tfTest
                modelTextPaint = Paint(textPaint)
                modelTextPaint.color = getColorEx(R.color.wheelview_text)
                versionPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                versionPaint.textAlign = Paint.Align.RIGHT
                versionPaint.color = getColorEx(R.color.wheelview_versiontext)
                versionPaint.typeface = tfTest
            }
            R.style.AJDMTheme -> {
                outerStrokeWidth = dpToPx(context, 30).toFloat()
                innerStrokeWidth = dpToPx(context, 25).toFloat()
                middleStrokeWidth = dpToPx(context, 10).toFloat()
                voltStrokeWidth = dpToPx(context, 5).toFloat()
                innerOuterPadding = dpToPx(context, 5).toFloat()
                innerTextPadding = 0f
                mediumInnerPadding = dpToPx(context, 5).toFloat()
                outerMediumPadding = dpToPx(context, 5).toFloat()
                boxMiddlePadding = dpToPx(context, 10).toFloat()
                boxTopPadding = dpToPx(context, 10).toFloat()
                boxOuterPadding = dpToPx(context, 10).toFloat()
                boxInnerPadding = dpToPx(context, 10).toFloat()
                outerArcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                outerArcPaint.isAntiAlias = true
                outerArcPaint.strokeWidth = outerStrokeWidth
                outerArcPaint.style = Paint.Style.STROKE
                innerArcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                innerArcPaint.isAntiAlias = true
                innerArcPaint.strokeWidth = innerStrokeWidth
                innerArcPaint.style = Paint.Style.STROKE
                middleArcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                middleArcPaint.isAntiAlias = true
                middleArcPaint.strokeWidth = middleStrokeWidth
                middleArcPaint.style = Paint.Style.STROKE
                voltArcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                voltArcPaint.isAntiAlias = true
                voltArcPaint.strokeWidth = voltStrokeWidth
                voltArcPaint.style = Paint.Style.STROKE
                textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                textPaint.textAlign = Paint.Align.CENTER
                textPaint.typeface = tfTest
                modelTextPaint = Paint(textPaint)
                modelTextPaint.color = getColorEx(R.color.wheelview_text)
                versionPaint = Paint(Paint.ANTI_ALIAS_FLAG)
                versionPaint.textAlign = Paint.Align.RIGHT
                versionPaint.color = getColorEx(R.color.wheelview_versiontext)
                versionPaint.typeface = tfTest
            }
        }
    }

    private val gestureDetector = GestureDetector(
        context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                WheelData.getInstance().adapter?.switchFlashlight()
                return super.onDoubleTap(e)
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (WheelLog.AppConfig.useBeepOnSingleTap) {
                    SomeUtil.playBeep(context)
                    return true
                }
                return super.onSingleTapConfirmed(e)
            }

            override fun onLongPress(e: MotionEvent) {
                super.onLongPress(e)
                val i = getBlockIndexBy(e.x, e.y)
                if (i == -1) {
                    return
                }
                val currentTitle = mViewBlocks.filter { it.enabled }[i].title
                val items = mViewBlocks.filter { !it.enabled }.map { block -> block.title }
                AlertDialog.Builder(context, R.style.OriginalTheme_Dialog_Alert)
                    .setIcon(R.drawable.ic_baseline_dashboard_customize_24)
                    .setTitle(
                        String.format(
                            context.getString(R.string.replace_info_block),
                            currentTitle
                        )
                    )
                    .setItems(items.toTypedArray()) { _, which ->
                        val title = items[which]
                        WheelLog.AppConfig.viewBlocksString =
                            WheelLog.AppConfig.viewBlocksString?.replace(currentTitle, title)
                        updateViewBlocksVisibility()
                        redrawTextBoxes()
                    }
                    .setCancelable(true)
                    .create()
                    .show()
                // Toast.makeText(context, "long press: " + e.x + " " + e.y + " | " + i, Toast.LENGTH_SHORT).show()
            }
        })

    init {
        if (isInEditMode) {
            currentTheme = R.style.OriginalTheme
            WheelLog.AppConfig = AppConfig(context)
            mSpeed = 380
            targetSpeed = (mSpeed.toFloat() / 500 * 112).roundToInt()
            currentSpeed = targetSpeed
            mCurrent = 15.0
            targetCurrent = (mSpeed.toFloat() / 500 * 112).roundToInt()
            currentCurrent = 15

            mTemperature = 35
            mMaxTemperature = 70
            targetTemperature = 112 - (40f / 80f * MathUtils.clamp(mTemperature, 0, 80)).roundToInt()
            currentTemperature = targetTemperature
            mBattery = 50
            mBatteryLowest = 30
            targetBatteryLowest = 10
            targetBattery = (40f / 100f * MathUtils.clamp(mBattery, 0, 100)).roundToInt()
            currentBattery = targetBattery
            mWheelModel = "GotInSong Z10"
            try {
                val wd = WheelData()
                val wdField = WheelData::class.java.getDeclaredField("mInstance")
                wdField.isAccessible = true
                wdField[null] = wd
                ReflectUtil.SetPrivateField(wd, "mCalculatedPwm", 0.05)
                ReflectUtil.SetPrivateField(wd, "mMaxPwm", 0.97)
                ReflectUtil.SetPrivateField(wd, "mConnectionState", true)
            } catch (ignored: Exception) {
            }
        } else {
            currentTheme = WheelLog.AppConfig.appTheme
        }

        useMph = WheelLog.AppConfig.useMph
        mViewBlocks = viewBlockInfo
        updateViewBlocksVisibility()
        onChangeTheme()
        setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }
}
