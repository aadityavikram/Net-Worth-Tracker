package com.networth.tracker.data.cas

/**
 * Parses CAMS / KFintech CAS text extracted from a PDF.
 * Supports Detailed (labelled valuation lines) and Summary (tabular / jumbled text).
 */
object CasTextParser {

    private val folioLineRe = Regex(
        """Folio\s+No\s*:\s*(\d+(?:\s*/\s*\d+)?)""",
        RegexOption.IGNORE_CASE
    )
    private val schemeHeadRe = Regex(
        """^\s*([A-Z0-9][A-Z0-9 ]{0,14}?)\s*-\s*(.+)$""",
        RegexOption.IGNORE_CASE
    )
    private val closeBalRe = Regex(
        """Closing\s+Unit\s+Balance\s*:?\s*([\d,.]+)""",
        RegexOption.IGNORE_CASE
    )
    private val navRe = Regex(
        """NAV\s+on\s+(\d{2}\s*[-/]\s*[A-Za-z]{3}\s*[-/]\s*\d{4}|\d{2}\s*[-/]\s*\d{2}\s*[-/]\s*\d{4})\s*:\s*INR\s*([\d,.]+)""",
        RegexOption.IGNORE_CASE
    )
    private val valuationRe = Regex(
        """(?:Valuation|Market\s+Value)\s+on\s+(\d{2}\s*[-/]\s*[A-Za-z]{3}\s*[-/]\s*\d{4}|\d{2}\s*[-/]\s*\d{2}\s*[-/]\s*\d{4})\s*:\s*INR\s*([\d,.]+)""",
        RegexOption.IGNORE_CASE
    )
    private val costValueRe = Regex(
        """(?:Total\s+)?Cost\s+Value\s*:?\s*([\d,.]+)""",
        RegexOption.IGNORE_CASE
    )
    /** MF ISINs are 12 chars starting with INF; allow glued neighbours from PDF extract. */
    private val isinRe = Regex("""(?<![A-Z0-9])(INF[0-9A-Z]{9})(?![A-Z0-9])""")
    private val asOnRe = Regex(
        """as\s+on\s+(\d{2}\s*[-/]\s*[A-Za-z]{3}\s*[-/]\s*\d{4}|\d{2}\s*[-/]\s*\d{2}\s*[-/]\s*\d{4})""",
        RegexOption.IGNORE_CASE
    )
    private val dateRe = Regex(
        """(\d{2}\s*-\s*[A-Za-z]{3}\s*-\s*\d{4}|\d{2}\s*/\s*[A-Za-z]{3}\s*/\s*\d{4}|\d{2}\s*[-/]\s*\d{2}\s*[-/]\s*\d{4})"""
    )
    private val amountRe = Regex("""\d{1,3}(?:,\d{2,3})+\.\d{1,4}|\d+\.\d{1,4}""")
    private val folioNearRe = Regex("""(\d{5,16}(?:\s*/\s*\d{1,4})?)""")
    private val summaryRowRe = Regex(
        """(\d{5,16}(?:\s*/\s*\d{1,4})?)\s+(INF[0-9A-Z]{9})\s+(.+?)\s+([\d,]+\.\d{1,4})\s+([\d,]+\.\d{1,4})\s+(\d{2}\s*[-/]\s*[A-Za-z0-9]{2,3}\s*[-/]\s*\d{4})\s+([\d,]+\.\d{1,4})\s+([\d,]+\.\d{1,4})""",
        RegexOption.IGNORE_CASE
    )
    private val summaryRowNoIsinRe = Regex(
        """(\d{5,16}(?:\s*/\s*\d{1,4})?)\s+([A-Z0-9][A-Z0-9 ]{0,14}?)\s*-\s*(.+?)\s+([\d,]+\.\d{1,4})\s+([\d,]+\.\d{1,4})\s+(\d{2}\s*[-/]\s*[A-Za-z0-9]{2,3}\s*[-/]\s*\d{4})\s+([\d,]+\.\d{1,4})\s+([\d,]+\.\d{1,4})""",
        RegexOption.IGNORE_CASE
    )
    private val registrarTokenRe = Regex("""\b(CAMS|KFINTECH|KFIN|KARVY)\b""", RegexOption.IGNORE_CASE)
    private val junkSchemeHints = listOf(
        "page ", "consolidated account", "www.", "camsonline",
        "this is a computer", "important", "note:", "disclaimer", "folio no",
        "scheme name", "cost value", "market value", "unit balance", "nav date",
        "closing unit", "registrar"
    )

