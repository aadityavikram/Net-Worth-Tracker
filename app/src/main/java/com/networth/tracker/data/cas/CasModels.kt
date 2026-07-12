package com.networth.tracker.data.cas

data class CasHolding(
    val schemeName: String,
    val folio: String = "",
    val isin: String = "",
    val investedAmount: Double,
    val currentAmount: Double,
    val units: Double = 0.0,
    val nav: Double = 0.0,
    val asOfDate: String = ""
) {
    val totalReturn: Double
        get() = currentAmount - investedAmount

    val returnPercent: Double
        get() = if (investedAmount > 0) (totalReturn / investedAmount) * 100.0 else 0.0
}

data class CasParseResult(
    val holdings: List<CasHolding>,
    val statementAsOf: String = "",
    val sourceHint: String = ""
) {
    val totalInvested: Double get() = holdings.sumOf { it.investedAmount }
    val totalCurrent: Double get() = holdings.sumOf { it.currentAmount }
    val totalReturn: Double get() = totalCurrent - totalInvested
    val returnPercent: Double
        get() = if (totalInvested > 0) (totalReturn / totalInvested) * 100.0 else 0.0
}

sealed class CasImportException(message: String) : Exception(message) {
    class InvalidPassword : CasImportException("Incorrect CAS password. Use your PAN (e.g. ABCDE1234F).")
    class EmptyHoldings : CasImportException(
        "No mutual fund holdings found in this CAS. Try a Summary or Detailed CAS from CAMS / KFintech / MF Central."
    )
    class ReadFailed(detail: String) : CasImportException(detail)
}
