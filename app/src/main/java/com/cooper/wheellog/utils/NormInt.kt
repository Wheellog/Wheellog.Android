package com.cooper.wheellog.utils

class NormInt {
    private var values: MutableList<Int> = mutableListOf()
    private var averageIgnoreCounter: Int = 0

    var length: Int = 5
    var averageDiff: Int = 30
    var averageDiffIgnore: Int = 10
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
        if (value > average + averageDiff || value < average - averageDiff) {
            averageIgnoreCounter++
            if (averageIgnoreCounter > averageDiffIgnore) {
                values.clear()
                values.add(value)
                return true
            }
            return false
        }
        values.removeAt(0)
        values.add(value)
        return true
    }
}