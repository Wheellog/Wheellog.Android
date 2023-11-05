package com.cooper.wheellog.utils.gotway

import com.cooper.wheellog.AppConfig
import io.mockk.every
import io.mockk.mockkClass
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

class GotwayScaledVoltageCalculatorTest {

    private lateinit var calculator: GotwayScaledVoltageCalculator
    private val appConfig = mockkClass(AppConfig::class, relaxed = true)

    @Before
    fun setUp() {
        calculator = GotwayScaledVoltageCalculator(appConfig)
    }

    @Test
    fun testGetScaledVoltage_default() {
        appConfig.gotwayVoltage = "" // Assuming this method sets the voltage to default value
        assertEquals(100.0, calculator.getScaledVoltage(100.0), 0.001)
    }

    @Test
    fun testGetScaledVoltage_case1() {
        every { appConfig.gotwayVoltage } returns "1"
        assertEquals(125.0, calculator.getScaledVoltage(100.0), 0.001)
    }

    @Test
    fun testGetScaledVoltage_case2() {
        every { appConfig.gotwayVoltage } returns "2"
        assertEquals(150.0, calculator.getScaledVoltage(100.0), 0.001)
    }

    @Test
    fun testGetScaledVoltage_case3() {
        every { appConfig.gotwayVoltage } returns "3"
        assertEquals(173.8095238095238, calculator.getScaledVoltage(100.0), 0.001)
    }

    @Test
    fun testGetScaledVoltage_case4() {
        every { appConfig.gotwayVoltage } returns "4"
        assertEquals(200.0, calculator.getScaledVoltage(100.0), 0.001)
    }
}
