package com.cooper.wheellog.utils.gotway

import com.cooper.wheellog.AppConfig
import com.cooper.wheellog.WheelData
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class GotwayFrameBDecoderTest {

    private lateinit var wheelData: WheelData
    private lateinit var appConfig: AppConfig
    private lateinit var gotwayFrameBDecoder: GotwayFrameBDecoder

    @Before
    fun setUp() {
        wheelData = mockk(relaxed = true)
        appConfig = mockk(relaxed = true)
        gotwayFrameBDecoder = GotwayFrameBDecoder(wheelData, appConfig)
    }

    @Test
    fun `When decoding frame B, Wheel Data and AppConfig are updated`() {
        // Given
        val buff = ByteArray(16)
        val useRatio = true
        val lock = 0
        val firmwareVersion = "1.0.0"

        // When
        gotwayFrameBDecoder.decode(buff, useRatio, lock, firmwareVersion)

        // Then
        verify { wheelData.totalDistance = any() }
        verify { appConfig.pedalsMode = any() }
        verify { appConfig.alarmMode = any() }
        verify { appConfig.lightMode = any() }
        verify { appConfig.ledMode = any() }

        confirmVerified(wheelData)
    }

    // todo test decode frame b
}
