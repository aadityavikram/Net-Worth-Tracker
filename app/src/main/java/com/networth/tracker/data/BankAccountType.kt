package com.networth.tracker.data

enum class BankAccountType(val displayName: String, val isLiability: Boolean) {
    SAVINGS("Savings", false),
    CURRENT("Current", false),
    SALARY("Salary", false),
    CREDIT_CARD("Credit Card", true),
    OVERDRAFT("Overdraft Balance", true);

    companion object {
        val assets = entries.filter { !it.isLiability }
        val liabilities = entries.filter { it.isLiability }
    }
}

enum class BankAccountAddContext {
    ASSETS,
    LIABILITIES;

    val allowedTypes: List<BankAccountType>
        get() = when (this) {
            ASSETS -> listOf(
                BankAccountType.SAVINGS,
                BankAccountType.CURRENT,
                BankAccountType.SALARY
            )
            LIABILITIES -> listOf(
                BankAccountType.CREDIT_CARD,
                BankAccountType.OVERDRAFT
            )
        }
}
