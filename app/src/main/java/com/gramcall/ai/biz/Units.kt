package com.gramcall.ai.biz

object Units {
    const val KG = "KG"
    const val GRAM = "GRAM"
    const val LITRE = "LITRE"
    const val ML = "ML"
    const val PACKET = "PACKET"
    const val PIECE = "PIECE"
    const val DOZEN = "DOZEN"
    val ALL = listOf(KG, GRAM, LITRE, ML, PACKET, PIECE, DOZEN)

    fun label(u: String) = when (u) {
        KG -> "किलोग्राम"; GRAM -> "ग्राम"; LITRE -> "लीटर"; ML -> "मिलीलीटर"
        PACKET -> "पैकेट"; PIECE -> "पीस"; DOZEN -> "दर्जन"; else -> u
    }

    fun short(u: String) = when (u) {
        KG -> "किलो"; GRAM -> "ग्राम"; LITRE -> "लीटर"; ML -> "मिली"
        PACKET -> "पैकेट"; PIECE -> "पीस"; DOZEN -> "दर्जन"; else -> u
    }

    /** Bole gaye unit se product ke unit mein badalna; mel na khaye to null. */
    fun convert(qty: Double, from: String, to: String): Double? = when {
        from == to -> qty
        from == GRAM && to == KG -> qty / 1000.0
        from == KG && to == GRAM -> qty * 1000.0
        from == ML && to == LITRE -> qty / 1000.0
        from == LITRE && to == ML -> qty * 1000.0
        from == DOZEN && to == PIECE -> qty * 12.0
        from == PIECE && to == DOZEN -> qty / 12.0
        else -> null
    }

    fun fmt(q: Double): String =
        if (q == Math.floor(q) && Math.abs(q) < 1e9) q.toLong().toString()
        else "%.2f".format(q).trimEnd('0').trimEnd('.')
}

object Categories {
    val DEFAULT = listOf(
        "आटा", "चावल", "दाल", "चीनी", "नमक", "तेल",
        "मसाले", "चाय", "बिस्कुट", "साबुन", "शैम्पू", "अन्य सामान"
    )
}

object OrderStatus {
    const val CONFIRMED = "CONFIRMED"
    const val PENDING = "PENDING"
    const val PROCESSING = "PROCESSING"
    const val OUT_FOR_DELIVERY = "OUT_FOR_DELIVERY"
    const val DELIVERED = "DELIVERED"
    const val CANCELLED = "CANCELLED"
    val ALL = listOf(CONFIRMED, PENDING, PROCESSING, OUT_FOR_DELIVERY, DELIVERED, CANCELLED)

    fun label(s: String) = when (s) {
        CONFIRMED -> "कन्फ़र्म"; PENDING -> "पेंडिंग"; PROCESSING -> "तैयार हो रहा है"
        OUT_FOR_DELIVERY -> "डिलीवरी के लिए निकला"; DELIVERED -> "डिलीवर हुआ"; CANCELLED -> "रद्द"
        else -> s
    }
}

object Money {
    fun round2(v: Double): Double = Math.round(v * 100.0) / 100.0
    fun fmt(v: Double): String = "₹" + (if (v == Math.floor(v)) v.toLong().toString() else "%.2f".format(v))
}
