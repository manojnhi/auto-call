package com.gramcall.ai

import com.gramcall.ai.biz.*
import org.junit.Assert.*
import org.junit.Test

class HindiOrderParserTest {
    private val catalog = listOf(
        CatalogItem(1, "आटा", Units.KG),
        CatalogItem(2, "चीनी", Units.KG),
        CatalogItem(3, "तेल", Units.LITRE, listOf("oil")),
        CatalogItem(4, "चावल", Units.KG)
    )

    @Test fun specExampleConversation() {
        val lines = HindiOrderParser.parse(
            "मुझे 5 किलो आटा और 2 किलो चीनी चाहिए। एक लीटर तेल भी दे दीजिए।", catalog
        )
        assertEquals(3, lines.size)
        assertEquals(listOf(1L, 2L, 3L), lines.map { it.product?.id })
        assertEquals(listOf(5.0, 2.0, 1.0), lines.map { it.quantity })
        assertTrue(lines.all { it.state == LineState.OK })
    }

    @Test fun gramsConvertToKg() {
        val l = HindiOrderParser.parse("500 ग्राम चीनी", catalog).single()
        assertEquals(0.5, l.quantity!!, 0.0001)
    }

    @Test fun halfAndDevanagariDigits() {
        val lines = HindiOrderParser.parse("आधा किलो चावल, ५ किलो आटे", catalog)
        assertEquals(listOf(0.5, 5.0), lines.map { it.quantity })
        assertEquals(listOf(4L, 1L), lines.map { it.product?.id })
    }

    @Test fun missingQuantityIsFlaggedNotGuessed() {
        val l = HindiOrderParser.parse("तेल चाहिए", catalog).single()
        assertEquals(LineState.NEEDS_QUANTITY, l.state)
        assertNull(l.quantity)
    }

    @Test fun unknownProductIsFlaggedNotInvented() {
        val l = HindiOrderParser.parse("3 किलो मटर", catalog).single()
        assertEquals(LineState.NEEDS_PRODUCT, l.state)
        assertNull(l.product)
    }

    @Test fun incompatibleUnitNeedsCheck() {
        val l = HindiOrderParser.parse("2 पैकेट चीनी", catalog).single()
        assertEquals(LineState.NEEDS_QUANTITY, l.state)
    }

    @Test fun aliasAndAttachedUnit() {
        val lines = HindiOrderParser.parse("2kg चीनी aur 1 ltr oil", catalog)
        assertEquals(listOf(2L, 3L), lines.map { it.product?.id })
    }

    @Test fun smallTalkProducesNoLines() {
        assertTrue(HindiOrderParser.parse("नमस्ते ठीक है धन्यवाद", catalog).isEmpty())
    }
}
