package com.gramcall.ai

import com.gramcall.ai.calling.CallingRules
import org.junit.Assert.*
import org.junit.Test

class CallingRulesTest {
    @Test fun callingHoursAre9To21() {
        assertFalse(CallingRules.isAllowedHour(8))
        assertTrue(CallingRules.isAllowedHour(9))
        assertTrue(CallingRules.isAllowedHour(20))
        assertFalse(CallingRules.isAllowedHour(21))
        assertFalse(CallingRules.isAllowedHour(0))
    }
}
