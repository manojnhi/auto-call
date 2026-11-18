package com.gramcall.ai.biz

data class CatalogItem(val id: Long, val name: String, val unit: String, val aliases: List<String> = emptyList())

enum class LineState { OK, NEEDS_PRODUCT, NEEDS_QUANTITY }

data class ParsedLine(
    val raw: String,
    val product: CatalogItem?,
    val candidates: List<CatalogItem>,
    val quantity: Double?,     // product ki unit mein, mil sake to
    val spokenQty: Double?,
    val spokenUnit: String?
) {
    val state: LineState
        get() = when {
            product == null -> LineState.NEEDS_PRODUCT
            quantity == null -> LineState.NEEDS_QUANTITY
            else -> LineState.OK
        }
}

/**
 * Niyam-adharit (rule-based) Hindi/Hinglish order parser. Sirf catalog ke products pehchanta hai;
 * koi product, daam ya matra apni taraf se nahi banata. Jo samajh na aaye use flag karta hai.
 */
object HindiOrderParser {
    private val numberWords = mapOf(
        "एक" to 1.0, "दो" to 2.0, "तीन" to 3.0, "चार" to 4.0, "पाँच" to 5.0, "पांच" to 5.0, "पाच" to 5.0,
        "छह" to 6.0, "छः" to 6.0, "छे" to 6.0, "सात" to 7.0, "आठ" to 8.0, "नौ" to 9.0, "दस" to 10.0,
        "बीस" to 20.0, "पच्चीस" to 25.0, "पचास" to 50.0, "सौ" to 100.0,
        "आधा" to 0.5, "आधे" to 0.5, "डेढ़" to 1.5, "डेढ" to 1.5, "ढाई" to 2.5,
        "one" to 1.0, "two" to 2.0, "three" to 3.0, "four" to 4.0, "five" to 5.0,
        "six" to 6.0, "seven" to 7.0, "eight" to 8.0, "nine" to 9.0, "ten" to 10.0,
        "ek" to 1.0, "do" to 2.0, "teen" to 3.0, "char" to 4.0, "paanch" to 5.0, "panch" to 5.0, "aadha" to 0.5
    )

    private val unitWords = mapOf(
        "किलो" to Units.KG, "किलोग्राम" to Units.KG, "kg" to Units.KG, "kilo" to Units.KG,
        "ग्राम" to Units.GRAM, "gram" to Units.GRAM, "gm" to Units.GRAM,
        "लीटर" to Units.LITRE, "litre" to Units.LITRE, "liter" to Units.LITRE, "ltr" to Units.LITRE,
        "मिली" to Units.ML, "ml" to Units.ML,
        "पैकेट" to Units.PACKET, "packet" to Units.PACKET, "pkt" to Units.PACKET,
        "पीस" to Units.PIECE, "piece" to Units.PIECE, "pcs" to Units.PIECE, "नग" to Units.PIECE,
        "दर्जन" to Units.DOZEN, "dozen" to Units.DOZEN
    )

    private val separators = setOf("और", "aur", "तथा", "and", "व")
    private const val MATRAS = "ािीुूृेैोौंँः्"
    private const val DEV_DIGITS = "०१२३४५६७८९"

    fun tokenize(text: String): List<String> {
        val sb = StringBuilder()
        for (ch in text.lowercase()) {
            val d = DEV_DIGITS.indexOf(ch)
            sb.append(if (d >= 0) ('0' + d) else ch)
        }
        val spaced = sb.toString().replace(Regex("[,।;\\n]"), " और ")
        val out = mutableListOf<String>()
        for (raw in spaced.split(Regex("[\\s!?]+"))) {
            val t = raw.trim('.')
            if (t.isEmpty()) continue
            val m = Regex("^(\\d+(?:\\.\\d+)?)([^\\d.].*)$").find(t)
            if (m != null) {
                out.add(m.groupValues[1]); out.add(m.groupValues[2])
            } else out.add(t)
        }
        return out
    }

    private fun stem(w: String): String {
        val s = w.trimEnd(*MATRAS.toCharArray())
        return if (s.length >= 2) s else w
    }

    private fun wordMatches(word: String, nameToken: String) = stem(word) == stem(nameToken)

    private fun score(words: List<String>, name: String): Int {
        val toks = tokenize(name).filter { it !in separators }
        if (toks.isEmpty()) return 0
        return if (toks.all { t -> words.any { wordMatches(it, t) } }) toks.sumOf { it.length } else 0
    }

    private fun findProducts(words: List<String>, catalog: List<CatalogItem>): List<CatalogItem> {
        val scored = catalog.map { item ->
            val names = listOf(item.name) + item.aliases.filter { it.isNotBlank() }
            item to (names.maxOfOrNull { score(words, it) } ?: 0)
        }.filter { it.second > 0 }
        if (scored.isEmpty()) return emptyList()
        val best = scored.maxOf { it.second }
        return scored.filter { it.second == best }.map { it.first }
    }

    fun parse(text: String, catalog: List<CatalogItem>): List<ParsedLine> {
        val lines = mutableListOf<ParsedLine>()
        var qty: Double? = null
        var unit: String? = null
        val words = mutableListOf<String>()

        fun flush() {
            val found = findProducts(words, catalog)
            if (qty != null || found.isNotEmpty()) {
                val product = found.singleOrNull()
                val q = qty
                val inProductUnit = when {
                    product == null || q == null -> null
                    else -> Units.convert(q, unit ?: product.unit, product.unit)
                }
                val raw = (listOfNotNull(q?.let { Units.fmt(it) }, unit?.let { Units.short(it) }) + words).joinToString(" ")
                lines.add(ParsedLine(raw, product, found, inProductUnit, q, unit))
            }
            qty = null; unit = null; words.clear()
        }

        for (tok in tokenize(text)) {
            val n = tok.toDoubleOrNull() ?: numberWords[tok]
            val u = unitWords[tok]
            when {
                tok in separators -> flush()
                n != null -> { if (qty != null) flush(); qty = n }
                u != null -> if (unit == null) unit = u
                else -> words.add(tok)
            }
        }
        flush()
        return lines
    }
}
