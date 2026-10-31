package com.cooper.wheellog.voice

import org.junit.Assert.*
import org.junit.Test

class VoiceReportScheduleTest {
    @Test fun `periodic reports cross ride multiples without replaying missed intervals`() {
        val schedule = VoiceReportSchedule()
        val config = DistanceReports(periodic = true, intervalMetres = 2000)
        assertFalse(schedule.due("a", 0, 100000, 0, false, 60000, config).any)
        assertFalse(schedule.due("a", 1, 101999, 1999, false, 60000, config).any)
        val due = schedule.due("a", 2, 102003, 2003, false, 60000, config)
        assertTrue(due.periodic); schedule.spoken(2, due)
        assertFalse(schedule.due("a", 3, 103999, 3999, false, 60000, config).any)
        val jump = schedule.due("a", 4, 108100, 8100, false, 60000, config)
        assertTrue(jump.periodic); schedule.spoken(4, jump)
        assertFalse(schedule.due("a", 5, 108100, 8100, false, 60000, config).any)
    }
    @Test fun `enabling during a ride waits for the next multiple and editing rebases it`() {
        val schedule = VoiceReportSchedule()
        val config = DistanceReports(periodic = true, intervalMetres = 2000)
        assertFalse(schedule.due("a", 0, 104500, 4500, false, 60000, config).any)
        assertTrue(schedule.due("a", 1, 106000, 6000, false, 60000, config).periodic)
        assertFalse(schedule.due("a", 2, 106000, 6000, false, 60000, config.copy(intervalMetres = 3000)).any)
    }
    @Test fun `a trip reset and a different wheel cannot inherit periodic progress`() {
        val schedule = VoiceReportSchedule()
        val config = DistanceReports(periodic = true, intervalMetres = 2000)
        schedule.due("a", 0, 109000, 9000, false, 60000, config)
        assertFalse(schedule.due("a", 1, 109010, 10, false, 60000, config).any)
        assertTrue(schedule.due("a", 2, 111001, 2001, false, 60000, config).periodic)
        assertFalse(schedule.due("b", 3, 500000, 10000, false, 60000, config).any)
    }
    @Test fun `distance and time conditions coincide in one due decision`() {
        val schedule = VoiceReportSchedule()
        val config = DistanceReports(true, 2000, true, 2000, 100000, true, 102000)
        assertFalse(schedule.due("a", 0, 100000, 0, true, 60000, config).any)
        val due = schedule.due("a", 60000, 102001, 2001, true, 60000, config)
        assertTrue(due.time && due.periodic && due.after && due.target)
        schedule.spoken(60000, due)
        assertFalse(schedule.due("a", 60001, 102001, 2001, true, 60000,
            config.copy(after = false, target = false)).any)
    }
    @Test fun `blocked or rejected speech does not consume a distance condition`() {
        val schedule = VoiceReportSchedule()
        val config = DistanceReports(periodic = true, intervalMetres = 2000)
        schedule.due("a", 0, 100000, 0, false, 60000, config)
        assertTrue(schedule.due("a", 1, 102001, 2001, false, 60000, config).periodic)
        assertTrue(schedule.due("a", 2, 102010, 2010, false, 60000, config).periodic)
        val policy = VoicePolicy()
        policy.alarm("CURRENT", 4, 2, 10000)
        assertFalse(policy.canReport(3)); policy.complete()
        assertFalse(policy.canReport(10001)); assertTrue(policy.canReport(10002))
    }
    @Test fun `relative distance needs an origin and odometer rollback cannot trigger it`() {
        val schedule = VoiceReportSchedule()
        val config = DistanceReports(after = true, afterMetres = 500)
        assertFalse(schedule.due("a", 0, 100000, 0, false, 60000, config).any)
        val armed = config.copy(originMetres = 100000)
        assertFalse(schedule.due("a", 1, 99999, 0, false, 60000, armed).any)
        assertFalse(schedule.due("a", 2, 100499, 499, false, 60000, armed).any)
        assertTrue(schedule.due("a", 3, 100503, 503, false, 60000, armed).after)
    }
    @Test fun `target works without time or a repeating distance condition`() {
        val schedule = VoiceReportSchedule()
        val config = DistanceReports(target = true, targetMetres = 12345678)
        assertFalse(schedule.due("a", 0, 12345677, 10, false, 60000, config).any)
        val due = schedule.due("a", 1, 12345680, 13, false, 60000, config)
        assertTrue(due.target); assertFalse(due.time || due.periodic || due.after)
    }
    @Test fun `the new default interval waits for five kilometres then the next multiple`() {
        val schedule = VoiceReportSchedule()
        val config = DistanceReports(periodic = true)
        assertFalse(schedule.due("wheel", 0, 100000, 0, false, 60000, config).any)
        assertFalse(schedule.due("wheel", 1, 102001, 2001, false, 60000, config).any)
        assertFalse(schedule.due("wheel", 2, 104999, 4999, false, 60000, config).any)
        val first = schedule.due("wheel", 3, 105001, 5001, false, 60000, config)
        assertTrue(first.periodic); schedule.spoken(3, first)
        assertFalse(schedule.due("wheel", 4, 109999, 9999, false, 60000, config).any)
        assertTrue(schedule.due("wheel", 5, 110001, 10001, false, 60000, config).periodic)
    }
}
