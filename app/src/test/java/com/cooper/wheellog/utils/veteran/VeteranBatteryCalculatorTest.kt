package com.cooper.wheellog.utils.veteran

import org.junit.Test

class VeteranBatteryCalculatorTest {

    // TODO battery calculator for high voltage wheels
    private val calculator: VeteranBatteryCalculator = VeteranBatteryCalculator()

    @Test
    fun `test that a Patton at 125V shows 100 percent without using accurate percent`() {
        val voltage = 12500 // 125 V
        val version = 4 // patton
        val percentage = calculator.calculateBattery(voltage, version, false)
        assert(percentage == 100)
    }

    @Test
    fun `test that a Patton at 125V shows 99 percent using accurate percent`() {
        val voltage = 12500 // 125 V
        val version = 4 // patton
        val percentage = calculator.calculateBattery(voltage, version, true)
        assert(percentage == 99)
    }

    @Test
    fun `test that a Patton at 126V shows 100 percent without using accurate percent`() {
        val voltage = 12600 // 126 V
        val version = 4 // patton
        val percentage = calculator.calculateBattery(voltage, version, false)
        assert(percentage == 100)
    }

    @Test
    fun `test that a Patton at 126V shows 100 percent using accurate percent`() {
        val voltage = 12600 // 126 V
        val version = 4 // patton
        val percentage = calculator.calculateBattery(voltage, version, true)
        assert(percentage == 100)
    }

    @Test
    fun `test that a Sherman shows 100 percent without using accurate percent`() {
        val voltage = 10020 // 100.2 Volts is max for Sherman
        val version = 3 // Versions higher than this are patton
        val percentage = calculator.calculateBattery(voltage, version, false)
        assert(percentage == 100)
    }

    @Test
    fun `test that a Sherman at 100V shows 99 percent using accurate percent when 2 tenths of a percent is left to charge`() {
        val voltage = 10000 // 100 V
        val version = 3 // Versions higher than this are patton
        val percentage = calculator.calculateBattery(voltage, version, true)
        assert(percentage == 99)
    }

    @Test
    fun `test that a Patton at 100 V percent is down to 6 percent using accurate percent calculation`() {
        val voltage = 10000 // 100 V
        val version = 4 // Versions higher than this are patton
        val percentage = calculator.calculateBattery(voltage, version, true)
        assert(percentage == 6)
    }

    @Test
    fun `test that a Patton at 100 V percent is down to 3 percent without using accurate percent`() {
        val voltage = 10000 // 100 V
        val version = 4 // Versions higher than this are patton
        val percentage = calculator.calculateBattery(voltage, version, false)
        assert(percentage == 3)
    }
}
