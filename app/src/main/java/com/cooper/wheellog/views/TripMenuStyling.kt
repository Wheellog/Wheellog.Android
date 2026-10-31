package com.cooper.wheellog.views

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.Menu
import com.cooper.wheellog.map.tripMutedColor
import com.cooper.wheellog.map.tripTextColor
import com.cooper.wheellog.map.tripIconColor

/** The menu uses settings text colors and a consistent accent for every icon. */
internal fun styleTripMenu(context: Context, menu: Menu) {
    val accent = context.tripIconColor()
    for (index in 0 until menu.size()) {
        val item = menu.getItem(index)
        item.title = SpannableString(item.title.toString()).apply {
            setSpan(ForegroundColorSpan(if (item.isEnabled) context.tripTextColor() else context.tripMutedColor()),
                0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        item.icon = item.icon?.mutate()?.apply {
            setTint(accent)
            alpha = if (item.isEnabled) 255 else 96
        }
    }
}
