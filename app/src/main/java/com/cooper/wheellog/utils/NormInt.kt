package com.cooper.wheellog.utils

class NormInt {
    private var values: MutableList<Int> = mutableListOf()

    var length: Int = 5
    var maxDiff: Int = 30
    var min: Int = Int.MIN_VALUE
    var max: Int = Int.MAX_VALUE

    fun push(value: Int): Boolean {
        if (value < min || value > max) {
            return false
        }
        if (values.size < length) {
            values.add(value)
            return true
        }
        val average = values.average()
        if (value > average + maxDiff || value < average - maxDiff) {
            return false
        }
        values.removeAt(0)
        values.add(value)
        return true
    }
}