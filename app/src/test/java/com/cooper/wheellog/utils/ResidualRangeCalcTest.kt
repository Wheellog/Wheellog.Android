package com.cooper.wheellog.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ResidualRangeCalcTest {

    @Test
    fun calcConsumption() {
        // Arrange.
        val calc = ResidualRangeCalc(70.0).apply {
            updateData(2.3, 100.0, 0.0)
            updateData(2.4, 100.0, 30.0)
            updateData(3.3, 98.0, 50.0)
            updateData(5.3, 96.0, 200.0)
            updateData(6.6, 95.0, 300.0)
            updateData(7.1, 93.0, 400.0)
            for (i in 1..20) {
                updateData(7.1, 88.0, 1050.0)
            }
        }

        // Act.
        calc.calc()

        // Assert.
        assertThat(calc.consumption[0]).isEqualTo(0)
        // real consumption is 10 V/km = 10V / 1000 m = (98 - 88) / (1050 - 50)
        assertThat(calc.consumption[5]).isLessThan(10)
        assertThat(calc.consumption[5]).isGreaterThan(9)
        assertThat(calc.consumption[10]).isEqualTo(0)
        // real residual is 1800 meters = 18 V remain to critical / 10 consumption = (88 - 70) / 10
        assertThat(calc.residual[5]).isLessThan(2_200)
        assertThat(calc.residual[5]).isGreaterThan(1_799)
    }
}