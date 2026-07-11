package com.networth.tracker.data

enum class AssetCategory(
    val displayName: String,
    val isLiability: Boolean,
    val defaultCurrency: Currency
) {
    US_STOCKS("US Stocks", false, Currency.USD),
    MUTUAL_FUNDS("Mutual Funds", false, Currency.INR),
    INDIAN_STOCKS("Indian Stocks", false, Currency.INR),
    EPF("EPF", false, Currency.INR),
    NPS("NPS", false, Currency.INR),
    GOLD("Gold", false, Currency.INR),
    REAL_ESTATE("Real Estate", false, Currency.INR),
    HOME_LOAN("Home Loan", true, Currency.INR);

    companion object {
        val assets = entries.filter { !it.isLiability }
        val liabilities = entries.filter { it.isLiability }
    }
}

enum class Currency(val code: String, val symbol: String) {
    INR("INR", "₹"),
    USD("USD", "$")
}
