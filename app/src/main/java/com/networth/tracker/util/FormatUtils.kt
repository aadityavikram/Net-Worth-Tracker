package com.networth.tracker.util

import com.networth.tracker.data.AssetCategory
import com.networth.tracker.data.Currency
import java.text.NumberFormat
import java.util.Locale

object FormatUtils {
    private val inrFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    private val usdFormat = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    fun formatInr(amount: Double): String = inrFormat.format(amount)

    fun formatCurrency(amount: Double, currency: Currency): String {
        return when (currency) {
            Currency.INR -> formatInr(amount)
            Currency.USD -> usdFormat.format(amount)
        }
    }

    fun formatCompactInr(amount: Double): String {
        val abs = kotlin.math.abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            abs >= 1_00_00_000 -> "$sign₹${String.format(Locale.US, "%.2f", abs / 1_00_00_000)} Cr"
            abs >= 1_00_000 -> "$sign₹${String.format(Locale.US, "%.2f", abs / 1_00_000)} L"
            else -> formatInr(amount)
        }
    }

    fun formatExchangeRate(rate: Double): String {
        return "₹${String.format(Locale.US, "%.2f", rate)}"
    }

    fun formatRelativeTime(timestamp: Long): String {
        val diffMs = System.currentTimeMillis() - timestamp
        val minutes = diffMs / 60_000
        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            minutes < 24 * 60 -> "${minutes / 60}h ago"
            else -> "${minutes / (24 * 60)}d ago"
        }
    }

    fun formatReturnPercent(percent: Double): String {
        val sign = if (percent > 0) "+" else ""
        return "$sign${String.format(Locale.US, "%.2f", percent)}%"
    }

    fun formatSignedInr(amount: Double): String {
        val sign = when {
            amount > 0 -> "+"
            amount < 0 -> "-"
            else -> ""
        }
        return "$sign${formatCompactInr(kotlin.math.abs(amount))}"
    }
}

fun AssetCategory.iconName(): String = when (this) {
    AssetCategory.US_STOCKS -> "ShowChart"
    AssetCategory.MUTUAL_FUNDS -> "PieChart"
    AssetCategory.INDIAN_STOCKS -> "TrendingUp"
    AssetCategory.EPF -> "AccountBalance"
    AssetCategory.NPS -> "Savings"
    AssetCategory.GOLD -> "Diamond"
    AssetCategory.REAL_ESTATE -> "Home"
    AssetCategory.HOME_LOAN -> "CreditCard"
}
