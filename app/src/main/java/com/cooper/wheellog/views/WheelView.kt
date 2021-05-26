package com.cooper.wheellog.views

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.AbstractComposeView
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.SomeUtil.Companion.getColorEx

class WheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {
    var mSpeedDouble = mutableStateOf(WheelData.getInstance().speedDouble)


    @Composable
    override fun Content() {
        val speed = remember { mSpeedDouble }
        val batteryLevel = remember { mutableStateOf(WheelData.getInstance().batteryLevel.toFloat()) }
        val temperature = remember { mutableStateOf(WheelData.getInstance().temperature.toFloat()) }
        DataArcs(
            modifier = Modifier.fillMaxWidth(),
            mainArc = speed,
            leftArc = batteryLevel,
            rightArc = temperature
        )
    }

    @Composable
    private fun DataArcs(
        modifier: Modifier,
        mainArc: MutableState<Double>,
        leftArc: MutableState<Float>,
        rightArc: MutableState<Float>,
    ) {
        Canvas(modifier) {
            /*
             * Drawing the main arc
             */

            // Some variables
            val mainArcSize = if (size.width > size.height) size.height else size.width
            val mainArcStroke = Stroke(width = mainArcSize / 8)

            // Calculation of arc sizes
            val mainArcSweepAngle = 252f * (mainArc.value / WheelLog.AppConfig.maxSpeed)

            // Background
            drawArc(
                color = Color(getColorEx(R.color.wheelview_arc_dim)),
                144f,
                252f,
                false,
                topLeft = Offset(mainArcStroke.width / 2, mainArcStroke.width / 2),
                size = Size(mainArcSize - mainArcStroke.width, mainArcSize - mainArcStroke.width),
                style = mainArcStroke
            )
            // Foreground content
            drawArc(
                color = Color(getColorEx(R.color.wheelview_main_positive_dial)),
                144f,
                mainArcSweepAngle.toFloat(),
                false,
                topLeft = Offset(mainArcStroke.width / 2, mainArcStroke.width / 2),
                size = Size(mainArcSize - mainArcStroke.width, mainArcSize - mainArcStroke.width),
                style = mainArcStroke
            )
            // And foreground marks
        }
    }

    @Composable
    private fun InfoBlock(label: String, value: String) {

    }

    // Later this function will be replaced by ViewModel
    // and Kotlin Flow, so this view will update by itself,
    // without external update calls
    fun refresh() {
        mSpeedDouble.value = WheelData.getInstance().speedDouble
    }

    fun resetBatteryLowest() {

    }
}
