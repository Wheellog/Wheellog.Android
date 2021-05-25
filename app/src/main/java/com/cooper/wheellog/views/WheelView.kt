package com.cooper.wheellog.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.AbstractComposeView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.cooper.wheellog.R
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.WheelLog
import com.cooper.wheellog.utils.SomeUtil.Companion.getColorEx

@SuppressLint("ClickableViewAccessibility")
class WheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {
    var mSpeedDouble = WheelData.getInstance().speedDouble

    @Composable
    override fun Content() {
        val speed = remember { mutableStateOf(mSpeedDouble) }
        val batteryLevel = remember { mutableStateOf(WheelData.getInstance().batteryLevel) }
        val temperature = remember { mutableStateOf(WheelData.getInstance().temperature) }
        DataArcs(
            modifier = Modifier.fillMaxWidth(),
            mainArc = speed.value.toFloat(),
            leftArc = batteryLevel.value.toFloat(),
            rightArc = temperature.value.toFloat()
        )
    }

    @Composable
    private fun DataArcs(
        modifier: Modifier,
        mainArc: Float,
        leftArc: Float,
        rightArc: Float
    ) {
        Canvas(modifier) {
            /*
             * Drawing the main arc
             */

            // Some variables
            val mainArcSize = if (size.width > size.height) size.height else size.width
            val mainArcStroke = Stroke(width = mainArcSize / 8)

            // Calculation of arc sizes
            val mainArcSweepAngle = 252f * (mainArc / WheelLog.AppConfig.maxSpeed)

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
            // Foreground
            drawArc(
                color = Color(getColorEx(R.color.wheelview_main_positive_dial)),
                144f,
                mainArcSweepAngle,
                false,
                topLeft = Offset(mainArcStroke.width / 2, mainArcStroke.width / 2),
                size = Size(mainArcSize - mainArcStroke.width, mainArcSize - mainArcStroke.width),
                style = mainArcStroke
            )
        }
    }

    @Composable
    private fun InfoBlock(label: String, value: String) {

    }

    fun resetBatteryLowest() {

    }
}
