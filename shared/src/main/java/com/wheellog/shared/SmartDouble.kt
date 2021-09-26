package com.wheellog.shared

class SmartDouble: Comparable<SmartDouble> {
    var value: Double = 0.0
        set(value) {
            field = value
            max = maxOf(value, max)
            min = minOf(value, min)
        }

    var max: Double = Double.MIN_VALUE
    var min: Double = Double.MAX_VALUE

    override fun toString(): String {
        return value.toString()
    }

    override fun compareTo(other: SmartDouble): Int {
        return value.compareTo(other.value) and
                max.compareTo(other.max) and
                min.compareTo(other.min)
    }
}