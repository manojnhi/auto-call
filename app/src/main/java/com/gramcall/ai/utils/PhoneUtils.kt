package com.gramcall.ai.utils

object PhoneUtils {
    /** Sirf digits rakhta hai; +91 / 0 prefix hata kar 10 digit number deta hai. */
    fun normalize(raw: String): String {
        var d = raw.filter { it.isDigit() }
        if (d.length == 12 && d.startsWith("91")) d = d.substring(2)
        else if (d.length == 11 && d.startsWith("0")) d = d.substring(1)
        return d
    }

    /** Valid Indian mobile: 10 digit, 6-9 se shuru. */
    fun isValid(normalized: String): Boolean =
        normalized.length == 10 && normalized[0] in '6'..'9'
}
