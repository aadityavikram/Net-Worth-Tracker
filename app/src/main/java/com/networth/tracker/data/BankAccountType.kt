package com.networth.tracker.data

enum class BankAccountType(val displayName: String, val isLiability: Boolean) {
    SAVINGS("Savings", false),
    CURRENT("Current", false),
    CREDIT_CARD("Credit Card", true),
    OVERDRAFT("Overdraft", true);

    companion object {
        val assets = entries.filter { !it.isLiability }
        val liabilities = entries.filter { it.isLiability }
    }
}