    fun parse(rawText: String): CasParseResult {
        val normalized = normalize(rawText)
        val asOf = normalizeDate(asOnRe.find(normalized)?.groupValues?.get(1).orEmpty())

        val detailed = parseDetailed(normalized)
        val summary = parseSummary(normalized)

        val looksDetailed = normalized.contains("Folio No:", ignoreCase = true) ||
            normalized.contains("Opening Unit Balance", ignoreCase = true) ||
            normalized.contains("Total Cost Value", ignoreCase = true)

        // Prefer Detailed when the PDF is clearly Detailed (summary heuristics false-positive on it).
        // Otherwise take whichever recovered more schemes.
        val (holdings, sourceHint) = when {
            looksDetailed && detailed.isNotEmpty() -> detailed to "Detailed CAS"
            summary.size >= detailed.size && summary.isNotEmpty() -> summary to "Summary CAS"
            detailed.isNotEmpty() -> detailed to "Detailed CAS"
            summary.isNotEmpty() -> summary to "Summary CAS"
            else -> emptyList<CasHolding>() to ""
        }

        return CasParseResult(
            holdings = holdings,
            statementAsOf = asOf,
            sourceHint = sourceHint
        )
    }

    private fun normalize(rawText: String): String {
        return rawText
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace('\u00a0', ' ')
            .replace(Regex("""[ \t\u000B\u000C]+"""), " ")
            .replace(Regex(""" *\n *"""), "\n")
    }

    private fun parseDetailed(text: String): List<CasHolding> {
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        val holdings = mutableListOf<CasHolding>()

        var folio = ""
        var schemeName = ""
        var isin = ""
        var units = 0.0
        var nav = 0.0
        var market = 0.0
        var cost: Double? = null
        var asOf = ""
        var collectingScheme = false

        fun resetScheme() {
            schemeName = ""
            isin = ""
            units = 0.0
            nav = 0.0
            market = 0.0
            cost = null
            asOf = ""
            collectingScheme = false
        }

        fun flush() {
            val name = cleanSchemeName(schemeName).ifBlank {
                if (isin.isNotBlank()) "Mutual Fund ($isin)" else ""
            }
            if (name.isBlank() || market <= 0.0) {
                resetScheme()
                return
            }
            val invested = cost?.takeIf { it > 0 } ?: market
            holdings += CasHolding(
                schemeName = name,
                folio = folio.trim(),
                isin = isin,
                investedAmount = invested,
                currentAmount = market,
                units = units,
                nav = nav,
                asOfDate = asOf
            )
            resetScheme()
        }

        for (line in lines) {
            val folioMatch = folioLineRe.find(line)
            if (folioMatch != null) {
                if (schemeName.isNotBlank() && market > 0) flush()
                folio = folioMatch.groupValues[1].replace("\\s+".toRegex(), " ").trim()
                continue
            }

            if (closeBalRe.containsMatchIn(line) ||
                navRe.containsMatchIn(line) ||
                valuationRe.containsMatchIn(line) ||
                costValueRe.containsMatchIn(line)
            ) {
                collectingScheme = false
            }

            closeBalRe.find(line)?.let {
                units = parseAmount(it.groupValues[1])
            }
            navRe.find(line)?.let {
                asOf = normalizeDate(it.groupValues[1])
                nav = parseAmount(it.groupValues[2])
            }
            valuationRe.find(line)?.let {
                asOf = normalizeDate(it.groupValues[1])
                market = parseAmount(it.groupValues[2])
            }
            costValueRe.find(line)?.let {
                cost = parseAmount(it.groupValues[1])
                if (market > 0 && (schemeName.isNotBlank() || isin.isNotBlank())) {
                    flush()
                }
            }

            val looksLikeScheme = schemeHeadRe.matchEntire(line) != null &&
                !line.contains("Folio", ignoreCase = true) &&
                !closeBalRe.containsMatchIn(line) &&
                !navRe.containsMatchIn(line) &&
                !valuationRe.containsMatchIn(line) &&
                !costValueRe.containsMatchIn(line) &&
                !line.contains("Opening Unit Balance", ignoreCase = true) &&
                junkSchemeHints.none { line.lowercase().contains(it) }

            if (looksLikeScheme) {
                if (schemeName.isNotBlank() && market > 0) flush()
                val match = schemeHeadRe.matchEntire(line)!!
                var name = match.groupValues[2].trim()
                isinRe.find(name)?.let { found ->
                    isin = found.groupValues[1]
                    name = name.replace(found.value, "").trim()
                }
                name = name
                    .replace(Regex("""Registrar\s*:.*""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""\(Advisor\s*:.*""", RegexOption.IGNORE_CASE), "")
                    .trim()
                    .trimEnd('-', ' ', ':')
                schemeName = name
                collectingScheme = true
                continue
            }

            if (collectingScheme && schemeName.isNotBlank()) {
                if (line.contains("Opening Unit Balance", ignoreCase = true) ||
                    line.contains("Registrar", ignoreCase = true) ||
                    line.startsWith("Date ", ignoreCase = true)
                ) {
                    collectingScheme = false
                } else if (!line.contains("Folio", ignoreCase = true)) {
                    isinRe.find(line)?.let { found ->
                        isin = found.groupValues[1]
                    }
                    val cleaned = line
                        .replace(isinRe, "")
                        .replace(Regex("""Registrar\s*:.*""", RegexOption.IGNORE_CASE), "")
                        .trim()
                    if (cleaned.isNotBlank() && cleaned.length < 80) {
                        schemeName = "$schemeName $cleaned".trim()
                    }
                }
            }
        }

        if ((schemeName.isNotBlank() || isin.isNotBlank()) && market > 0) flush()
        return dedupeHoldings(holdings)
    }

