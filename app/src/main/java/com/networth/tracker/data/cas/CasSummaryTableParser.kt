package com.networth.tracker.data.cas

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.IOException
import kotlin.math.abs

/**
 * Position-based parser for Summary CAS tables:
 * Folio No. | ISIN | Scheme Name | Cost Value (INR) | Unit Balance |
 * NAV Date | NAV (INR) | Market Value (INR) | Registrar
 *
 * One holding = one folio row (same ISIN on different folios stays separate).
 * Plain 6-digit numbers (PIN codes) are rejected as folios.
 */
object CasSummaryTableParser {

    private enum class Col {
        FOLIO, ISIN, SCHEME, COST, UNITS, NAV_DATE, NAV, MARKET, REGISTRAR
    }

    private data class Glyph(val text: String, val x0: Float, val x1: Float, val y: Float)
    private data class Word(val text: String, val x0: Float, val x1: Float, val y: Float) {
        val xMid: Float get() = (x0 + x1) / 2f
    }

    private data class Line(val y: Float, val words: List<Word>) {
        val text: String get() = words.joinToString(" ") { it.text }
    }

    private data class Column(val col: Col, val x0: Float, val x1: Float) {
        fun contains(x: Float, pad: Float = 0f): Boolean = x >= x0 - pad && x <= x1 + pad
    }

    private class MutableHolding(
        var folio: String,
        var isin: String,
        var scheme: String,
        var cost: Double?,
        var units: Double?,
        var navDate: String,
        var nav: Double?,
        var market: Double?
    )

    private val folioRe = Regex("""(\d{6,16}(?:\s*/\s*\d{1,4})?)""")
    private val isinRe = Regex("""(?<![A-Z0-9])(INF[0-9A-Z]{9})(?![A-Z0-9])""")
    private val dateRe = Regex("""(\d{2}-[A-Za-z]{3}-\d{4})""")
    private val amountRe = Regex("""\d{1,3}(?:,\d{2,3})+\.\d{1,4}|\d+\.\d{1,4}""")
    private val asOnRe = Regex("""(?i)as\s+on\s+(\d{2}-[A-Za-z]{3}-\d{4})""")
    private val totalRe = Regex("""(?i)\btotal\b""")
    private val headerTokenRe = Regex(
        """(?i)\b(folio|isin|scheme|cost|unit|balance|nav|market|registrar|value|date)\b"""
    )

    fun parse(document: PDDocument): CasParseResult {
        val glyphs = collectGlyphs(document)
        if (glyphs.isEmpty()) return CasParseResult(emptyList())

        val words = clusterWords(glyphs)
        val lines = clusterLines(words)
        if (lines.isEmpty()) return CasParseResult(emptyList())

        val asOf = lines.asSequence()
            .mapNotNull { asOnRe.find(it.text)?.groupValues?.get(1) }
            .firstOrNull()
            .orEmpty()

        val ordered = orderTopToBottom(lines)
        val holdings = mutableListOf<CasHolding>()
        var columns: List<Column> = emptyList()
        var current: MutableHolding? = null

        fun flush() {
            val row = current ?: return
            current = null
            var market = row.market
            if ((market == null || market <= 0) && row.units != null && row.nav != null) {
                market = row.units!! * row.nav!!
            }
            if (market == null || market <= 0 || row.folio.isBlank()) return
            if (!isValidFolio(row.folio)) return
            val cost = row.cost?.takeIf { it > 0 } ?: market
            val scheme = cleanScheme(row.scheme).ifBlank {
                if (row.isin.isNotBlank()) "Mutual Fund (${row.isin})" else "Mutual Fund"
            }
            holdings += CasHolding(
                schemeName = scheme,
                folio = normalizeFolio(row.folio),
                isin = row.isin,
                investedAmount = cost,
                currentAmount = market,
                units = row.units ?: 0.0,
                nav = row.nav ?: 0.0,
                asOfDate = row.navDate.ifBlank { asOf }
            )
        }

        for (line in ordered) {
            if (isHeaderLine(line)) {
                flush()
                val rebuilt = buildColumns(line.words)
                if (rebuilt.any { it.col == Col.FOLIO } && rebuilt.any { it.col == Col.MARKET }) {
                    columns = rebuilt
                }
                continue
            }
            if (columns.isEmpty()) continue
            if (totalRe.containsMatchIn(line.text) && folioRe.find(line.text) == null) {
                flush()
                continue
            }

            val cells = assignCells(line.words, columns)
            val folioRaw = cells[Col.FOLIO].orEmpty()
            val folio = folioRe.find(folioRaw)?.groupValues?.get(1)?.let { normalizeFolio(it) }.orEmpty()
            val isin = isinRe.find(cells[Col.ISIN].orEmpty())?.groupValues?.get(1)
                ?: isinRe.find(line.text)?.groupValues?.get(1).orEmpty()
            val scheme = cells[Col.SCHEME].orEmpty().trim()
            val cost = parseAmount(cells[Col.COST])
            val units = parseAmount(cells[Col.UNITS])
            val navDate = dateRe.find(cells[Col.NAV_DATE].orEmpty())?.groupValues?.get(1).orEmpty()
            val nav = parseAmount(cells[Col.NAV])
            val market = parseAmount(cells[Col.MARKET])

            val isMainRow = folio.isNotBlank() &&
                isValidFolio(folio) &&
                (isin.isNotBlank() || scheme.isNotBlank() || market != null || cost != null) &&
                !headerTokenRe.containsMatchIn(folioRaw)

            if (isMainRow) {
                flush()
                current = MutableHolding(
                    folio = folio,
                    isin = isin,
                    scheme = scheme,
                    cost = cost,
                    units = units,
                    navDate = navDate,
                    nav = nav,
                    market = market
                )
                continue
            }

            val cur = current ?: continue
            if (scheme.isNotBlank() && !isHeaderish(scheme)) {
                cur.scheme = listOf(cur.scheme, scheme).filter { it.isNotBlank() }.joinToString(" ")
            }
            if (cur.isin.isBlank() && isin.isNotBlank()) cur.isin = isin
            if (cost != null) cur.cost = cost
            if (units != null) cur.units = units
            if (navDate.isNotBlank()) cur.navDate = navDate
            if (nav != null) cur.nav = nav
            if (market != null) cur.market = market
        }
        flush()

        return CasParseResult(
            holdings = dedupeByFolioIsin(holdings),
            statementAsOf = asOf,
            sourceHint = "Summary CAS (table)"
        )
    }

