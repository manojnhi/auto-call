package com.gramcall.ai

import com.gramcall.ai.biz.*
import com.gramcall.ai.calling.CallStatus
import com.gramcall.ai.database.CallHistoryEntity
import com.gramcall.ai.database.OrderEntity
import com.gramcall.ai.database.OrderItemEntity
import org.junit.Assert.*
import org.junit.Test

class ReportCalcTest {
    private fun call(id: Long, cust: Long, status: String) = CallHistoryEntity(
        id = id, campaignId = 1, customerId = cust, customerName = "C$cust",
        mobile = "98765432$cust", status = status, position = id.toInt(), startedAt = 1000L + id
    )

    private fun order(id: Long, cust: Long, total: Double, status: String = OrderStatus.CONFIRMED) = OrderEntity(
        id = id, customerId = cust, customerName = "C$cust", customerMobile = "98765432$cust",
        subtotal = total, total = total, status = status
    )

    @Test fun summaryIgnoresCancelledInSales() {
        val calls = listOf(call(1, 1, CallStatus.COMPLETED), call(2, 2, CallStatus.NO_ANSWER), call(3, 3, CallStatus.FAILED))
        val orders = listOf(order(1, 1, 500.0), order(2, 1, 300.0, OrderStatus.CANCELLED))
        val s = ReportCalc.summarize(calls, orders)
        assertEquals(3, s.totalCalls); assertEquals(1, s.totalOrders); assertEquals(500.0, s.totalSales, 0.001)
        assertEquals(1, s.successfulCalls); assertEquals(1, s.noAnswer); assertEquals(1, s.failedCalls)
        assertEquals(1, s.cancelledOrders); assertEquals(3, s.customersContacted)
    }

    @Test fun villageReportCounts() {
        val calls = listOf(
            call(1, 1, CallStatus.COMPLETED), call(2, 2, CallStatus.COMPLETED),
            call(3, 3, CallStatus.NO_ANSWER)
        )
        val orders = listOf(order(1, 1, 700.0))
        val items = listOf(OrderItemEntity(1, 1, 5, "आटा", Units.KG, 5.0, 40.0, 200.0))
        val r = ReportCalc.village(calls, orders, items)
        assertEquals(3, r.customersCalled); assertEquals(1, r.ordersReceived)
        assertEquals(1, r.noAnswer); assertEquals(1, r.talkedNoOrder); assertEquals(700.0, r.totalSales, 0.001)
        assertTrue(r.rows.first { it.customerName == "C1" }.orderSummary.contains("आटा"))
    }
}
