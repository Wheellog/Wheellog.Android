package com.cooper.wheellog.utils

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer

class MathUtilTest {

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
    }

    @Test
    fun `get byte array`() {
        // Arrange.
        val original2 = 6666.toShort()
        val original2Bytes = ByteBuffer.allocate(2).putShort(original2).array()
        val original4 = 1234567980
        val original4Bytes = ByteBuffer.allocate(4).putInt(original4).array()
        val mixed1 = original2Bytes + original4Bytes
        val mixed2 = original4Bytes + original2Bytes

        // Act.
        val result2 = MathsUtil.getInt2(original2Bytes, 0)
        val result4 = MathsUtil.getInt4(original4Bytes, 0)
        val result2m1 = MathsUtil.getInt2(mixed1, 0)
        val result2m2 = MathsUtil.getInt2(mixed2, 4)
        val result4m1 = MathsUtil.getInt4(mixed1, 2)
        val result4m2 = MathsUtil.getInt4(mixed2, 0)

        // Assert.
        assertThat(result2).isEqualTo(original2)
        assertThat(result4).isEqualTo(original4)
        assertThat(result2m1).isEqualTo(original2)
        assertThat(result2m2).isEqualTo(original2)
        assertThat(result4m1).isEqualTo(original4)
        assertThat(result4m2).isEqualTo(original4)
    }

    @Test
    fun `set byte array`() {
        // Arrange.
        val original2 = 6666.toShort()
        val original4 = 1234567980

        // Act.
        val int2 = MathsUtil.getBytes(original2)
        val int4 = MathsUtil.getBytes(original4)
        val result2 = MathsUtil.getInt2(int2, 0)
        val result4 = MathsUtil.getInt4(int4, 0)

        // Assert.
        assertThat(result2).isEqualTo(original2)
        assertThat(result4).isEqualTo(original4)
    }

    @Test
    fun `reverse bytes for KingSong`() {
        // Arrange.
        val original2 = 6666.toShort()
        val original4 = 1234567980
        val mixed = MathsUtil.getBytes(original2) + MathsUtil.getBytes(original4)

        // Act.
        val reversed = MathsUtil.reverseEvery2(mixed)
        val reversedBack = MathsUtil.reverseEvery2(reversed)

        // Assert.
        assertThat(mixed).isEqualTo(reversedBack)
        for (i in 0..mixed.size - 2 step 2) {
            assertThat(mixed[i]).isEqualTo(reversed[i + 1])
            assertThat(mixed[i + 1]).isEqualTo(reversed[i])
        }
    }
}
