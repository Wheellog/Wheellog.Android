package com.cooper.wheellog.utils

import androidx.annotation.StringRes
import com.cooper.wheellog.R

enum class ThemeEnum(val value: Int, @StringRes val title: Int) {
    Original(0, R.string.theme_standard),
    Contrast(2, R.string.theme_contrast),
    Inferno(3, R.string.theme_inferno),
    AJDM(1, R.string.theme_ajdm),
    Porcelain(4, R.string.theme_porcelain),
    Polar(5, R.string.theme_polar),
    Jade(6, R.string.theme_jade),
    Steel(7, R.string.theme_steel),
    Heather(8, R.string.theme_heather);

    companion object {
        fun fromInt(value: Int) = values().firstOrNull { it.value == value } ?: Original
    }
}
