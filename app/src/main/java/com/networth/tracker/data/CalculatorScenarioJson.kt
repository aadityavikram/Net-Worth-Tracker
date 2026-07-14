package com.networth.tracker.data

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.util.Date
import java.util.Locale

data class InvestmentScenarioInput(
    val mode: InvestmentMode,
    val amount: Double,
    val annualReturnPercent: Double,
    val years: Int,
    val stepUpPercent: Double
)

data class InvestmentScenarioExport(
    val input: InvestmentScenarioInput,
    val output: List<InvestmentYearRow>
)

data class LoanScenarioPrepayment(
    val amount: Double,
    val frequency: PrepaymentFrequency,
    val startMonth: YearMonth,
    val endMonth: YearMonth?
)

data class LoanScenarioInput(
    val loanAmount: Double,
    val interestRate: Double,
    val tenure: Int,
    val tenureUnit: TenureUnit,
    val loanStartMonth: YearMonth,
    val prepaymentEffect: PrepaymentEffect,
    val prepayments: List<LoanScenarioPrepayment>
)

data class LoanScenarioExport(
    val input: LoanScenarioInput,
    val output: EmiResult
)

sealed class CalculatorScenarioParseResult<out T> {
    data class Success<T>(val value: T) : CalculatorScenarioParseResult<T>()
    data class Error(val message: String) : CalculatorScenarioParseResult<Nothing>()
}

object CalculatorScenarioJson {
    const val INVESTMENT_TYPE = "investment_projection"
    const val LOAN_EMI_TYPE = "loan_emi"
    private const val VERSION = 1

    fun investmentFileName(): String = fileName("investment_projection")

    fun loanEmiFileName(): String = fileName("loan_emi")

    fun serializeInvestment(export: InvestmentScenarioExport): String {
        val root = JSONObject().apply {
            put("type", INVESTMENT_TYPE)
            put("version", VERSION)
            put("savedAt", System.currentTimeMillis())
            put("input", JSONObject().apply {
                put("mode", export.input.mode.name)
                put("amount", export.input.amount)
                put("annualReturnPercent", export.input.annualReturnPercent)
                put("years", export.input.years)
                put("stepUpPercent", export.input.stepUpPercent)
            })
            put("output", JSONObject().apply {
                put("yearRows", JSONArray().apply {
                    export.output.forEach { row ->
                        put(JSONObject().apply {
                            put("year", row.year)
                            put("invested", row.invested)
                            put("yearReturn", row.yearReturn)
                            put("final", row.final)
                        })
                    }
                })
            })
        }
        return root.toString(2)
    }

    fun parseInvestment(json: String): CalculatorScenarioParseResult<InvestmentScenarioExport> {
        return try {
            val root = JSONObject(json)
            val type = root.optString("type")
            if (type.isNotEmpty() && type != INVESTMENT_TYPE) {
                return CalculatorScenarioParseResult.Error("This file is not an investment projection scenario")
            }

            val inputObj = root.optJSONObject("input")
                ?: return CalculatorScenarioParseResult.Error("Missing input in scenario file")

            val modeName = inputObj.getString("mode")
            val mode = runCatching { InvestmentMode.valueOf(modeName) }.getOrNull()
                ?: return CalculatorScenarioParseResult.Error("Unknown investment mode: $modeName")

            val amount = inputObj.getDouble("amount")
            val annualReturnPercent = inputObj.getDouble("annualReturnPercent")
            val years = inputObj.getInt("years")
            val stepUpPercent = inputObj.optDouble("stepUpPercent", 0.0)

            if (amount <= 0 || years <= 0) {
                return CalculatorScenarioParseResult.Error("Scenario has invalid amount or years")
            }

            val input = InvestmentScenarioInput(
                mode = mode,
                amount = amount,
                annualReturnPercent = annualReturnPercent,
                years = years,
                stepUpPercent = stepUpPercent
            )

            val output = InvestmentProjectionCalculator.calculate(
                mode = input.mode,
                amount = input.amount,
                annualReturnPercent = input.annualReturnPercent,
                years = input.years,
                stepUpPercent = if (input.mode == InvestmentMode.SIP) input.stepUpPercent else 0.0
            )

            CalculatorScenarioParseResult.Success(
                InvestmentScenarioExport(input = input, output = output)
            )
        } catch (e: Exception) {
            CalculatorScenarioParseResult.Error(e.message ?: "Could not read investment scenario file")
        }
    }

