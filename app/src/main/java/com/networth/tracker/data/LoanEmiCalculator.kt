package com.networth.tracker.data

import kotlin.math.pow

enum class TenureUnit {
    YEARS,
    MONTHS
}

enum class PrepaymentFrequency {
    ONE_TIME,
    MONTHLY,
    YEARLY
}

enum class PrepaymentEffect {
    REDUCE_TENURE,
    REDUCE_EMI
}

data class PrepaymentEntry(
    val amount: Double,
    val frequency: PrepaymentFrequency,
    val startMonth: Int,
    val endMonth: Int? = null
)

data class EmiMonthRow(
    val month: Int,
    val principal: Double,
    val interest: Double,
    val emi: Double,
    val prepayment: Double = 0.0,
    val totalPayment: Double = emi,
    val remainingPrincipal: Double
)

data class EmiYearRow(
    val year: Int,
    val principal: Double,
    val interest: Double,
    val emi: Double,
    val prepayment: Double,
    val totalPayment: Double,
    val remainingPrincipal: Double,
    val months: List<EmiMonthRow>
)

data class EmiResult(
    val monthlyEmi: Double,
    val totalPrincipal: Double,
    val totalInterest: Double,
    val totalPrepayment: Double,
    val totalPayment: Double,
    val tenureMonths: Int,
    val originalTenureMonths: Int,
    val interestSaved: Double,
    val baselineInterest: Double,
    val prepaymentEffect: PrepaymentEffect,
    val yearRows: List<EmiYearRow>
)

object LoanEmiCalculator {
    fun calculate(
        principal: Double,
        annualRatePercent: Double,
        tenure: Int,
        tenureUnit: TenureUnit,
        prepayments: List<PrepaymentEntry> = emptyList(),
        prepaymentEffect: PrepaymentEffect = PrepaymentEffect.REDUCE_TENURE
    ): EmiResult? {
        if (principal <= 0 || tenure <= 0 || annualRatePercent < 0) return null
        if (prepayments.any { it.amount <= 0 || it.startMonth < 1 }) return null

        val tenureMonths = when (tenureUnit) {
            TenureUnit.YEARS -> tenure * 12
            TenureUnit.MONTHS -> tenure
        }

        val monthlyRate = annualRatePercent / 12.0 / 100.0
        val emi = computeEmi(principal, monthlyRate, tenureMonths)

        val baselineRows = buildSchedule(principal, monthlyRate, emi, tenureMonths)
        val baselineInterest = baselineRows.sumOf { it.interest }

        val monthRows = if (prepayments.isEmpty()) {
            baselineRows
        } else {
            buildScheduleWithPrepayments(
                principal = principal,
                monthlyRate = monthlyRate,
                emi = emi,
                originalTenureMonths = tenureMonths,
                prepayments = prepayments,
                effect = prepaymentEffect
            )
        }

        val yearRows = groupByYear(monthRows)
        val totalInterest = monthRows.sumOf { it.interest }
        val totalPrepayment = monthRows.sumOf { it.prepayment }

        return EmiResult(
            monthlyEmi = emi,
            totalPrincipal = principal,
            totalInterest = totalInterest,
            totalPrepayment = totalPrepayment,
            totalPayment = monthRows.sumOf { it.totalPayment },
            tenureMonths = monthRows.size,
            originalTenureMonths = tenureMonths,
            interestSaved = (baselineInterest - totalInterest).coerceAtLeast(0.0),
            baselineInterest = baselineInterest,
            prepaymentEffect = prepaymentEffect,
            yearRows = yearRows
        )
    }

    private fun computeEmi(principal: Double, monthlyRate: Double, tenureMonths: Int): Double {
        if (tenureMonths <= 0) return 0.0
        if (monthlyRate == 0.0) return principal / tenureMonths
        val factor = (1 + monthlyRate).pow(tenureMonths.toDouble())
        return principal * monthlyRate * factor / (factor - 1)
    }

