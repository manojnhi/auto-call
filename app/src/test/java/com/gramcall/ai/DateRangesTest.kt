package com.gramcall.ai

import com.gramcall.ai.biz.DatePreset
import com.gramcall.ai.biz.DateRanges
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class DateRangesTest {
    private fun ts(y: Int, m: Int, d: Int, h: Int = 15) =
        Calendar.getInstance().apply { clear(); set(y, m - 1, d, h, 30, 0) }.timeInMillis

    private fun day(t: Long) = Calendar.getInstance().apply { timeInMillis = t }.get(Calendar.DAY_OF_MONTH)

    @Test fun today() {
        val now = ts(2026, 11, 17)
        val (a, b) = DateRanges.resolve(DatePreset.TODAY, now)
        assertTrue(now in a..b); assertEquals(17, day(a)); assertEquals(17, day(b))
    }

    @Test fun yesterdayExcludesToday() {
        val now = ts(2026, 11, 17)
        val (a, b) = DateRanges.resolve(DatePreset.YESTERDAY, now)
        assertEquals(16, day(a)); assertEquals(16, day(b)); assertFalse(now in a..b)
    }

    @Test fun last7IsSevenCalendarDays() {
        val now = ts(2026, 11, 17)
        val (a, _) = DateRanges.resolve(DatePreset.LAST7, now)
        assertEquals(11, day(a))
    }

    @Test fun thisMonthStartsOnFirst() {
        val (a, _) = DateRanges.resolve(DatePreset.THIS_MONTH, ts(2026, 11, 17))
        assertEquals(1, day(a))
    }

    @Test fun customRangeIsInclusiveAndOrderInsensitive() {
        val (a, b) = DateRanges.resolve(DatePreset.CUSTOM, 0L, ts(2026, 11, 20), ts(2026, 11, 10))
        assertEquals(10, day(a)); assertEquals(20, day(b))
        assertTrue(ts(2026, 11, 20, 23) in a..b)
    }
}
