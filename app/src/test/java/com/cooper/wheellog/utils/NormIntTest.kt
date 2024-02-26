package com.cooper.wheellog.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NormIntTest {

    @Test
    fun normIntPush_inNormal_expectsAllTrue() {
        // Arrange.
        val norm = NormInt().apply {
            length = 5
            averageDiff = 10
            min = -100
            max = 100
        }

        // Act.
        val results: MutableList<Boolean> = mutableListOf()
        results.add(norm.push(1))
        results.add(norm.push(2))
        results.add(norm.push(3))
        results.add(norm.push(4))
        results.add(norm.push(5))
        results.add(norm.push(10))  // average = (1 + 2 + 3 + 4 + 5) / 5  = 3
        results.add(norm.push(5))   // average = (2 + 3 + 4 + 5 + 10) / 5 = 4.8
        results.add(norm.push(-2))  // average = (3 + 4 + 5 + 10 + 5) / 5 = 5.4
        results.add(norm.push(0))   // average = (4 + 5 + 10 + 5 - 2) / 5 = 5

        // Assert.
        assertThat(results.all { it }).isEqualTo(true)
    }

    @Test
    fun normIntPush_belowMin_expectsFalse() {
        // Arrange.
        val norm = NormInt().apply {
            min = 0
        }

        // Act.
        val result = norm.push(-1)

        // Assert.
        assertThat(result).isEqualTo(false)
    }

    @Test
    fun normIntPush_MoreThanMax_expectsFalse() {
        // Arrange.
        val norm = NormInt().apply {
            max = 100
        }

        // Act.
        val result = norm.push(101)

        // Assert.
        assertThat(result).isEqualTo(false)
    }

    @Test
    fun normIntPush_MoreThanAverageDiff_expectsLastIsFalse() {
        // Arrange.
        val norm = NormInt().apply {
            length = 5
            min = 0
            max = 10000
            averageDiff = 10
        }

        // Act.
        val results: MutableList<Boolean> = mutableListOf()
        results.add(norm.push(1))
        results.add(norm.push(2))
        results.add(norm.push(3))
        results.add(norm.push(4))
        results.add(norm.push(5))
        results.add(norm.push(100)) // false (average + averageDiff < 100)

        // Assert.
        assertThat(results.count { it }).isEqualTo(5)
        assertThat(results.last()).isEqualTo(false)
    }

    @Test
    fun normIntPush_AverageDiffIgnoreCalled_ExpectsClearValues() {
        // Arrange.
        val norm = NormInt().apply {
            length = 5
            min = 0
            max = 100
            averageDiff = 10
            averageDiffIgnore = 5
        }

        // Act.
        val results: MutableList<Boolean> = mutableListOf()
        results.add(norm.push(1))
        results.add(norm.push(2))
        results.add(norm.push(3))
        results.add(norm.push(4))
        results.add(norm.push(5))
        results.add(norm.push(20)) // false
        results.add(norm.push(21)) // false
        results.add(norm.push(22)) // false
        results.add(norm.push(23)) // false
        results.add(norm.push(24)) // false
        results.add(norm.push(25)) // true - averageDiffIgnore called - prev values is cleared
        results.add(norm.push(26)) // true
        results.add(norm.push(27)) // true
        results.add(norm.push(28)) // true

        // Assert.
        assertThat(results.count { it }).isEqualTo(9)
        assertThat(results.count { !it }).isEqualTo(5)
        assertThat(results.last()).isEqualTo(true)
    }
}