    private fun prepaymentForMonth(month: Int, entries: List<PrepaymentEntry>): Double {
        return entries.sumOf { entry ->
            when (entry.frequency) {
                PrepaymentFrequency.ONE_TIME ->
                    if (month == entry.startMonth) entry.amount else 0.0
                PrepaymentFrequency.MONTHLY -> {
                    val endMonth = entry.endMonth ?: Int.MAX_VALUE
                    if (month in entry.startMonth..endMonth) entry.amount else 0.0
                }
                PrepaymentFrequency.YEARLY -> {
                    val endMonth = entry.endMonth ?: Int.MAX_VALUE
                    if (month in entry.startMonth..endMonth &&
                        (month - entry.startMonth) % 12 == 0
                    ) {
                        entry.amount
                    } else {
                        0.0
                    }
                }
            }
        }
    }

    private fun buildSchedule(
        principal: Double,
        monthlyRate: Double,
        emi: Double,
        tenureMonths: Int
    ): List<EmiMonthRow> {
        var balance = principal
        val rows = mutableListOf<EmiMonthRow>()

        for (month in 1..tenureMonths) {
            val row = buildMonthRow(balance, monthlyRate, emi, month, prepayment = 0.0)
            balance = row.remainingPrincipal
            rows += row
            if (balance <= 0.0) break
        }

        return rows
    }

    private fun buildScheduleWithPrepayments(
        principal: Double,
        monthlyRate: Double,
        emi: Double,
        originalTenureMonths: Int,
        prepayments: List<PrepaymentEntry>,
        effect: PrepaymentEffect
    ): List<EmiMonthRow> {
        var balance = principal
        var currentEmi = emi
        val rows = mutableListOf<EmiMonthRow>()
        val maxMonths = originalTenureMonths * 3
        var month = 1

        while (balance > 0.01 && month <= maxMonths) {
            val row = buildMonthRow(
                balance = balance,
                monthlyRate = monthlyRate,
                emi = currentEmi,
                month = month,
                prepayment = prepaymentForMonth(month, prepayments)
            )
            rows += row
            balance = row.remainingPrincipal

            if (effect == PrepaymentEffect.REDUCE_EMI && row.prepayment > 0 && balance > 0.01) {
                val remainingMonths = originalTenureMonths - month
                if (remainingMonths > 0) {
                    currentEmi = computeEmi(balance, monthlyRate, remainingMonths)
                }
            }

            month++
        }

        return rows
    }

    private fun buildMonthRow(
        balance: Double,
        monthlyRate: Double,
        emi: Double,
        month: Int,
        prepayment: Double
    ): EmiMonthRow {
        val interest = balance * monthlyRate
        var principalComponent = (emi - interest).coerceAtLeast(0.0)
        if (principalComponent > balance) {
            principalComponent = balance
        }

        var remaining = balance - principalComponent
        val appliedPrepayment = prepayment.coerceAtMost(remaining)
        remaining -= appliedPrepayment

        val actualEmi = principalComponent + interest

        return EmiMonthRow(
            month = month,
            principal = principalComponent,
            interest = interest,
            emi = actualEmi,
            prepayment = appliedPrepayment,
            totalPayment = actualEmi + appliedPrepayment,
            remainingPrincipal = remaining.coerceAtLeast(0.0)
        )
    }

    private fun groupByYear(monthRows: List<EmiMonthRow>): List<EmiYearRow> {
        return monthRows
            .groupBy { ((it.month - 1) / 12) + 1 }
            .toSortedMap()
            .map { (year, months) ->
                EmiYearRow(
                    year = year,
                    principal = months.sumOf { it.principal },
                    interest = months.sumOf { it.interest },
                    emi = months.sumOf { it.emi },
                    prepayment = months.sumOf { it.prepayment },
                    totalPayment = months.sumOf { it.totalPayment },
                    remainingPrincipal = months.last().remainingPrincipal,
                    months = months
                )
            }
    }
}
