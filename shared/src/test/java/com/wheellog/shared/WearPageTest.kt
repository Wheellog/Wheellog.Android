package com.wheellog.shared

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

internal class WearPageTest {

    lateinit var pages: WearPages

    @Before
    fun setUp() {
        pages = WearPages.noneOf(WearPage::class.java)
    }

    @Test
    fun and() {
        // Arrange.
        // Act.
        pages = WearPage.Temperature and WearPage.Main

        // Assert.
        assertThat(pages.contains(WearPage.Temperature))
        assertThat(pages.contains(WearPage.Main))
        assertThat(!pages.contains(WearPage.PWM))
        assertThat(!pages.contains(WearPage.Distance))
    }

    @Test
    fun `and 3+`() {
        // Arrange.
        // Act.
        pages = WearPage.Temperature and WearPage.PWM and WearPage.Distance

        // Assert.
        assertThat(pages.contains(WearPage.Temperature))
        assertThat(pages.contains(WearPage.PWM))
        assertThat(pages.contains(WearPage.Distance))
    }

    @Test
    fun serialize() {
        // Arrange.
        pages = WearPage.Temperature and WearPage.Distance and WearPage.PWM

        // Act.
        val serialized = pages.serialize()

        // Assert.
        assertThat(serialized).isNotEmpty()
        assertThat(serialized).contains(WearPage.Temperature.toString())
        assertThat(serialized).contains(WearPage.Distance.toString())
        assertThat(serialized).contains(WearPage.PWM.toString())
    }

    @Test
    fun deserialize() {
        // Arrange.
        pages = WearPage.Temperature and WearPage.Distance
        val serialized = pages.serialize()

        // Act.
        val deserialized = WearPage.deserialize(serialized)

        // Assert.
        assertThat(deserialized.contains(WearPage.Temperature))
        assertThat(!deserialized.contains(WearPage.PWM))
        assertThat(deserialized.contains(WearPage.Distance))
    }
}
