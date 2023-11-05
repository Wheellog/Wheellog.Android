import com.cooper.wheellog.WheelData
import com.cooper.wheellog.utils.gotway.GotwayBatteryCalculator
import com.cooper.wheellog.utils.gotway.GotwayFrameADecoder
import com.cooper.wheellog.utils.gotway.GotwayScaledVoltageCalculator
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class GotwayFrameADecoderTest {

    private lateinit var wheelData: WheelData
    private lateinit var gotwayScaledVoltageCalculator: GotwayScaledVoltageCalculator
    private lateinit var gotwayBatteryCalculator: GotwayBatteryCalculator
    private lateinit var gotwayFrameADecoder: GotwayFrameADecoder

    @Before
    fun setUp() {
        wheelData = mockk(relaxed = true)
        gotwayScaledVoltageCalculator = mockk(relaxed = true)
        gotwayBatteryCalculator = mockk(relaxed = true)
        gotwayFrameADecoder =
            GotwayFrameADecoder(wheelData, gotwayScaledVoltageCalculator, gotwayBatteryCalculator)
    }

    @Test
    fun `test decode frame a updates wheel data outputs`() {
        // Given
        val buff = ByteArray(16)
        val useRatio = true
        val useBetterPercents = true
        val gotwayNegative = 1

        every { gotwayBatteryCalculator.getBattery(useBetterPercents, any()) } returns 75
        every { gotwayScaledVoltageCalculator.getScaledVoltage(any()) } returns 1000.toDouble()

        // When
        gotwayFrameADecoder.decode(buff, useRatio, useBetterPercents, gotwayNegative)

        // Then
        verify { gotwayBatteryCalculator.getBattery(useBetterPercents, any()) }
        verify { wheelData.speed = any() }
        verify { wheelData.topSpeed = any() }
        verify { wheelData.setWheelDistance(any()) }
        verify { wheelData.temperature = any() }
        verify { wheelData.phaseCurrent = any() }
        verify { wheelData.voltage = any() }
        verify { wheelData.voltageSag = any() }
        verify { wheelData.batteryLevel = any() }
        verify { wheelData.output = any() }
        verify { wheelData.updateRideTime() }

        // Ensure no other interactions
        confirmVerified(wheelData)
    }
}
