package com.gramcall.ai

import com.gramcall.ai.biz.*
import org.junit.Assert.*
import org.junit.Test

class OrderCalcTest {
    @Test fun totalsWithDiscount() {
        val t = OrderCalc.totals(listOf(5.0 to 40.0, 2.0 to 45.5, 1.0 to 120.0), 11.0)
        assertEquals(411.0, t.subtotal, 0.001)
        assertEquals(11.0, t.discount, 0.001)
        assertEquals(400.0, t.total, 0.001)
    }

    @Test fun discountNeverExceedsSubtotal() {
        val t = OrderCalc.totals(listOf(1.0 to 100.0), 500.0)
        assertEquals(100.0, t.discount, 0.001)
        assertEquals(0.0, t.total, 0.001)
    }

    @Test fun negativeDiscountIgnored() {
        assertEquals(100.0, OrderCalc.totals(listOf(1.0 to 100.0), -5.0).total, 0.001)
    }

    @Test fun roundsToPaise() {
        assertEquals(3.33, OrderCalc.lineTotal(3.0, 1.111), 0.0001)
    }

    @Test fun unitConversion() {
        assertEquals(0.5, Units.convert(500.0, Units.GRAM, Units.KG)!!, 0.0001)
        assertEquals(24.0, Units.convert(2.0, Units.DOZEN, Units.PIECE)!!, 0.0001)
        assertNull(Units.convert(1.0, Units.PACKET, Units.KG))
    }
}
