package com.cooper.wheellog.utils

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer

class StringUtilTest {
    @Test
    fun `deleteFirstSentence`() {
        // Arrange.
        val text = "%s%n%s%n%s".format("bla1", "bla2", "bla3")
        val expected = "%s%n%s".format("bla2", "bla3")

        // Act.
        val withoutFirstSentence = StringUtil.deleteFirstSentence(text)

        // Assert.
        assertThat(withoutFirstSentence).isEqualTo(expected)
    }

    @Test
    fun `isCorrectMac`() {
        // Arrange.
        val mac1 = "00:30:48:5a:58:65"
        val mac2 = "00:30:48:5a:58:655"
        val mac3 = "00:30:GG:5a:58:65"
        val mac4 = "00:30:45"
        val mac5 = "AA:BB:11:22:33:00"
        val mac6 = "AA-BB-CC-DD-EE-FF"

        // Act.
        val mac1Check = StringUtil.isCorrectMac(mac1)
        val mac2Check = StringUtil.isCorrectMac(mac2)
        val mac3Check = StringUtil.isCorrectMac(mac3)
        val mac4Check = StringUtil.isCorrectMac(mac4)
        val mac5Check = StringUtil.isCorrectMac(mac5)
        val mac6Check = StringUtil.isCorrectMac(mac6)

        // Assert.
        assertThat(mac1Check).isEqualTo(true)
        assertThat(mac2Check).isEqualTo(false)
        assertThat(mac3Check).isEqualTo(false)
        assertThat(mac4Check).isEqualTo(false)
        assertThat(mac5Check).isEqualTo(true)
        assertThat(mac6Check).isEqualTo(true)
    }
}
