package com.cooper.wheellog.voice

import org.junit.Assert.*
import org.junit.Test

class VoicePolicyTest {
    @Test fun `200 millisecond alarm updates cannot restart or accumulate speech`() {
        val policy = VoicePolicy()
        assertTrue(policy.alarm("PWM", 3, 0, 10000))
        for (now in 200L..2000L step 200) assertFalse(policy.alarm("PWM", 3, now, 10000))
        policy.complete()
        for (now in 2200L..9800L step 200) assertFalse(policy.alarm("PWM", 3, now, 10000))
        assertTrue(policy.alarm("PWM", 3, 10000, 10000))
    }
    @Test fun `a new alarm episode rearms after a gap`() {
        val policy = VoicePolicy()
        assertTrue(policy.alarm("TEMP", 4, 0, 60000))
        policy.complete()
        assertTrue(policy.alarm("TEMP", 4, 3000, 60000))
    }
    @Test fun `a critical alarm preempts a report but lower priorities cannot interrupt it`() {
        val policy = VoicePolicy()
        policy.occupy(0)
        assertTrue(policy.alarm("CURRENT", 4, 0, 10000))
        assertFalse(policy.alarm("PWM", 3, 200, 10000))
        assertTrue(policy.alarm("WHEEL", 5, 400, 10000))
        policy.complete()
        assertTrue(policy.alarm("PWM", 3, 600, 10000))
    }
    @Test fun `reports require fresh data movement and an idle speaker`() {
        val policy = VoicePolicy()
        assertFalse(policy.reportDue("wheel", 0, 60000, true, true))
        assertFalse(policy.reportDue("wheel", 60000, 60000, false, true))
        assertFalse(policy.reportDue("wheel", 61000, 60000, true, false))
        policy.occupy(4)
        assertFalse(policy.reportDue("wheel", 62000, 60000, true, true))
        policy.complete()
        assertTrue(policy.reportDue("wheel", 63000, 60000, true, true))
        assertFalse(policy.reportDue("wheel", 64000, 60000, true, true))
    }
    @Test fun `reports wait after an alarm and do not replay missed intervals`() {
        val policy = VoicePolicy()
        policy.reportDue("a", 0, 15000, true, true)
        policy.alarm("CURRENT", 4, 20000, 10000)
        policy.complete()
        assertFalse(policy.reportDue("a", 29999, 15000, true, true))
        assertTrue(policy.reportDue("a", 30000, 15000, true, true))
        assertFalse(policy.reportDue("a", 31000, 15000, true, true))
        assertTrue(policy.reportDue("a", 90000, 15000, true, true))
        assertFalse(policy.reportDue("a", 90001, 15000, true, true))
    }
    @Test fun `a wheel change and a reconnect reset the report schedule`() {
        val policy = VoicePolicy()
        policy.reportDue("a", 0, 60000, true, true)
        assertFalse(policy.reportDue("b", 59000, 60000, true, true))
        assertFalse(policy.reportDue("b", 60000, 60000, true, true))
        assertTrue(policy.reportDue("b", 119000, 60000, true, true))
        policy.reset()
        assertFalse(policy.reportDue("b", 120000, 60000, true, true))
    }
}
