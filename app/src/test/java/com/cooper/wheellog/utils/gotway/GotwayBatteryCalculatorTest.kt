package com.cooper.wheellog.utils.gotway

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.roundToInt

class GotwayBatteryCalculatorTest {

    private val calculator = GotwayBatteryCalculator()

    // Test cases for useBetterPercents = true
    @Test
    fun testGetBattery_useBetterPercents_highVoltage() {
        assertEquals(100, calculator.getBattery(useBetterPercents = true, voltage = 6700))
    }

    @Test
    fun testGetBattery_useBetterPercents_mediumVoltage() {
        assertEquals(
            ((6600 - 5380) / 13),
            calculator.getBattery(useBetterPercents = true, voltage = 6600),
        )
    }

    @Test
    fun testGetBattery_useBetterPercents_lowMediumVoltage() {
        assertEquals(
            (((5300 - 5290) / 32.5).roundToInt()),
            calculator.getBattery(useBetterPercents = true, voltage = 5300),
        )
    }

    @Test
    fun testGetBattery_useBetterPercents_lowVoltage() {
        assertEquals(0, calculator.getBattery(useBetterPercents = true, voltage = 5280))
    }

    // Test cases for useBetterPercents = false
    @Test
    fun testGetBattery_standardPercents_lowVoltage() {
        assertEquals(0, calculator.getBattery(useBetterPercents = false, voltage = 5280))
    }

    @Test
    fun testGetBattery_standardPercents_highVoltage() {
        assertEquals(100, calculator.getBattery(useBetterPercents = false, voltage = 6600))
    }

    @Test
    fun testGetBattery_standardPercents_mediumVoltage() {
        assertEquals(
            ((5400 - 5290) / 13),
            calculator.getBattery(useBetterPercents = false, voltage = 5400),
        )
    }

    // Edge cases
    @Test
    fun testGetBattery_edgeCase_exactLowBoundary() {
        assertEquals(0, calculator.getBattery(useBetterPercents = true, voltage = 5290))
    }

    @Test
    fun testGetBattery_edgeCase_exactHighBoundary() {
        assertEquals(100, calculator.getBattery(useBetterPercents = false, voltage = 6580))
    }
}
