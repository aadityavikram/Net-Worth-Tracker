package com.networth.tracker.data.cas

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.IOException

object CasPdfImporter {

    @Volatile
    private var pdfBoxReady = false

    fun ensureInitialized(context: Context) {
        if (!pdfBoxReady) {
            synchronized(this) {
                if (!pdfBoxReady) {
                    PDFBoxResourceLoader.init(context.applicationContext)
                    pdfBoxReady = true
                }
            }
        }
    }

    suspend fun importFromUri(
        context: Context,
        uri: Uri,
        password: String
    ): CasParseResult = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        val pwd = password.trim().uppercase()
        if (pwd.isBlank()) {
            throw CasImportException.InvalidPassword()
        }

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedInputStream(input).use { buffered ->
                    PDDocument.load(buffered, pwd).use { document ->
                        if (document.isEncrypted) {
                            document.setAllSecurityToBeRemoved(true)
                        }

                        val candidates = mutableListOf<CasParseResult>()

                        runCatching { CasSummaryTableParser.parse(document) }
                            .getOrNull()
                            ?.takeIf { it.holdings.isNotEmpty() }
                            ?.let { candidates += it }

                        for (text in listOf(
                            extractText(document, spaced = true, sorted = true),
                            extractText(document, spaced = true, sorted = false),
                            extractText(document, spaced = false, sorted = true)
                        )) {
                            val parsed = CasTextParser.parse(text)
                            if (parsed.holdings.isNotEmpty()) candidates += parsed
                        }

                        val result = pickBestParse(candidates)
                            ?: throw CasImportException.EmptyHoldings()
                        result
                    }
                }
            } ?: throw CasImportException.ReadFailed("Could not open the selected PDF.")
        } catch (e: CasImportException) {
            throw e
        } catch (e: InvalidPasswordException) {
            throw CasImportException.InvalidPassword()
        } catch (e: Exception) {
            val message = e.message.orEmpty()
            if (message.contains("password", ignoreCase = true) ||
                message.contains("Cannot decrypt", ignoreCase = true)
            ) {
                throw CasImportException.InvalidPassword()
            }
            throw CasImportException.ReadFailed(
                e.message?.takeIf { it.isNotBlank() } ?: "Failed to read CAS PDF."
            )
        }
    }

    private fun pickBestParse(candidates: List<CasParseResult>): CasParseResult? {
        if (candidates.isEmpty()) return null

        // Prefer the parse that recovered the most distinct folio+ISIN holdings.
        val best = candidates.maxWithOrNull(
            compareBy<CasParseResult> { it.holdings.size }
                .thenBy { it.holdings.count { h -> h.folio.isNotBlank() } }
                .thenBy { it.holdings.count { h -> h.isin.isNotBlank() } }
                .thenBy { it.totalInvested }
        ) ?: return null

        if (best.holdings.isEmpty()) return null

        // Merge equal-sized candidates so partial strategies can fill gaps.
        val topSize = best.holdings.size
        val peers = candidates.filter { it.holdings.size >= topSize - 1 }
        if (peers.size == 1) return best

        val merged = LinkedHashMap<String, CasHolding>()
        var statementAsOf = ""
        var sourceHint = ""
        for (parsed in peers.sortedByDescending { it.holdings.size }) {
            if (parsed.statementAsOf.isNotBlank() && statementAsOf.isBlank()) {
                statementAsOf = parsed.statementAsOf
            }
            if (parsed.sourceHint.isNotBlank() && sourceHint.isBlank()) {
                sourceHint = parsed.sourceHint
            }
            for (holding in parsed.holdings) {
                val key = holdingKey(holding)
                val existing = merged[key]
                if (existing == null || scoreHolding(holding) > scoreHolding(existing)) {
                    merged[key] = holding
                }
            }
        }
        return CasParseResult(
            holdings = merged.values.toList(),
            statementAsOf = statementAsOf.ifBlank { best.statementAsOf },
            sourceHint = sourceHint.ifBlank { best.sourceHint.ifBlank { "Summary CAS" } }
        )
    }

    private fun scoreHolding(holding: CasHolding): Int {
        var score = 0
        if (holding.folio.isNotBlank()) score += 4
        if (holding.isin.isNotBlank()) score += 3
        if (!holding.schemeName.startsWith("Mutual Fund")) score += 2
        if (holding.investedAmount > 0 && holding.investedAmount != holding.currentAmount) score += 1
        if (holding.units > 0) score += 1
        return score
    }

    private fun holdingKey(holding: CasHolding): String {
        val folio = holding.folio.replace(" ", "")
        return when {
            folio.isNotBlank() && holding.isin.isNotBlank() -> "$folio|${holding.isin}"
            folio.isNotBlank() -> "$folio|${holding.schemeName.lowercase()}|${"%.2f".format(holding.currentAmount)}"
            holding.isin.isNotBlank() ->
                "${holding.isin}|${"%.2f".format(holding.investedAmount)}|${"%.2f".format(holding.currentAmount)}"
            else -> "${holding.schemeName.lowercase()}|${"%.2f".format(holding.currentAmount)}"
        }
    }

    private fun extractText(document: PDDocument, spaced: Boolean, sorted: Boolean): String {
        val stripper = if (spaced) ColumnAwareTextStripper() else PDFTextStripper()
        stripper.sortByPosition = sorted
        stripper.setSpacingTolerance(0.35f)
        stripper.setAverageCharTolerance(0.25f)
        return stripper.getText(document)
    }
}

private class ColumnAwareTextStripper : PDFTextStripper() {
    @Throws(IOException::class)
    override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
        if (textPositions.isEmpty()) {
            super.writeString(text, textPositions)
            return
        }
        val builder = StringBuilder()
        var previous: TextPosition? = null
        for (position in textPositions) {
            if (previous != null) {
                val gap = position.xDirAdj - (previous.xDirAdj + previous.widthDirAdj)
                val minGap = maxOf(previous.widthDirAdj, position.widthDirAdj) * 0.35f
                if (gap > minGap) builder.append(' ')
            }
            builder.append(position.unicode)
            previous = position
        }
        writeString(builder.toString())
    }
}
