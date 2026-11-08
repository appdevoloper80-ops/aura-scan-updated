package com.example.engine

data class AiDocumentSummary(
    val title: String,
    val overviewBullets: List<String>,
    val keyPoints: List<String>,
    val detectedType: String
)

object AiSummaryEngine {

    /**
     * Pure on-device heuristic summarizer.
     * Analyzes OCR lines, detects document structure, amounts, dates, and item lists.
     */
    fun summarizeDocumentText(rawText: String): AiDocumentSummary {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return AiDocumentSummary(
                title = "Empty Document",
                overviewBullets = listOf("No readable text detected in this document."),
                keyPoints = listOf("0 lines detected"),
                detectedType = "General"
            )
        }

        val totalRegex = Regex("""(?i)(?:total|amount|sum|net|balance|due|paid|grand total|subtotal|कुल|कुल राशि|कुल योग|总计|合計|합계)[\s:]*([$₹€£¥]?\s*[\d,]+(?:\.\d{1,2})?)""")
        val dateRegex = Regex("""\b(?:\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\d{4}[/-]\d{1,2}[/-]\d{1,2}|\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]* \d{1,2},? \d{4})\b""")
        val merchantRegex = Regex("""(?i)(?:shop|store|mart|market|restaurant|cafe|mall|pharmacy|inc|ltd|co|मार्ट|दुकान|商店|ストア|마켓)""")
        val invoiceRegex = Regex("""(?i)(?:inv|invoice|receipt|order|bill|slip|reference)[\s#:]*([A-Z0-9-]{4,})""")

        var detectedTotal: String? = null
        var detectedDate: String? = null
        var detectedMerchant: String? = null
        var detectedInvoice: String? = null
        val itemsFound = mutableListOf<String>()

        for (line in lines) {
            if (detectedTotal == null) {
                val match = totalRegex.find(line)
                if (match != null) {
                    detectedTotal = match.groupValues.getOrNull(1)?.trim() ?: line
                }
            }
            if (detectedDate == null) {
                val match = dateRegex.find(line)
                if (match != null) {
                    detectedDate = match.value
                }
            }
            if (detectedMerchant == null && merchantRegex.containsMatchIn(line)) {
                detectedMerchant = line
            }
            if (detectedInvoice == null) {
                val match = invoiceRegex.find(line)
                if (match != null) {
                    detectedInvoice = match.groupValues.getOrNull(1)
                }
            }
            if (line.matches(Regex(""".*?\s+[$₹€£¥]?\d+(?:\.\d{1,2})?"""))) {
                itemsFound.add(line)
            }
        }

        val firstLine = lines.firstOrNull() ?: "Document"
        val isReceipt = rawText.contains("receipt", ignoreCase = true) ||
                rawText.contains("mart", ignoreCase = true) ||
                rawText.contains("store", ignoreCase = true) ||
                rawText.contains("thank you", ignoreCase = true) ||
                rawText.contains("inv #", ignoreCase = true) ||
                detectedTotal != null

        val detectedType = when {
            isReceipt -> "Receipt / Invoice"
            rawText.contains("passport", ignoreCase = true) || rawText.contains("identity", ignoreCase = true) || rawText.contains("license", ignoreCase = true) -> "ID / Official Document"
            rawText.contains("contract", ignoreCase = true) || rawText.contains("agreement", ignoreCase = true) || rawText.contains("terms", ignoreCase = true) -> "Legal Agreement"
            rawText.contains("medical", ignoreCase = true) || rawText.contains("prescription", ignoreCase = true) || rawText.contains("patient", ignoreCase = true) -> "Medical Report"
            else -> "Notes / Document"
        }

        val overviewBullets = mutableListOf<String>()
        if (isReceipt) {
            val merchantName = (detectedMerchant ?: firstLine).take(30)
            overviewBullets.add("Purchases from $merchantName.")
            if (detectedTotal != null) overviewBullets.add("Total amount: $detectedTotal")
            if (itemsFound.isNotEmpty()) {
                val previewItems = itemsFound.take(3).joinToString(", ") { it.substringBeforeLast(" ") }
                overviewBullets.add("Items: $previewItems")
            }
        } else {
            overviewBullets.add("Document subject: ${firstLine.take(40)}")
            val preview = lines.drop(1).take(2).joinToString(" ").take(100)
            if (preview.isNotBlank()) overviewBullets.add(preview)
            if (detectedDate != null) overviewBullets.add("Date detected: $detectedDate")
        }

        val keyPoints = mutableListOf<String>()
        if (detectedTotal != null) keyPoints.add("Total: $detectedTotal")
        if (detectedDate != null) keyPoints.add("Date: $detectedDate")
        if (detectedInvoice != null) keyPoints.add("Ref/Inv: $detectedInvoice")
        keyPoints.add("Total lines: ${lines.size}")
        if (itemsFound.isNotEmpty()) keyPoints.add("Line items: ${itemsFound.size} entries")

        return AiDocumentSummary(
            title = if (isReceipt) "Store receipt / Invoice" else firstLine.take(32),
            overviewBullets = if (overviewBullets.isNotEmpty()) overviewBullets else listOf("Document content parsed successfully."),
            keyPoints = keyPoints,
            detectedType = detectedType
        )
    }
}
