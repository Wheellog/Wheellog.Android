package com.cooper.wheellog.utils.kingsong

import com.cooper.wheellog.WheelData
import com.cooper.wheellog.utils.StringUtil
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test

class KingSongUtilsTest {

    private lateinit var wheelData: WheelData

    @Before
    fun setUp() {
        wheelData = mockk(relaxed = true)
        mockkStatic(StringUtil::class)
    }

    @Test
    fun `test is84vWheel with KS-18L model returns true`() {
        every { wheelData.model } returns "KS-18L"
        val result = KingsongUtils.is84vWheel(wheelData)
        assertThat(result).isTrue()
    }

    @Test
    fun `test is100vWheel with KS-S19 model returns true`() {
        every { wheelData.model } returns "KS-S19"
        val result = KingsongUtils.is100vWheel(wheelData)
        assertThat(result).isTrue()
    }

    @Test
    fun `test is126vWheel with KS-S20 model returns true`() {
        every { wheelData.model } returns "KS-S20"
        val result = KingsongUtils.is126vWheel(wheelData)
        assertThat(result).isTrue()
    }

    @Test
    fun `test is84vWheel with unknown model returns false`() {
        every { wheelData.model } returns "UNKNOWN"
        val result = KingsongUtils.is84vWheel(wheelData)
        assertThat(result).isFalse()
    }

    @Test
    fun `test is84vWheel with KS-16X model returns true`() {
        every { wheelData.model } returns "KS-16X"
        val result = KingsongUtils.is84vWheel(wheelData)
        assertThat(result).isTrue()
    }

    @Test
    fun `test is84vWheel with name starting with ROCKW returns true`() {
        every { wheelData.name } returns "ROCKWHEEL-GT16"
        val result = KingsongUtils.is84vWheel(wheelData)
        assertThat(result).isTrue()
    }

    @Test
    fun `test is84vWheel with btName RW returns true`() {
        every { wheelData.btName } returns "RW"
        val result = KingsongUtils.is84vWheel(wheelData)
        assertThat(result).isTrue()
    }

    // Tests for is100vWheel with various conditions
    @Test
    fun `test is100vWheel with incorrect model returns false`() {
        every { wheelData.model } returns "KS-16S"
        val result = KingsongUtils.is100vWheel(wheelData)
        assertThat(result).isFalse()
    }

    // Tests for is126vWheel with various conditions
    @Test
    fun `test is126vWheel with KS-S22 model returns true`() {
        every { wheelData.model } returns "KS-S22"
        val result = KingsongUtils.is126vWheel(wheelData)
        assertThat(result).isTrue()
    }

    @Test
    fun `test is126vWheel with incorrect model returns false`() {
        every { wheelData.model } returns "KS-14D"
        val result = KingsongUtils.is126vWheel(wheelData)
        assertThat(result).isFalse()
    }

    // Additional tests for robustness
    @Test
    fun `test is84vWheel with name not starting with ROCKW returns false`() {
        every { wheelData.name } returns "KingSong-14C"
        val result = KingsongUtils.is84vWheel(wheelData)
        assertThat(result).isFalse()
    }

    @Test
    fun `test is84vWheel with btName not RW returns false`() {
        every { wheelData.btName } returns "KS"
        val result = KingsongUtils.is84vWheel(wheelData)
        assertThat(result).isFalse()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }
}
