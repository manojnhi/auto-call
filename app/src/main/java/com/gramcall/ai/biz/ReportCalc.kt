package com.gramcall.ai.biz

import com.gramcall.ai.calling.CallStatus
import com.gramcall.ai.database.CallHistoryEntity
import com.gramcall.ai.database.OrderEntity
import com.gramcall.ai.database.OrderItemEntity

data class ReportSummary(
    val totalCalls: Int, val totalOrders: Int, val totalSales: Double,
    val customersContacted: Int, val successfulCalls: Int, val failedCalls: Int,
    val noAnswer: Int, val cancelledOrders: Int
)

data class VillageReportRow(
    val customerName: String, val mobile: String, val callStatus: String?,
    val orderSummary: String, val amount: Double, val orderDate: Long?
)

data class VillageReport(
    val customersCalled: Int, val ordersReceived: Int, val noAnswer: Int,
    val talkedNoOrder: Int, val totalSales: Double, val rows: List<VillageReportRow>
)

object ReportCalc {
    fun summarize(calls: List<CallHistoryEntity>, orders: List<OrderEntity>): ReportSummary {
        val live = orders.filter { it.status != OrderStatus.CANCELLED }
        return ReportSummary(
            totalCalls = calls.size,
            totalOrders = live.size,
            totalSales = Money.round2(live.sumOf { it.total }),
            customersContacted = calls.mapNotNull { it.customerId ?: -it.id }.distinct().size,
            successfulCalls = calls.count { it.status == CallStatus.COMPLETED },
            failedCalls = calls.count { it.status == CallStatus.FAILED },
            noAnswer = calls.count { it.status == CallStatus.NO_ANSWER },
            cancelledOrders = orders.count { it.status == OrderStatus.CANCELLED }
        )
    }

    fun village(
        calls: List<CallHistoryEntity>,
        orders: List<OrderEntity>,
        items: List<OrderItemEntity>
    ): VillageReport {
        val live = orders.filter { it.status != OrderStatus.CANCELLED }
        val lastCall = calls.groupBy { it.customerId }.mapValues { (_, v) -> v.maxByOrNull { it.startedAt ?: 0L }!! }
        val byCustomer = live.groupBy { it.customerId }
        val ids = (lastCall.keys + byCustomer.keys + orders.map { it.customerId }).toList().distinct()
        val itemsByOrder = items.groupBy { it.orderId }

        val rows = ids.flatMap { cid ->
            val call = lastCall[cid]
            val os = orders.filter { it.customerId == cid }
            if (os.isEmpty()) {
                listOf(VillageReportRow(call?.customerName ?: "—", call?.mobile ?: "", call?.status, "—", 0.0, null))
            } else os.map { o ->
                val text = itemsByOrder[o.id].orEmpty().joinToString(", ") {
                    "${Units.fmt(it.quantity)} ${Units.short(it.unit)} ${it.productName}"
                }
                VillageReportRow(
                    o.customerName, o.customerMobile, call?.status,
                    text + if (o.status == OrderStatus.CANCELLED) " (रद्द)" else "",
                    if (o.status == OrderStatus.CANCELLED) 0.0 else o.total, o.orderAt
                )
            }
        }
        val customersWithOrder = byCustomer.keys
        return VillageReport(
            customersCalled = lastCall.size,
            ordersReceived = live.size,
            noAnswer = lastCall.values.count { it.status == CallStatus.NO_ANSWER },
            talkedNoOrder = lastCall.entries.count { (cid, c) -> c.status == CallStatus.COMPLETED && cid !in customersWithOrder },
            totalSales = Money.round2(live.sumOf { it.total }),
            rows = rows
        )
    }
}
