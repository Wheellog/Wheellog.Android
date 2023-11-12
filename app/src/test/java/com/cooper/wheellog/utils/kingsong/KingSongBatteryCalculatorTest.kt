package com.cooper.wheellog.utils.kingsong

import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.WheelData
import com.cooper.wheellog.utils.kingsong.KingsongUtils.is100vWheel
import com.cooper.wheellog.utils.kingsong.KingsongUtils.is126vWheel
import com.cooper.wheellog.utils.kingsong.KingsongUtils.is84vWheel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class KingSongBatteryCalculatorTest {

    private lateinit var wd: WheelData
    private lateinit var appConfig: AppConfig
    private lateinit var calculator: KingSongBatteryCalculator

    @Before
    fun setUp() {
        wd = mockk(relaxed = true)
        appConfig = mockk(relaxed = true)
        calculator = KingSongBatteryCalculator(wd, appConfig)
    }

    @Test
    fun `calculateAndStoreBatteryLevel sets correct battery level for 84vWheel with better percents`() {
        every { is84vWheel(wd) } returns true
        every { is100vWheel(wd) } returns false
        every { is126vWheel(wd) } returns false
        every { wd.btName } returns "KS-16X"
        every { wd.model } returns "KS-16X"
        every { appConfig.useBetterPercents } returns true
        val voltage = 8400

        // Act
        calculator.calculateAndStoreBatteryLevel(voltage)

        // Assert
        verify { wd.batteryLevel = 100 }
    }

    @Test
    fun `calculateAndStoreBatteryLevel sets correct battery level for 84vWheel without better percents`() {
        every { is84vWheel(wd) } returns true
        every { is100vWheel(wd) } returns false
        every { is126vWheel(wd) } returns false
        every { wd.model } returns "KS-16X"
        every { wd.btName } returns "KS-16X"
        every { appConfig.useBetterPercents } returns false
        val voltage = 6300

        calculator.calculateAndStoreBatteryLevel(voltage)

        verify { wd.batteryLevel = 2 }
    }

    @Test
    fun `calculateAndStoreBatteryLevel sets correct battery level for 100vWheel with better percents`() {
        every { is84vWheel(wd) } returns false
        every { is100vWheel(wd) } returns true
        every { is126vWheel(wd) } returns false
        every { wd.model } returns "KS-S19"
        every { wd.btName } returns "KS-S19"
        every { appConfig.useBetterPercents } returns true
        val voltage = 10100

        calculator.calculateAndStoreBatteryLevel(voltage)

        verify { wd.batteryLevel = 100 }
    }

    @Test
    fun `calculateAndStoreBatteryLevel sets correct battery level for 100vWheel without better percents`() {
        every { is84vWheel(wd) } returns false
        every { is100vWheel(wd) } returns true
        every { is126vWheel(wd) } returns false
        every { appConfig.useBetterPercents } returns false
        every { wd.model } returns "KS-S19"
        every { wd.btName } returns "KS-S19"
        val voltage = 7700

        calculator.calculateAndStoreBatteryLevel(voltage)

        verify { wd.batteryLevel = 8 }
    }

    @Test
    fun `calculateAndStoreBatteryLevel sets correct battery level for 126vWheel with better percents`() {
        every { is126vWheel(wd) } returns true
        every { is100vWheel(wd) } returns false
        every { is84vWheel(wd) } returns false
        every { wd.model } returns "KS-S20"
        every { wd.btName } returns "KS-S20"
        every { appConfig.useBetterPercents } returns true
        val voltage = 12300

        calculator.calculateAndStoreBatteryLevel(voltage)

        verify { wd.batteryLevel = 91 }
    }

    @Test
    fun `calculateAndStoreBatteryLevel sets correct battery level for 126vWheel without better percents`() {
        every { is84vWheel(wd) } returns false
        every { is100vWheel(wd) } returns false
        every { is126vWheel(wd) } returns true
        every { wd.model } returns "KS-S20"
        every { wd.btName } returns "KS-S20"
        every { appConfig.useBetterPercents } returns false
        val voltage = 9500

        calculator.calculateAndStoreBatteryLevel(voltage)

        verify { wd.batteryLevel = 4 }
    }

    @Test
    fun `calculateAndStoreBatteryLevel sets correct battery level for non-standard voltage with better percents`() {
        every { is84vWheel(wd) } returns false
        every { is126vWheel(wd) } returns false
        every { is100vWheel(wd) } returns false
        every { appConfig.useBetterPercents } returns true
        every { wd.model } returns ""
        every { wd.btName } returns ""
        val voltage = 5600

        calculator.calculateAndStoreBatteryLevel(voltage)

        verify { wd.batteryLevel = 21 }
    }

    @Test
    fun `calculateAndStoreBatteryLevel sets correct battery level for non-standard voltage without better percents`() {
        every { is84vWheel(wd) } returns false
        every { is126vWheel(wd) } returns false
        every { is100vWheel(wd) } returns false
        every { wd.model } returns ""
        every { wd.btName } returns ""
        every { appConfig.useBetterPercents } returns false
        val voltage = 5100

        calculator.calculateAndStoreBatteryLevel(voltage)

        verify { wd.batteryLevel = 6 }
    }
}
