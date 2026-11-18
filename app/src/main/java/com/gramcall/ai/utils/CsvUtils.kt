package com.gramcall.ai.utils

object CsvUtils {
    fun parse(input: String): List<List<String>> {
        val text = input.removePrefix("\uFEFF")
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    sb.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        row.add(sb.toString())
                        sb.setLength(0)
                    }
                    '\n' -> {
                        row.add(sb.toString())
                        sb.setLength(0)
                        rows.add(row)
                        row = mutableListOf()
                    }
                    '\r' -> {}
                    else -> sb.append(c)
                }
            }
            i++
        }
        if (sb.isNotEmpty() || row.isNotEmpty()) {
            row.add(sb.toString())
            rows.add(row)
        }
        return rows.filter { r -> r.any { it.isNotBlank() } }
    }

    fun escape(s: String): String =
        if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' })
            "\"" + s.replace("\"", "\"\"") + "\""
        else s

    fun toCsv(rows: List<List<String>>): String =
        rows.joinToString("\r\n") { r -> r.joinToString(",") { escape(it) } } + "\r\n"
}
