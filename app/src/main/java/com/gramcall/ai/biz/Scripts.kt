package com.gramcall.ai.biz

import com.gramcall.ai.database.OrderEntity
import com.gramcall.ai.database.OrderItemEntity
import com.gramcall.ai.utils.Fmt

object Scripts {
    const val DEFAULT_GREETING =
        "नमस्ते! मैं {business} किराना स्टोर का AI सहायक बोल रहा हूँ। क्या आप आज किराने का कोई सामान मंगवाना चाहेंगे?"
    const val DEFAULT_CONFIRM = "जी, आपका ऑर्डर है {items}। कुल {total}। क्या यह सही है?"
    const val DEFAULT_THANKS = "धन्यवाद! आपका ऑर्डर दर्ज कर लिया गया है।"

    fun itemsText(items: List<Pair<String, Pair<Double, String>>>): String {
        // (नाम, (मात्रा, यूनिट))
        val parts = items.map { (name, q) -> "${Units.fmt(q.first)} ${Units.short(q.second)} $name" }
        return when (parts.size) {
            0 -> ""
            1 -> parts[0]
            else -> parts.dropLast(1).joinToString(", ") + " और " + parts.last()
        }
    }

    fun fill(template: String, vars: Map<String, String>): String =
        vars.entries.fold(template) { acc, (k, v) -> acc.replace("{$k}", v) }
}

object Invoice {
    fun text(shopName: String, shopAddress: String, shopMobile: String, o: OrderEntity, items: List<OrderItemEntity>): String {
        val sb = StringBuilder()
        sb.appendLine(shopName.ifBlank { "GramCall AI" })
        if (shopAddress.isNotBlank()) sb.appendLine(shopAddress)
        if (shopMobile.isNotBlank()) sb.appendLine("मोबाइल: $shopMobile")
        sb.appendLine("----------------------------")
        sb.appendLine("इनवॉइस / ऑर्डर #${o.id}")
        sb.appendLine("तारीख: ${Fmt.dateTime(o.orderAt)}")
        sb.appendLine("ग्राहक: ${o.customerName} (${o.customerMobile})")
        val place = listOf(o.villageName, o.cityName).filter { it.isNotBlank() }.joinToString(", ")
        if (place.isNotEmpty()) sb.appendLine("स्थान: $place")
        if (o.deliveryAddress.isNotBlank()) sb.appendLine("पता: ${o.deliveryAddress}")
        sb.appendLine("----------------------------")
        items.forEach {
            sb.appendLine("${it.productName}  ${Units.fmt(it.quantity)} ${Units.short(it.unit)} x ${Money.fmt(it.unitPrice)} = ${Money.fmt(it.lineTotal)}")
        }
        sb.appendLine("----------------------------")
        sb.appendLine("उप-योग: ${Money.fmt(o.subtotal)}")
        if (o.discount > 0) sb.appendLine("छूट: -${Money.fmt(o.discount)}")
        sb.appendLine("कुल: ${Money.fmt(o.total)}")
        sb.appendLine("स्थिति: ${OrderStatus.label(o.status)}")
        sb.appendLine("धन्यवाद!")
        return sb.toString()
    }

    fun html(text: String): String =
        "<html><head><meta charset=\"utf-8\"></head><body><pre style=\"font-size:16px\">" +
            text.replace("&", "&amp;").replace("<", "&lt;") + "</pre></body></html>"
}