    fun serializeLoanEmi(export: LoanScenarioExport): String {
        val root = JSONObject().apply {
            put("type", LOAN_EMI_TYPE)
            put("version", VERSION)
            put("savedAt", System.currentTimeMillis())
            put("input", JSONObject().apply {
                put("loanAmount", export.input.loanAmount)
                put("interestRate", export.input.interestRate)
                put("tenure", export.input.tenure)
                put("tenureUnit", export.input.tenureUnit.name)
                put("loanStartMonth", export.input.loanStartMonth.toString())
                put("prepaymentEffect", export.input.prepaymentEffect.name)
                put("prepayments", JSONArray().apply {
                    export.input.prepayments.forEach { entry ->
                        put(JSONObject().apply {
                            put("amount", entry.amount)
                            put("frequency", entry.frequency.name)
                            put("startMonth", entry.startMonth.toString())
                            if (entry.endMonth != null) {
                                put("endMonth", entry.endMonth.toString())
                            } else {
                                put("endMonth", JSONObject.NULL)
                            }
                        })
                    }
                })
            })
            put("output", serializeEmiResult(export.output))
        }
        return root.toString(2)
    }

    fun parseLoanEmi(json: String): CalculatorScenarioParseResult<LoanScenarioExport> {
        return try {
            val root = JSONObject(json)
            val type = root.optString("type")
            if (type.isNotEmpty() && type != LOAN_EMI_TYPE) {
                return CalculatorScenarioParseResult.Error("This file is not a loan EMI scenario")
            }

            val inputObj = root.optJSONObject("input")
                ?: return CalculatorScenarioParseResult.Error("Missing input in scenario file")

            val tenureUnitName = inputObj.getString("tenureUnit")
            val tenureUnit = runCatching { TenureUnit.valueOf(tenureUnitName) }.getOrNull()
                ?: return CalculatorScenarioParseResult.Error("Unknown tenure unit: $tenureUnitName")

            val effectName = inputObj.optString("prepaymentEffect", PrepaymentEffect.REDUCE_TENURE.name)
            val prepaymentEffect = runCatching { PrepaymentEffect.valueOf(effectName) }.getOrNull()
                ?: return CalculatorScenarioParseResult.Error("Unknown prepayment effect: $effectName")

            val loanStartMonth = runCatching {
                YearMonth.parse(inputObj.getString("loanStartMonth"))
            }.getOrNull()
                ?: return CalculatorScenarioParseResult.Error("Invalid loan start month")

            val loanAmount = inputObj.getDouble("loanAmount")
            val interestRate = inputObj.getDouble("interestRate")
            val tenure = inputObj.getInt("tenure")

            if (loanAmount <= 0 || interestRate < 0 || tenure <= 0) {
                return CalculatorScenarioParseResult.Error("Scenario has invalid loan inputs")
            }

            val prepaymentsArray = inputObj.optJSONArray("prepayments") ?: JSONArray()
            val prepayments = buildList {
                for (i in 0 until prepaymentsArray.length()) {
                    val entry = prepaymentsArray.getJSONObject(i)
                    val frequencyName = entry.getString("frequency")
                    val frequency = runCatching { PrepaymentFrequency.valueOf(frequencyName) }.getOrNull()
                        ?: return CalculatorScenarioParseResult.Error(
                            "Unknown prepayment frequency: $frequencyName"
                        )
                    val startMonth = runCatching {
                        YearMonth.parse(entry.getString("startMonth"))
                    }.getOrNull()
                        ?: return CalculatorScenarioParseResult.Error("Invalid prepayment start month")
                    val endMonth = if (entry.isNull("endMonth")) {
                        null
                    } else {
                        runCatching { YearMonth.parse(entry.getString("endMonth")) }.getOrNull()
                    }
                    add(
                        LoanScenarioPrepayment(
                            amount = entry.getDouble("amount"),
                            frequency = frequency,
                            startMonth = startMonth,
                            endMonth = endMonth
                        )
                    )
                }
            }

            val input = LoanScenarioInput(
                loanAmount = loanAmount,
                interestRate = interestRate,
                tenure = tenure,
                tenureUnit = tenureUnit,
                loanStartMonth = loanStartMonth,
                prepaymentEffect = prepaymentEffect,
                prepayments = prepayments
            )

            val relativePrepayments = prepayments.map { entry ->
                val start = monthsBetween(loanStartMonth, entry.startMonth)
                val end = entry.endMonth?.let { monthsBetween(loanStartMonth, it) }
                PrepaymentEntry(
                    amount = entry.amount,
                    frequency = entry.frequency,
                    startMonth = start,
                    endMonth = if (entry.frequency == PrepaymentFrequency.ONE_TIME) null else end
                )
            }

            val output = LoanEmiCalculator.calculate(
                principal = input.loanAmount,
                annualRatePercent = input.interestRate,
                tenure = input.tenure,
                tenureUnit = input.tenureUnit,
                prepayments = relativePrepayments,
                prepaymentEffect = input.prepaymentEffect
            ) ?: return CalculatorScenarioParseResult.Error("Could not recalculate loaded loan scenario")

            CalculatorScenarioParseResult.Success(
                LoanScenarioExport(input = input, output = output)
            )
        } catch (e: Exception) {
            CalculatorScenarioParseResult.Error(e.message ?: "Could not read loan EMI scenario file")
        }
    }

