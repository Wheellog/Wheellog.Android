package com.cooper.wheellog.views

import android.view.View

abstract class DoubleClickListener : View.OnClickListener {
    private val timeDelta: Long = 300 //milliseconds
    var lastClickTime: Long = 0

    override fun onClick(v: View) {
        val clickTime = System.currentTimeMillis()
        if (clickTime - lastClickTime < timeDelta) {
            onDoubleClick(v)
        } else {
            onSingleClick(v)
        }
        lastClickTime = clickTime
    }

    abstract fun onSingleClick(v: View)

    abstract fun onDoubleClick(v: View)
}