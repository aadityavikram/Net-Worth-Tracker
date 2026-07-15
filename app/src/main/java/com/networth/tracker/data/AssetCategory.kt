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
    HOME_LOAN("Home Loan", true, Currency.INR),
    VEHICLE_LOAN("Vehicle Loan", true, Currency.INR),
    PERSONAL_LOAN("Personal Loan", true, Currency.INR);

    val isLoan: Boolean
        get() = this == HOME_LOAN || this == VEHICLE_LOAN || this == PERSONAL_LOAN

    companion object {
        val assets = entries.filter { !it.isLiability }
        val liabilities = entries.filter { it.isLiability }
        val loans = listOf(HOME_LOAN, VEHICLE_LOAN, PERSONAL_LOAN)
    }
}

enum class AssetAddContext {
    ASSETS,
    LIABILITIES;

    val allowedCategories: List<AssetCategory>
        get() = when (this) {
            ASSETS -> AssetCategory.assets
            LIABILITIES -> AssetCategory.liabilities
        }
}

enum class Currency(val code: String, val symbol: String) {
    INR("INR", "₹"),
    USD("USD", "$")
}