    /** Plain 6-digit = PIN code; real folios are 7+ digits or have /sub-account. */
    internal fun isValidFolio(raw: String): Boolean {
        val n = normalizeFolio(raw)
        if (n.isEmpty()) return false
        if (n.contains('/')) {
            val parts = n.split('/')
            if (parts.size != 2) return false
            return parts[0].length in 6..16 && parts[0].all { it.isDigit() } &&
                parts[1].isNotEmpty() && parts[1].length <= 4 && parts[1].all { it.isDigit() }
        }
        return n.length in 7..16 && n.all { it.isDigit() }
    }

    private fun dedupeByFolioIsin(holdings: List<CasHolding>): List<CasHolding> {
        val map = LinkedHashMap<String, CasHolding>()
        for (h in holdings) {
            if (h.currentAmount <= 0 || !isValidFolio(h.folio)) continue
            val key = "${normalizeFolio(h.folio)}|${h.isin.ifBlank { h.schemeName.lowercase() }}"
            val existing = map[key]
            if (existing == null || h.currentAmount > existing.currentAmount) {
                map[key] = h
            }
        }
        return map.values.toList()
    }

    private fun isHeaderLine(line: Line): Boolean {
        val text = line.text.lowercase()
        var hits = 0
        if (text.contains("folio")) hits++
        if (text.contains("isin")) hits++
        if (text.contains("scheme")) hits++
        if (text.contains("cost")) hits++
        if (text.contains("market")) hits++
        if (text.contains("registrar") || text.contains("nav")) hits++
        return hits >= 4
    }

    private fun isHeaderish(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("scheme name") || t.contains("cost value") || t.contains("unit balance")
    }

    private fun orderTopToBottom(lines: List<Line>): List<Line> {
        if (lines.size < 2) return lines
        val ascending = lines.sortedBy { it.y }
        val descending = lines.sortedByDescending { it.y }
        fun headerIndex(list: List<Line>): Int = list.indexOfFirst { isHeaderLine(it) }
        val ascIdx = headerIndex(ascending)
        val descIdx = headerIndex(descending)
        return when {
            ascIdx >= 0 && descIdx >= 0 -> if (ascIdx <= descIdx) ascending else descending
            else -> ascending
        }
    }

