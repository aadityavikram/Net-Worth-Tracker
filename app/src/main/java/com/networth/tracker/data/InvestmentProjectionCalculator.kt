package com.networth.tracker.data

import kotlin.math.pow

enum class InvestmentMode {
    SIP,
    LUMPSUM
}

data class InvestmentYearRow(
    val year: Int,
    val invested: Double,
    val yearReturn: Double,
    val final: Double
)

object InvestmentProjectionCalculator {
    fun calculate(
        mode: InvestmentMode,
        amount: Double,
        annualReturnPercent: Double,
        years: Int,
        stepUpPercent: Double
    ): List<InvestmentYearRow> {
        if (amount <= 0 || years <= 0) return emptyList()

        return when (mode) {
            InvestmentMode.LUMPSUM -> calculateLumpsum(amount, annualReturnPercent, years)
            InvestmentMode.SIP -> calculateSip(amount, annualReturnPercent, years, stepUpPercent)
        }
    }

    /**
     * Groww uses the effective monthly rate so that 12 months compound back to the
     * stated annual return: (1 + annual)^(1/12) - 1, not annual / 12.
     */
    private fun monthlyRate(annualReturnPercent: Double): Double {
        val annualRate = annualReturnPercent / 100.0
        return (1 + annualRate).pow(1.0 / 12.0) - 1
    }

    private fun calculateLumpsum(
        amount: Double,
        annualReturnPercent: Double,
        years: Int
    ): List<InvestmentYearRow> {
        val monthlyRate = monthlyRate(annualReturnPercent)
        var balance = amount
        val rows = mutableListOf<InvestmentYearRow>()

        for (year in 1..years) {
            repeat(12) {
                balance *= (1 + monthlyRate)
            }
            rows += InvestmentYearRow(
                year = year,
                invested = amount,
                yearReturn = balance - amount,
                final = balance
            )
        }

        return rows
    }

    private fun calculateSip(
        monthlyAmount: Double,
        annualReturnPercent: Double,
        years: Int,
        stepUpPercent: Double
    ): List<InvestmentYearRow> {
        val monthlyRate = monthlyRate(annualReturnPercent)
        val stepUpMultiplier = 1 + stepUpPercent / 100.0
        var balance = 0.0
        var totalInvested = 0.0
        val rows = mutableListOf<InvestmentYearRow>()

        for (year in 1..years) {
            val yearMonthlyAmount = monthlyAmount * stepUpMultiplier.pow(year - 1)

            repeat(12) {
                balance = (balance + yearMonthlyAmount) * (1 + monthlyRate)
                totalInvested += yearMonthlyAmount
            }

            rows += InvestmentYearRow(
                year = year,
                invested = totalInvested,
                yearReturn = balance - totalInvested,
                final = balance
            )
        }

        return rows
    }
}