    private fun parseSummary(text: String): List<CasHolding> {
        // CAMS Summary PDFs often extract as: Folio Market+Scheme / Units Date NAV Registrar+ISIN Cost
        val camsMarketFirst = parseSummaryCamsMarketFirst(text)
        if (camsMarketFirst.size >= 2) {
            return dedupeHoldings(camsMarketFirst)
        }
        // Merge all strategies — row regex alone often recovers only a subset.
        return dedupeHoldings(
            camsMarketFirst +
                parseSummaryRows(text) +
                parseSummaryByIsinWindows(text) +
                parseSummaryByDateAnchors(text) +
                parseSummaryCollapsed(text)
        )
    }

    /**
     * Real CAMS Summary text extract (column order scrambled):
     * ```
     * 1040369504 53,883.86B02GZ - Aditya Birla ...
     * Growth-Direct Plan (Non-Demat)
     * 772.307 10-Jul-2026 69.77 CAMSINF209K01UN8 35,000.000
     * ```
     */
    private fun parseSummaryCamsMarketFirst(text: String): List<CasHolding> {
        val folioStartRe = Regex(
            """^(\d{6,16}(?:/\d{1,4})?)\s+([\d,]+\.\d{2})\s*(.+)$"""
        )
        val detailRe = Regex(
            """^([\d,]+\.\d{1,4})\s+(\d{1,2}-[A-Za-z]{3}-\d{4})\s+([\d,]+\.\d{1,4})\s+(CAMS|KFINTECH|KFIN|KARVY)\s*(INF[0-9A-Z]{9})\s+([\d,]+\.\d{1,4})\s*$""",
            RegexOption.IGNORE_CASE
        )
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        val holdings = mutableListOf<CasHolding>()
        var i = 0
        while (i < lines.size) {
            val start = folioStartRe.matchEntire(lines[i])
            if (start == null) {
                i++
                continue
            }
            val folio = start.groupValues[1]
            if (!CasSummaryTableParser.isValidFolio(folio)) {
                i++
                continue
            }
            val market = parseAmount(start.groupValues[2])
            val schemeParts = mutableListOf(start.groupValues[3].trim())
            i++
            while (i < lines.size) {
                val line = lines[i]
                if (detailRe.matches(line) || folioStartRe.matches(line) || isTotalLine(line)) break
                if (line.length < 160 && !line.equals("(INR)", ignoreCase = true)) {
                    schemeParts += line
                }
                i++
            }
            if (i >= lines.size) break
            val detail = detailRe.matchEntire(lines[i])
            if (detail == null) {
                // Folio line matched but detail did not — advance to avoid a stuck loop.
                continue
            }
            val units = parseAmount(detail.groupValues[1])
            val asOf = normalizeDate(detail.groupValues[2])
            val nav = parseAmount(detail.groupValues[3])
            val isin = detail.groupValues[5].uppercase()
            val cost = parseAmount(detail.groupValues[6])
            if (market <= 0) {
                i++
                continue
            }
            holdings += holdingFromSummaryParts(
                folio = folio,
                isin = isin,
                scheme = schemeParts.joinToString(" "),
                cost = cost,
                units = units,
                asOf = asOf,
                nav = nav,
                market = market
            )
            i++
        }
        return holdings
    }

