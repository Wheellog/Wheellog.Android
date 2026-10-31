package com.cooper.wheellog.utils

import org.junit.Assert.*
import org.junit.Test

class PaletteSignalsTest {
    @Test fun `speed warning uses tenths and includes the threshold`() {
        assertFalse(PaletteSignals.isSpeedWarning(true, false, 699, 70))
        assertTrue(PaletteSignals.isSpeedWarning(true, false, 700, 70))
        assertTrue(PaletteSignals.isSpeedWarning(true, false, 701, 70))
    }

    @Test fun `disabled or PWM based speed alarms do not activate speed color`() {
        assertFalse(PaletteSignals.isSpeedWarning(true, false, 800, 0))
        assertFalse(PaletteSignals.isSpeedWarning(true, true, 800, 70))
        assertFalse(PaletteSignals.isSpeedWarning(false, false, 800, 70))
    }

    @Test fun `battery warning includes low threshold and ignores unknown values`() {
        assertFalse(PaletteSignals.isBatteryWarning(true, 21, 20))
        assertTrue(PaletteSignals.isBatteryWarning(true, 20, 20))
        assertTrue(PaletteSignals.isBatteryWarning(true, 0, 20))
        assertFalse(PaletteSignals.isBatteryWarning(true, -1, 20))
        assertFalse(PaletteSignals.isBatteryWarning(true, 101, 20))
        assertFalse(PaletteSignals.isBatteryWarning(false, 10, 20))
    }
}