    private fun serializeEmiResult(result: EmiResult): JSONObject {
        return JSONObject().apply {
            put("monthlyEmi", result.monthlyEmi)
            put("totalPrincipal", result.totalPrincipal)
            put("totalInterest", result.totalInterest)
            put("totalPrepayment", result.totalPrepayment)
            put("totalPayment", result.totalPayment)
            put("tenureMonths", result.tenureMonths)
            put("originalTenureMonths", result.originalTenureMonths)
            put("interestSaved", result.interestSaved)
            put("baselineInterest", result.baselineInterest)
            put("prepaymentEffect", result.prepaymentEffect.name)
            put("yearRows", JSONArray().apply {
                result.yearRows.forEach { yearRow ->
                    put(JSONObject().apply {
                        put("year", yearRow.year)
                        put("principal", yearRow.principal)
                        put("interest", yearRow.interest)
                        put("emi", yearRow.emi)
                        put("prepayment", yearRow.prepayment)
                        put("totalPayment", yearRow.totalPayment)
                        put("remainingPrincipal", yearRow.remainingPrincipal)
                        put("months", JSONArray().apply {
                            yearRow.months.forEach { monthRow ->
                                put(JSONObject().apply {
                                    put("month", monthRow.month)
                                    put("principal", monthRow.principal)
                                    put("interest", monthRow.interest)
                                    put("emi", monthRow.emi)
                                    put("prepayment", monthRow.prepayment)
                                    put("totalPayment", monthRow.totalPayment)
                                    put("remainingPrincipal", monthRow.remainingPrincipal)
                                })
                            }
                        })
                    })
                }
            })
        }
    }

    private fun monthsBetween(loanStart: YearMonth, target: YearMonth): Int =
        (target.year - loanStart.year) * 12 + (target.monthValue - loanStart.monthValue) + 1

    private fun fileName(prefix: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "${prefix}_$timestamp.json"
    }
}
