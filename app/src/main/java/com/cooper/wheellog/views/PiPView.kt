package com.cooper.wheellog.views

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.lifecycle.ViewModel
import com.cooper.wheellog.R
import com.cooper.wheellog.utils.MathsUtil

class PiPView {

    class SpeedModel : ViewModel() {
        var value = mutableFloatStateOf(0f)
        var maxValue = mutableFloatStateOf(100f)
        var title = "speed"
    }

    @OptIn(ExperimentalTextApi::class)
    @Preview(widthDp = 160, heightDp = 90, showBackground = true)
    @Composable
    fun SpeedWidget(
        modifier: Modifier = Modifier,
        model: SpeedModel = SpeedModel(),
    ) {
        val textMeasure = rememberTextMeasurer()
        val textStyle = TextStyle(
            color = Color.White,
            fontFamily = primeFontFamily,
            shadow = Shadow(
                color = Color.LightGray,
                blurRadius = 5f
            )
        )

        val colorMain = colorResource(R.color.wheelview_main_positive_dial)
        val colorDim = colorResource(R.color.wheelview_arc_dim)
        val arcPercent = MathsUtil.clamp(model.value.value / model.maxValue.value, 0f, 1f)

        val animatedPercent by animateFloatAsState(
            targetValue = arcPercent,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        val animatedValue by animateFloatAsState(
            targetValue = model.value.value,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        val valueString = animatedValue.toInt().toString()

        Canvas(modifier.padding(5.dp)) {
            val mainArcSize = if (size.width > size.height) size.height else size.width
            val leftOffset = (size.width - mainArcSize) / 2
            val mainArcStroke = Stroke(width = mainArcSize / 7)
            val mainArcStrokeIn = Stroke(width = mainArcStroke.width * 0.8f)
            val mainArcStrokeIn2 = Stroke(width = mainArcStroke.width * 2f)

            val scale = mainArcSize / 100F
            val titleStyle = textStyle.copy(fontSize = TextUnit(5F * scale, TextUnitType.Sp))
            val valueStyle = textStyle.copy(fontSize = TextUnit(18F * scale, TextUnitType.Sp))

            drawArc(
                color = colorDim,
                140f,
                220f,
                false,
                topLeft = Offset(leftOffset + mainArcStroke.width / 2, mainArcStroke.width / 2),
                size = Size(mainArcSize - mainArcStroke.width, mainArcSize - mainArcStroke.width),
                style = mainArcStroke
            )

            drawArc(
                color = colorMain,
                141f,
                218f * animatedPercent,
                false,
                topLeft = Offset(leftOffset + mainArcStroke.width / 2, mainArcStroke.width / 2),
                size = Size(
                    mainArcSize - mainArcStroke.width,
                    mainArcSize - mainArcStroke.width
                ),
                style = mainArcStrokeIn
            )

            drawArc(
                color = Color.White,
                141f + 218f * animatedPercent,
                2f,
                false,
                topLeft = Offset(leftOffset + mainArcStroke.width / 2, mainArcStroke.width / 2),
                size = Size(
                    mainArcSize - mainArcStroke.width,
                    mainArcSize - mainArcStroke.width
                ),
                style = mainArcStrokeIn2
            )

            val valueTextSize =
                textMeasure.measure(text = AnnotatedString(valueString), valueStyle).size
            drawText(
                textMeasurer = textMeasure,
                text = valueString,
                maxLines = 1,
                style = valueStyle,
                topLeft = Offset(
                    leftOffset + (mainArcSize - valueTextSize.width) / 2F,
                    (mainArcSize - valueTextSize.height) / 2F
                )
            )

            val titleTextSize =
                textMeasure.measure(text = AnnotatedString(model.title), titleStyle).size
            drawText(
                textMeasurer = textMeasure,
                text = model.title,
                maxLines = 1,
                style = titleStyle,
                topLeft = Offset(
                    leftOffset + (mainArcSize - titleTextSize.width) / 2F,
                    mainArcSize / 1.2F - titleTextSize.height / 2F
                )
            )
        }
    }

    private val primeFontFamily = FontFamily(
        Font(R.font.prime, FontWeight.Normal)
    )
}