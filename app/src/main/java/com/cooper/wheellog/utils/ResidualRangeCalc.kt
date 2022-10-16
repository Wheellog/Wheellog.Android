package com.cooper.wheellog.utils

import kotlin.math.roundToInt

/**
 * Остаточный пробег
 * нужно считать расход Voltage на км при разных скоростях
 * например на скорости до 10 км/ч расход один, на 25 км/ч другой
 */
class ResidualRangeCalc(private val criticalLowVoltage: Double) {
    private val dataSVM: MutableMap<Int, MillageVoltageData> = mutableMapOf()
    private val speedStep = 5
    private val maxSpeed = 100

    /**
     * Volts by kilometer
     */
    val consumption: MutableMap<Int, Double> = mutableMapOf()
    val residual: MutableMap<Int, Double> = mutableMapOf()

    fun updateData(speed: Double, voltage: Double, millage: Double) {
        val speedRounded5 = (speed / speedStep).roundToInt() * speedStep
        val svm = dataSVM[speedRounded5]
        if (svm != null) {
            svm.add(millage, voltage)
        } else {
            dataSVM[speedRounded5] = MillageVoltageData().apply { add(millage, voltage) }
        }
    }

    fun calc() {
        for (speed in 0..maxSpeed step speedStep) {
            consumption[speed] = dataSVM[speed]?.getConsumption() ?: 0.0
            residual[speed] = dataSVM[speed]?.getResidualRange(criticalLowVoltage) ?: 0.0
        }
    }
}

class MillageVoltageData {
    private var millageMax = Double.MIN_VALUE
    private var millageMin = Double.MAX_VALUE
    private var voltageMax = Double.MIN_VALUE
    private var voltageMin = Double.MAX_VALUE
    private val softeningRatio = 4.0

    private fun millage(): Double {
        return millageMax - millageMin
    }

    private fun voltageSpent(): Double {
        return voltageMax - voltageMin
    }

    fun add(millage: Double, voltage: Double) {
        millageMax = millageMax.coerceAtLeast(millage)
        millageMin = millageMin.coerceAtMost(millage)
        voltageMax = voltageMax.coerceAtLeast(voltage)
        if (voltageMin > voltageMax) {
            voltageMin = voltageMax
        }
        if (voltageMin > voltage) {
            voltageMin -= (voltageMin - voltage) / softeningRatio
        }
    }

    fun isReady(): Boolean {
        return millage() >= 100
    }

    fun getResidualRange(lowVoltage: Double): Double {
        if (!isReady()) {
            return 0.0
        }
        val voltageOff = voltageMin - lowVoltage
        val consumption = getConsumption()
        return if (consumption > 0.00001) {
            1000 * voltageOff / consumption
        } else {
            0.0
        }
    }

    fun getConsumption(): Double {
        return if (!isReady()) {
            0.0
        } else {
            voltageSpent() / millage() * 1000
        }
    }
}