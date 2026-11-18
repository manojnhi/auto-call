package com.gramcall.ai.biz

/**
 * Ordering "samajhne" ka pluggable hissa. Abhi sirf on-device rule-based provider hai.
 * Baad mein backend ke through koi LLM provider jod sakte hain (API key sirf server par).
 */
interface OrderUnderstandingProvider {
    suspend fun parse(text: String, catalog: List<CatalogItem>): List<ParsedLine>
}

class RuleBasedProvider : OrderUnderstandingProvider {
    override suspend fun parse(text: String, catalog: List<CatalogItem>): List<ParsedLine> =
        HindiOrderParser.parse(text, catalog)
}
