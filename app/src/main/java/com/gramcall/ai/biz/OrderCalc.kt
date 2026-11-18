package com.gramcall.ai.biz

data class Totals(val subtotal: Double, val discount: Double, val total: Double)

object OrderCalc {
    fun lineTotal(qty: Double, unitPrice: Double): Double = Money.round2(qty * unitPrice)

    /** discount subtotal se zyada ho to subtotal tak rok diya jaata hai; total kabhi negative nahi. */
    fun totals(lines: List<Pair<Double, Double>>, discount: Double): Totals {
        val sub = Money.round2(lines.sumOf { lineTotal(it.first, it.second) })
        val d = Money.round2(discount.coerceIn(0.0, sub))
        return Totals(sub, d, Money.round2(sub - d))
    }
}
