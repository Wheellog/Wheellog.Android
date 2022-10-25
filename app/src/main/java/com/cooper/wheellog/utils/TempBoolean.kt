package com.cooper.wheellog.utils

class TempBoolean {
    private var timeSetValue: Long = 0
    private var setValue: Boolean = false

    var timeToResetToDefault: Long = 200
    var defaultValue: Boolean = false

    var value: Boolean
        get() {
            return if (System.currentTimeMillis() - timeSetValue < timeToResetToDefault) {
                setValue
            } else {
                defaultValue
            }
        }
        set(value) {
            timeSetValue = System.currentTimeMillis()
            setValue = value
        }
}