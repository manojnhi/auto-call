package com.gramcall.ai.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvUtilsTest {
    @Test fun parsesSimpleRows() {
        val rows = CsvUtils.parse("name,mobile\r\nRam,9876543210\r\n")
        assertEquals(listOf(listOf("name", "mobile"), listOf("Ram", "9876543210")), rows)
    }

    @Test fun parsesQuotedCommaQuoteAndNewline() {
        val rows = CsvUtils.parse("a,\"b,c\",\"d\"\"e\",\"x\ny\"\n")
        assertEquals(listOf(listOf("a", "b,c", "d\"e", "x\ny")), rows)
    }

    @Test fun stripsBomAndSkipsBlankLines() {
        val rows = CsvUtils.parse("\uFEFFname\n\n\nSita\n")
        assertEquals(listOf(listOf("name"), listOf("Sita")), rows)
    }

    @Test fun roundTrip() {
        val data = listOf(listOf("राम, कुमार", "a\"b", "सादा"))
        assertEquals(data, CsvUtils.parse(CsvUtils.toCsv(data)))
    }
}