    private fun collectGlyphs(document: PDDocument): List<Glyph> {
        val glyphs = mutableListOf<Glyph>()
        val stripper = object : PDFTextStripper() {
            @Throws(IOException::class)
            override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
                for (pos in textPositions) {
                    val unicode = pos.unicode ?: continue
                    if (unicode.isBlank() || unicode == "\n" || unicode == "\r") continue
                    if (pos.widthDirAdj <= 0 || pos.heightDir <= 0) continue
                    val x0 = pos.xDirAdj
                    glyphs += Glyph(unicode, x0, x0 + pos.widthDirAdj, pos.yDirAdj)
                }
            }
        }
        stripper.sortByPosition = true
        stripper.startPage = 1
        stripper.endPage = document.numberOfPages
        stripper.getText(document)
        return glyphs
    }

    private fun clusterWords(glyphs: List<Glyph>): List<Word> {
        if (glyphs.isEmpty()) return emptyList()
        val sorted = glyphs.sortedWith(compareBy<Glyph> { it.y }.thenBy { it.x0 })
        val words = mutableListOf<Word>()
        var text = StringBuilder()
        var x0 = 0f
        var x1 = 0f
        var y = 0f
        var open = false

        fun flush() {
            if (!open) return
            val t = text.toString().trim()
            if (t.isNotEmpty()) words += Word(t, x0, x1, y)
            text = StringBuilder()
            open = false
        }

        for (g in sorted) {
            if (!open) {
                text.append(g.text); x0 = g.x0; x1 = g.x1; y = g.y; open = true
                continue
            }
            val sameLine = abs(g.y - y) <= 2.2f
            val gap = g.x0 - x1
            if (sameLine && gap <= 2.4f) {
                text.append(g.text)
                x1 = maxOf(x1, g.x1)
                y = (y + g.y) / 2f
            } else {
                flush()
                text.append(g.text); x0 = g.x0; x1 = g.x1; y = g.y; open = true
            }
        }
        flush()
        return words
    }

    private fun clusterLines(words: List<Word>): List<Line> {
        if (words.isEmpty()) return emptyList()
        val sorted = words.sortedWith(compareBy<Word> { it.y }.thenBy { it.x0 })
        val lines = mutableListOf<Line>()
        var bucket = mutableListOf<Word>()
        var baseline = sorted.first().y

        fun flush() {
            if (bucket.isEmpty()) return
            lines += Line(bucket.map { it.y }.average().toFloat(), bucket.sortedBy { it.x0 })
            bucket = mutableListOf()
        }

        for (w in sorted) {
            if (bucket.isEmpty()) {
                bucket += w; baseline = w.y; continue
            }
            if (abs(w.y - baseline) <= 3.4f) {
                bucket += w
                baseline = bucket.map { it.y }.average().toFloat()
            } else {
                flush()
                bucket += w
                baseline = w.y
            }
        }
        flush()
        return lines
    }

    private fun buildColumns(headerWords: List<Word>): List<Column> {
        val clusters = mutableListOf<MutableList<Word>>()
        for (w in headerWords.sortedBy { it.x0 }) {
            val last = clusters.lastOrNull()
            if (last != null && w.x0 - last.maxOf { it.x1 } <= 10f) last += w
            else clusters += mutableListOf(w)
        }

        val labeled = mutableListOf<Pair<Col, Pair<Float, Float>>>()
        for (cluster in clusters) {
            val label = cluster.joinToString(" ") { it.text }.lowercase()
            val x0 = cluster.minOf { it.x0 }
            val x1 = cluster.maxOf { it.x1 }
            val col = when {
                label.contains("folio") -> Col.FOLIO
                label.contains("isin") -> Col.ISIN
                label.contains("scheme") -> Col.SCHEME
                label.contains("cost") -> Col.COST
                label.contains("unit") || (label.contains("closing") && label.contains("balance")) -> Col.UNITS
                label.contains("nav") && label.contains("date") -> Col.NAV_DATE
                label.contains("market") -> Col.MARKET
                label.contains("nav") || label.contains("price") -> Col.NAV
                label.contains("registrar") -> Col.REGISTRAR
                else -> null
            } ?: continue
            if (labeled.none { it.first == col }) labeled += col to (x0 to x1)
        }
        labeled.sortBy { it.second.first }
        if (labeled.isEmpty()) return emptyList()

        return labeled.mapIndexed { i, (col, range) ->
            val prevHi = labeled.getOrNull(i - 1)?.second?.second
            val nextLo = labeled.getOrNull(i + 1)?.second?.first
            val lo = if (prevHi != null) (prevHi + range.first) / 2f else range.first - 12f
            val hi = if (nextLo != null) (range.second + nextLo) / 2f else range.second + 40f
            Column(col, lo, hi)
        }
    }

    private fun assignCells(words: List<Word>, columns: List<Column>): Map<Col, String> {
        val buckets = columns.associate { it.col to StringBuilder() }.toMutableMap()
        for (w in words) {
            val col = columns.firstOrNull { it.contains(w.xMid) }?.col ?: continue
            val sb = buckets.getOrPut(col) { StringBuilder() }
            if (sb.isNotEmpty()) sb.append(' ')
            sb.append(w.text)
        }
        return buckets.mapValues { it.value.toString().trim() }
    }

    private fun parseAmount(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val match = amountRe.findAll(raw.replace(" ", "")).lastOrNull()?.value ?: return null
        return match.replace(",", "").toDoubleOrNull()
    }

    private fun normalizeFolio(raw: String): String =
        raw.replace("\\s+".toRegex(), "").trim()

    private fun cleanScheme(name: String): String =
        name.replace(Regex("""\s+"""), " ")
            .replace(Regex("""(?i)\b(CAMS|KFINTECH|KFIN|KARVY)\b"""), "")
            .trim()
            .trimEnd('-', ':', ' ')
}
