package com.cooper.wheellog.utils.kingsong

import com.cooper.wheellog.WheelData
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KingSongLiveDataDecoderTest {

    private lateinit var wd: WheelData
    private lateinit var kingSongBatteryCalculator: KingSongBatteryCalculator
    private lateinit var decoder: KingSongLiveDataDecoder

    @BeforeEach
    fun setUp() {
        wd =
            mockk(relaxed = true) // `relaxed = true` allows for all methods to be stubbed with default answers
        kingSongBatteryCalculator = mockk(relaxed = true)
        decoder = KingSongLiveDataDecoder(wd, kingSongBatteryCalculator)
    }

    @Test
    fun testDecode() {
        val data =
            ByteArray(20) // The size should cover all the indices that are read by the decode method.

        // Populate with test data
        data[2] = 0x34 // 52 as a low byte of voltage
        data[3] =
            0x12 // 18 as a high byte of voltage (big-endian representation of 18*256 + 52 = 4660)
        data[4] = 0x78 // 120 as a low byte of speed
        data[5] = 0x56 // 86 as a high byte of speed
        // Total distance, assuming 4 bytes are used
        data[6] = 0x01
        data[7] = 0x02
        data[8] = 0x03
        data[9] = 0x04 // Represents an arbitrary 4-byte integer
        // Current, 2 bytes, little-endian
        data[10] = 0x50
        data[11] = 0x60
        // Temperature, 2 bytes, big-endian
        data[12] = 0x7B // 123 as a low byte of temperature
        data[13] = 0x00 // 0 as a high byte of temperature
        // Mode-related bytes
        data[14] = 0x03 // Mode 3
        data[15] = 0xE0.toByte() // Sentinel value indicating mode information is available

        // The rest of the data can remain zero or can be set to other dummy values depending on additional tests

        val m18Lkm = false
        val mode = 0

        // Mock the responses from the WheelData
        every { wd.getModel() } returns "KS-18L"

        // Assume we have predefined values in the byte array
        every { wd.setVoltage(any()) } just Runs
        every { wd.setSpeed(any()) } just Runs
        every { wd.setTotalDistance(any()) } just Runs
        every { wd.setCurrent(any()) } just Runs
        every { wd.setTemperature(any()) } just Runs
        every { wd.setVoltageSag(any()) } just Runs
        every { kingSongBatteryCalculator.calculateAndStoreBatteryLevel(any()) } just Runs

        // Call the method under test
        val returnedMode = decoder.decode(data, m18Lkm, mode)

        // Assertions and Verifications
        verify { wd.setVoltage(any()) }
        verify { wd.setSpeed(any()) }
        verify { wd.setTotalDistance(any()) }
        verify { wd.setCurrent(any()) }
        verify { wd.setTemperature(any()) }
        verify { wd.setVoltageSag(any()) }
        verify { kingSongBatteryCalculator.calculateAndStoreBatteryLevel(any()) }

        // You can also verify the returned mode and other specifics if necessary
        assertEquals(returnedMode, 3, "Mode should be correct")

        // Add more assertions and verifications as needed to cover the functionality.
    }
}
