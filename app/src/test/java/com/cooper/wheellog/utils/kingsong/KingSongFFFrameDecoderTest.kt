package com.cooper.wheellog.utils.kingsong

import com.cooper.wheellog.WheelData
import com.cooper.wheellog.utils.MathsUtil
import com.cooper.wheellog.utils.SmartBms
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import kotlin.math.roundToInt

class KingSongFFFrameDecoderTest {

    @Before
    fun setUp() {
        mockkStatic(MathsUtil::class)
    }

    @Test
    fun `decode sets bms values for packet number 0x00`() {
        // Arrange
        val wheelData = mockk<WheelData>(relaxed = true)
        val bms = mockk<SmartBms>(relaxed = true)
        val decoder = KingSongFFFrameDecoder(wheelData)
        val data = ByteArray(20)
        // Assuming packet number is 0x00
        data[16] = 0xF1.toByte() // BMS1 identifier
        data[17] = 0x00.toByte() // Packet number 0x00

        // Mock the getInt2R method for all the required indices
        every { MathsUtil.getInt2R(data, 2) } returns 4200 // Mocked Voltage
        every { MathsUtil.getInt2R(data, 4) } returns -150 // Mocked Current (could be negative)
        every { MathsUtil.getInt2R(data, 6) } returns 650 // Mocked Remaining Capacity
        every { MathsUtil.getInt2R(data, 8) } returns 100 // Mocked Factory Capacity
        every { MathsUtil.getInt2R(data, 10) } returns 200 // Mocked Full Cycles

        // Mocking BMS related methods
        every { wheelData.bms1 } returns bms
        every { bms.serialNumber } returns ""
        every { bms.factoryCap } returns 10000 // Mocked value for calculation

        // Act
        decoder.decode(data)

        // Assert
        // Verify that getInt2R was called with the correct parameters
        verify { MathsUtil.getInt2R(data, 2) }
        verify { MathsUtil.getInt2R(data, 4) }
        verify { MathsUtil.getInt2R(data, 6) }
        verify { MathsUtil.getInt2R(data, 8) }
        verify { MathsUtil.getInt2R(data, 10) }

        // Assert that the correct values are set on the bms object
        verify { bms.voltage = 42.0 } // 4200 / 1000
        verify { bms.current = -1.5 } // -1500 / 1000
        verify { bms.remCap = 6500 } // 650 * 10
        verify { bms.factoryCap = 1000 } // 10000 / 10
        verify { bms.fullCycles = 200 }

        // Assert the remaining percentage calculation
        val remPerc = (6500 / 1000.0 / 100).roundToInt()
        verify { bms.remPerc = remPerc }
    }

    @Test
    fun `decode sets BMS temperatures for packet number 0x01`() {
    }

    @Test
    fun `decode sets cell voltages for packet number 0x02`() {
    }

    @Test
    fun `decode sets cell voltages for packet number 0x03`() {
    }

    @Test
    fun `decode sets cell voltages for packet number 0x04`() {
    }

    @Test
    fun `decode sets cell voltages for packet number 0x05`() {
    }

    @Test
    fun `decode calculates cell differential for packet number 0x06`() {
    }
}