    private fun parseSummaryRows(text: String): List<CasHolding> {
        val holdings = mutableListOf<CasHolding>()
        for (line in text.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || isTotalLine(trimmed)) continue

            summaryRowRe.find(trimmed)?.let { m ->
                holdings += holdingFromSummaryParts(
                    folio = m.groupValues[1],
                    isin = m.groupValues[2],
                    scheme = m.groupValues[3],
                    cost = parseAmount(m.groupValues[4]),
                    units = parseAmount(m.groupValues[5]),
                    asOf = normalizeDate(m.groupValues[6]),
                    nav = parseAmount(m.groupValues[7]),
                    market = parseAmount(m.groupValues[8])
                )
                return@let
            }

            summaryRowNoIsinRe.find(trimmed)?.let { m ->
                holdings += holdingFromSummaryParts(
                    folio = m.groupValues[1],
                    isin = "",
                    scheme = m.groupValues[3],
                    cost = parseAmount(m.groupValues[4]),
                    units = parseAmount(m.groupValues[5]),
                    asOf = normalizeDate(m.groupValues[6]),
                    nav = parseAmount(m.groupValues[7]),
                    market = parseAmount(m.groupValues[8])
                )
            }
        }
        return holdings
    }

    private fun parseSummaryByIsinWindows(text: String): List<CasHolding> {
        val collapsed = collapse(text)
        val matches = isinRe.findAll(collapsed).toList()
        if (matches.isEmpty()) return emptyList()

        val holdings = mutableListOf<CasHolding>()
        for (index in matches.indices) {
            val match = matches[index]
            val isin = match.groupValues[1]
            val before = collapsed.substring(maxOf(0, match.range.first - 48), match.range.first)
            val afterEnd = if (index + 1 < matches.size) {
                matches[index + 1].range.first
            } else {
                minOf(collapsed.length, match.range.last + 1 + 320)
            }
            var after = collapsed.substring(match.range.last + 1, afterEnd)
            after = registrarTokenRe.replace(after, " ").replace(Regex("""\s+"""), " ").trim()

            val folio = folioNearRe.findAll(before).lastOrNull()?.groupValues?.get(1)
                ?.replace("\\s+".toRegex(), " ")
                .orEmpty()

            val holding = holdingFromIsinWindow(isin, folio, after) ?: continue
            holdings += holding
        }
        return holdings
    }

    /**
     * Anchor on NAV dates (Cost Units | Date | NAV Market) — recovers rows where
     * scheme text / column glue confuses the plain ISIN window parser.
     */
    private fun parseSummaryByDateAnchors(text: String): List<CasHolding> {
        val collapsed = collapse(text)
        val holdings = mutableListOf<CasHolding>()
        val dates = dateRe.findAll(collapsed).toList()
        for (dateMatch in dates) {
            val windowStart = maxOf(0, dateMatch.range.first - 240)
            val windowEnd = minOf(collapsed.length, dateMatch.range.last + 90)
            val before = collapsed.substring(windowStart, dateMatch.range.first)
            val after = collapsed.substring(dateMatch.range.last + 1, windowEnd)

            if (isTotalLine(before.takeLast(40))) continue

            val isin = isinRe.findAll(before).lastOrNull()?.groupValues?.get(1) ?: continue
            val folio = run {
                val isinStartInBefore = before.lastIndexOf(isin)
                val folioRegion = if (isinStartInBefore >= 0) {
                    before.substring(maxOf(0, isinStartInBefore - 40), isinStartInBefore)
                } else {
                    before.takeLast(40)
                }
                folioNearRe.findAll(folioRegion).lastOrNull()?.groupValues?.get(1)
                    ?.replace("\\s+".toRegex(), " ")
                    .orEmpty()
            }

            val amountsBefore = amountRe.findAll(before).map { parseAmount(it.value) }.toList()
            val amountsAfter = amountRe.findAll(after).map { parseAmount(it.value) }.toList()
            if (amountsBefore.size < 2 || amountsAfter.isEmpty()) continue

            val cost = amountsBefore[amountsBefore.size - 2]
            val units = amountsBefore[amountsBefore.size - 1]
            val nav = amountsAfter[0]
            val market = amountsAfter.getOrNull(1) ?: continue
            if (market <= 0) continue

            val schemeRegionEnd = amountRe.findAll(before).toList().let { list ->
                if (list.size >= 2) list[list.size - 2].range.first else before.length
            }
            val isinIdx = before.lastIndexOf(isin)
            val scheme = if (isinIdx >= 0 && isinIdx + isin.length < schemeRegionEnd) {
                cleanSchemeName(before.substring(isinIdx + isin.length, schemeRegionEnd))
            } else {
                ""
            }

            holdings += holdingFromSummaryParts(
                folio = folio,
                isin = isin,
                scheme = scheme.ifBlank { "Mutual Fund ($isin)" },
                cost = cost,
                units = units,
                asOf = normalizeDate(dateMatch.groupValues[1]),
                nav = nav,
                market = market
            )
        }
        return holdings
    }

    private fun holdingFromIsinWindow(isin: String, folio: String, afterRaw: String): CasHolding? {
        val after = afterRaw.trim()
        val dateMatch = dateRe.find(after)
        val asOf = normalizeDate(dateMatch?.groupValues?.get(1).orEmpty())

        val amountMatches = amountRe.findAll(after).toList()
        val amounts = amountMatches.map { parseAmount(it.value) }
        if (amounts.size < 2) return null

        // Prefer Cost/Units before date and NAV/Market after date when possible.
        val parsed = if (dateMatch != null) {
            val beforeDate = after.substring(0, dateMatch.range.first)
            val afterDate = after.substring(dateMatch.range.last + 1)
            val beforeAmts = amountRe.findAll(beforeDate).map { parseAmount(it.value) }.toList()
            val afterAmts = amountRe.findAll(afterDate).map { parseAmount(it.value) }.toList()
            when {
                beforeAmts.size >= 2 && afterAmts.size >= 2 -> SummaryAmounts(
                    cost = beforeAmts[beforeAmts.size - 2],
                    units = beforeAmts[beforeAmts.size - 1],
                    nav = afterAmts[0],
                    market = afterAmts[1]
                )
                beforeAmts.size >= 2 && afterAmts.size == 1 -> null // incomplete
                else -> amountsFromSummaryList(amounts)
            }
        } else {
            amountsFromSummaryList(amounts)
        } ?: return null

        val schemeEnd = amountMatches.firstOrNull()?.range?.first
            ?: dateMatch?.range?.first
            ?: minOf(after.length, 120)
        var scheme = cleanSchemeName(after.substring(0, schemeEnd.coerceIn(0, after.length)))
        if (scheme.isBlank() || junkSchemeHints.any { scheme.lowercase().contains(it) }) {
            scheme = "Mutual Fund ($isin)"
        }

        return holdingFromSummaryParts(
            folio = folio,
            isin = isin,
            scheme = scheme,
            cost = parsed.cost,
            units = parsed.units,
            asOf = asOf,
            nav = parsed.nav,
            market = parsed.market
        )
    }

    private fun parseSummaryCollapsed(text: String): List<CasHolding> {
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        val holdings = mutableListOf<CasHolding>()
        var i = 0
        while (i < lines.size) {
            val folioMatch = folioNearRe.matchEntire(lines[i].replace(" ", ""))
                ?: folioNearRe.find(lines[i])
            val looksLikeFolioStart = folioMatch != null &&
                lines[i].length <= 28 &&
                !lines[i].contains("INF", ignoreCase = true) &&
                amountRe.findAll(lines[i]).none()

            if (!looksLikeFolioStart) {
                i++
                continue
            }

            val folio = folioMatch!!.groupValues[1].replace("\\s+".toRegex(), " ")
            val block = StringBuilder(lines[i])
            var j = i + 1
            while (j < lines.size && j <= i + 14) {
                if (isTotalLine(lines[j])) break
                val nextFolioOnly = folioNearRe.matchEntire(lines[j].replace(" ", "")) != null &&
                    lines[j].length <= 28 &&
                    !lines[j].contains("INF", ignoreCase = true) &&
                    amountRe.findAll(lines[j]).none()
                if (nextFolioOnly && j > i + 1) break
                block.append(' ').append(lines[j])
                if (isinRe.containsMatchIn(block) && amountRe.findAll(block).count() >= 2) {
                    // Keep reading a bit more so NAV/market after a wrapped date are included.
                    if (amountRe.findAll(block).count() >= 4 || dateRe.containsMatchIn(block)) {
                        j++
                        // one extra line for trailing market value / registrar
                        if (j < lines.size && j <= i + 14 && !isTotalLine(lines[j])) {
                            val maybeNext = lines[j]
                            if (amountRe.containsMatchIn(maybeNext) || registrarTokenRe.containsMatchIn(maybeNext)) {
                                block.append(' ').append(maybeNext)
                                j++
                            }
                        }
                        break
                    }
                }
                j++
            }

            val window = block.toString()
            val isin = isinRe.find(window)?.groupValues?.get(1).orEmpty()
            val afterIsin = if (isin.isNotBlank()) {
                window.substringAfter(isin)
            } else {
                window.substringAfter(folio)
            }
            val holding = if (isin.isNotBlank()) {
                holdingFromIsinWindow(isin, folio, afterIsin)
            } else {
                val date = normalizeDate(dateRe.find(afterIsin)?.groupValues?.get(1).orEmpty())
                val amounts = amountRe.findAll(afterIsin).map { parseAmount(it.value) }.toList()
                val parsed = amountsFromSummaryList(amounts)
                if (parsed == null) {
                    null
                } else {
                    val schemeEnd = amountRe.find(afterIsin)?.range?.first ?: minOf(afterIsin.length, 100)
                    val scheme = cleanSchemeName(afterIsin.substring(0, schemeEnd))
                    if (scheme.isBlank()) {
                        null
                    } else {
                        holdingFromSummaryParts(
                            folio = folio,
                            isin = "",
                            scheme = scheme,
                            cost = parsed.cost,
                            units = parsed.units,
                            asOf = date,
                            nav = parsed.nav,
                            market = parsed.market
                        )
                    }
                }
            }
            if (holding != null) holdings += holding
            i = maxOf(i + 1, j)
        }
        return holdings
    }

    private data class SummaryAmounts(
        val cost: Double,
        val units: Double,
        val nav: Double,
        val market: Double
    )

    private fun amountsFromSummaryList(amounts: List<Double>): SummaryAmounts? {
        if (amounts.size < 2) return null
        return when {
            amounts.size >= 4 -> SummaryAmounts(
                cost = amounts[0],
                units = amounts[1],
                nav = amounts[amounts.size - 2],
                market = amounts.last()
            )
            amounts.size == 3 -> SummaryAmounts(
                cost = amounts[0],
                units = amounts[1],
                nav = 0.0,
                market = amounts[2]
            )
            else -> SummaryAmounts(
                cost = amounts[0],
                units = 0.0,
                nav = 0.0,
                market = amounts[1]
            )
        }.takeIf { it.market > 0 }
    }

    private fun holdingFromSummaryParts(
        folio: String,
        isin: String,
        scheme: String,
        cost: Double,
        units: Double,
        asOf: String,
        nav: Double,
        market: Double
    ): CasHolding {
        val cleanedFolio = folio.replace("\\s+".toRegex(), "").trim()
        val cleanedScheme = cleanSchemeName(scheme).ifBlank {
            if (isin.isNotBlank()) "Mutual Fund ($isin)" else "Mutual Fund"
        }
        return CasHolding(
            schemeName = cleanedScheme,
            folio = if (CasSummaryTableParser.isValidFolio(cleanedFolio)) cleanedFolio else "",
            isin = isin,
            investedAmount = if (cost > 0) cost else market,
            currentAmount = market,
            units = units,
            nav = nav,
            asOfDate = asOf
        )
    }

    private fun holdingKey(holding: CasHolding): String {
        val folio = holding.folio.replace(" ", "")
        return when {
            folio.isNotBlank() && holding.isin.isNotBlank() -> "$folio|${holding.isin}"
            folio.isNotBlank() -> "$folio|${holding.schemeName.lowercase()}|${"%.2f".format(holding.currentAmount)}"
            // Never key by ISIN alone — different folios can share one ISIN.
            holding.isin.isNotBlank() ->
                "${holding.isin}|${"%.2f".format(holding.investedAmount)}|${"%.2f".format(holding.currentAmount)}"
            else -> "${holding.schemeName.lowercase()}|${"%.2f".format(holding.currentAmount)}"
        }
    }

    private fun dedupeHoldings(holdings: List<CasHolding>): List<CasHolding> {
        val merged = LinkedHashMap<String, CasHolding>()
        for (holding in holdings) {
            if (holding.currentAmount <= 0) continue
            val key = holdingKey(holding)
            val existing = merged[key]
            if (existing == null) {
                merged[key] = holding
            } else {
                merged[key] = pickBetter(existing, holding)
            }
        }
        return merged.values.toList()
    }

    private fun pickBetter(a: CasHolding, b: CasHolding): CasHolding {
        val aScore = (if (a.schemeName.startsWith("Mutual Fund")) 0 else 2) +
            (if (a.investedAmount > 0 && a.investedAmount != a.currentAmount) 1 else 0) +
            (if (a.units > 0) 1 else 0) +
            (if (a.folio.isNotBlank()) 2 else 0)
        val bScore = (if (b.schemeName.startsWith("Mutual Fund")) 0 else 2) +
            (if (b.investedAmount > 0 && b.investedAmount != b.currentAmount) 1 else 0) +
            (if (b.units > 0) 1 else 0) +
            (if (b.folio.isNotBlank()) 2 else 0)
        return when {
            bScore > aScore -> b
            aScore > bScore -> a
            b.currentAmount >= a.currentAmount -> b
            else -> a
        }
    }

    private fun collapse(text: String): String =
        text.replace('\n', ' ').replace(Regex("""\s+"""), " ")

    private fun isTotalLine(line: String): Boolean {
        return line.contains(Regex("""(?i)(?:^|\s)(?:grand\s+|sub\s+|portfolio\s+)?total\b"""))
    }

    private fun cleanSchemeName(name: String): String {
        return name
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""Registrar\s*:.*""", RegexOption.IGNORE_CASE), "")
            .replace(registrarTokenRe, "")
            .trim()
            .trimEnd('-', ':', ' ')
    }

    private fun normalizeDate(raw: String): String {
        if (raw.isBlank()) return ""
        return raw.replace(Regex("""\s*[-/]\s*"""), "-").trim()
    }

    fun parseAmount(raw: String): Double {
        val cleaned = raw.replace(",", "").replace(" ", "").trim()
        return cleaned.toDoubleOrNull() ?: 0.0
    }
}
