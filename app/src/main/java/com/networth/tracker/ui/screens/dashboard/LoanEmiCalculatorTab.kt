package com.networth.tracker.ui.screens.dashboard


import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Work
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.networth.tracker.data.AssetAddContext
import com.networth.tracker.data.AssetCategory
import com.networth.tracker.data.AssetEntity
import com.networth.tracker.data.BankAccountAddContext
import com.networth.tracker.data.BankAccountEntity
import com.networth.tracker.data.BankAccountType
import com.networth.tracker.data.BankAccountTypeSummary
import com.networth.tracker.data.CalculatorScenarioJson
import com.networth.tracker.data.CalculatorScenarioParseResult
import com.networth.tracker.data.CategorySummary
import com.networth.tracker.data.ExchangeRateState
import com.networth.tracker.data.LoanScenarioExport
import com.networth.tracker.data.LoanScenarioInput
import com.networth.tracker.data.LoanScenarioPrepayment
import com.networth.tracker.data.NetWorthSummary
import com.networth.tracker.data.AssetRepository
import com.networth.tracker.data.EmiResult
import com.networth.tracker.data.EmiYearRow
import com.networth.tracker.data.InvestmentMode
import com.networth.tracker.data.InvestmentProjectionCalculator
import com.networth.tracker.data.InvestmentYearRow
import com.networth.tracker.data.LoanEmiCalculator
import com.networth.tracker.data.PrepaymentEffect
import com.networth.tracker.data.PrepaymentEntry
import com.networth.tracker.data.PrepaymentFrequency
import com.networth.tracker.data.TenureUnit
import com.networth.tracker.data.ReturnCalculator
import com.networth.tracker.data.ReturnMetrics
import com.networth.tracker.ui.theme.AssetPositiveColor
import com.networth.tracker.ui.theme.LiabilityColor
import com.networth.tracker.util.FormatUtils
import com.networth.tracker.viewmodel.DashboardViewModel

internal data class DraftPrepayment(
    val amountText: String = "",
    val frequency: PrepaymentFrequency = PrepaymentFrequency.ONE_TIME,
    val startMonth: YearMonth? = null,
    val endMonth: YearMonth? = null
)

internal data class AddedPrepayment(
    val id: Long,
    val amount: Double,
    val frequency: PrepaymentFrequency,
    val startMonth: YearMonth,
    val endMonth: YearMonth? = null
)

internal val yearMonthFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)

internal fun formatYearMonth(yearMonth: YearMonth): String = yearMonth.format(yearMonthFormatter)

internal fun emiMonthNumber(loanStart: YearMonth, target: YearMonth): Int =
    (target.year - loanStart.year) * 12 + (target.monthValue - loanStart.monthValue) + 1

internal fun frequencyLabel(frequency: PrepaymentFrequency): String = when (frequency) {
    PrepaymentFrequency.ONE_TIME -> "One-time"
    PrepaymentFrequency.MONTHLY -> "Monthly"
    PrepaymentFrequency.YEARLY -> "Yearly"
}

@Composable
internal fun LoanEmiCalculatorTabContent() {
    val context = LocalContext.current
    var loanAmountText by remember { mutableStateOf("") }
    var interestRateText by remember { mutableStateOf("") }
    var tenureText by remember { mutableStateOf("") }
    var tenureUnit by remember { mutableStateOf(TenureUnit.YEARS) }
    var loanStartMonth by remember { mutableStateOf(YearMonth.now()) }
    var prepaymentEffect by remember { mutableStateOf(PrepaymentEffect.REDUCE_TENURE) }
    var prepaymentsExpanded by remember { mutableStateOf(false) }
    var addedExpanded by remember { mutableStateOf(true) }
    var draftPrepayment by remember { mutableStateOf(DraftPrepayment()) }
    var draftError by remember { mutableStateOf<String?>(null) }
    var addedPrepayments by remember { mutableStateOf<List<AddedPrepayment>>(emptyList()) }
    var result by remember { mutableStateOf<EmiResult?>(null) }
    var tableExpanded by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var pendingDownloadJson by remember { mutableStateOf<String?>(null) }

    fun applyLoanScenario(export: LoanScenarioExport) {
        loanAmountText = formatScenarioNumber(export.input.loanAmount)
        interestRateText = formatScenarioNumber(export.input.interestRate)
        tenureText = export.input.tenure.toString()
        tenureUnit = export.input.tenureUnit
        loanStartMonth = export.input.loanStartMonth
        prepaymentEffect = export.input.prepaymentEffect
        addedPrepayments = export.input.prepayments.mapIndexed { index, entry ->
            AddedPrepayment(
                id = System.nanoTime() + index,
                amount = entry.amount,
                frequency = entry.frequency,
                startMonth = entry.startMonth,
                endMonth = entry.endMonth
            )
        }
        draftPrepayment = DraftPrepayment()
        draftError = null
        prepaymentsExpanded = export.input.prepayments.isNotEmpty()
        addedExpanded = true
        result = export.output
        tableExpanded = true
        errorMessage = null
        statusMessage = "Scenario loaded"
    }

    val downloadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val json = pendingDownloadJson
        pendingDownloadJson = null
        if (uri == null || json == null) return@rememberLauncherForActivityResult
        val saved = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
            } != null
        }.getOrDefault(false)
        statusMessage = if (saved) "Downloaded JSON" else "Could not save JSON file"
        if (!saved) errorMessage = "Could not save JSON file"
    }

    val loadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val json = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.getOrNull()
        if (json.isNullOrBlank()) {
            errorMessage = "Could not read JSON file"
            statusMessage = null
            return@rememberLauncherForActivityResult
        }
        when (val parsed = CalculatorScenarioJson.parseLoanEmi(json)) {
            is CalculatorScenarioParseResult.Success -> applyLoanScenario(parsed.value)
            is CalculatorScenarioParseResult.Error -> {
                errorMessage = parsed.message
                statusMessage = null
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Calculate monthly EMI and repayment schedule for home, car, or personal loans",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    OutlinedTextField(
                        value = loanAmountText,
                        onValueChange = { loanAmountText = it },
                        label = { Text("Loan amount") },
                        placeholder = { Text("e.g. 2500000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = interestRateText,
                        onValueChange = { interestRateText = it },
                        label = { Text("Interest rate (% per annum)") },
                        placeholder = { Text("e.g. 8.5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text("Tenure", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = tenureUnit == TenureUnit.YEARS,
                            onClick = { tenureUnit = TenureUnit.YEARS },
                            label = { Text("Years") }
                        )
                        FilterChip(
                            selected = tenureUnit == TenureUnit.MONTHS,
                            onClick = { tenureUnit = TenureUnit.MONTHS },
                            label = { Text("Months") }
                        )
                    }

                    OutlinedTextField(
                        value = tenureText,
                        onValueChange = { tenureText = it.filter { ch -> ch.isDigit() } },
                        label = {
                            Text(
                                if (tenureUnit == TenureUnit.YEARS) "Loan tenure (years)" else "Loan tenure (months)"
                            )
                        },
                        placeholder = {
                            Text(if (tenureUnit == TenureUnit.YEARS) "e.g. 20" else "e.g. 240")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    MonthYearField(
                        label = "Loan start month",
                        value = loanStartMonth,
                        onValueChange = { loanStartMonth = it }
                    )

                    PrepaymentsSection(
                        expanded = prepaymentsExpanded,
                        onToggleExpanded = { prepaymentsExpanded = !prepaymentsExpanded },
                        prepaymentEffect = prepaymentEffect,
                        onPrepaymentEffectChange = { prepaymentEffect = it },
                        draft = draftPrepayment,
                        onDraftChange = {
                            draftPrepayment = it
                            draftError = null
                        },
                        draftError = draftError,
                        onAddDraft = {
                            val amount = draftPrepayment.amountText.toDoubleOrNull()
                            val start = draftPrepayment.startMonth
                            val end = draftPrepayment.endMonth

                            draftError = when {
                                amount == null || amount <= 0 -> "Enter a valid prepayment amount"
                                start == null -> "Select a start month"
                                start.isBefore(loanStartMonth) ->
                                    "Start month must be on or after loan start"
                                draftPrepayment.frequency != PrepaymentFrequency.ONE_TIME &&
                                    end != null && end.isBefore(start) ->
                                    "End month must be on or after start month"
                                else -> null
                            }

                            if (draftError == null) {
                                addedPrepayments = addedPrepayments + AddedPrepayment(
                                    id = System.nanoTime(),
                                    amount = amount!!,
                                    frequency = draftPrepayment.frequency,
                                    startMonth = start!!,
                                    endMonth = if (draftPrepayment.frequency == PrepaymentFrequency.ONE_TIME) {
                                        null
                                    } else {
                                        end
                                    }
                                )
                                draftPrepayment = DraftPrepayment()
                                addedExpanded = true
                            }
                        },
                        addedPrepayments = addedPrepayments,
                        addedExpanded = addedExpanded,
                        onToggleAddedExpanded = { addedExpanded = !addedExpanded },
                        onRemovePrepayment = { id ->
                            addedPrepayments = addedPrepayments.filter { it.id != id }
                        }
                    )

                    errorMessage?.let { message ->
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    statusMessage?.let { message ->
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = {
                            val principal = loanAmountText.toDoubleOrNull()
                            val interestRate = interestRateText.toDoubleOrNull()
                            val tenure = tenureText.toIntOrNull()

                            val tenureMonths = when (tenureUnit) {
                                TenureUnit.YEARS -> (tenure ?: 0) * 12
                                TenureUnit.MONTHS -> tenure ?: 0
                            }

                            val parsedPrepayments = mutableListOf<PrepaymentEntry>()
                            var prepaymentError: String? = null

                            for ((index, entry) in addedPrepayments.withIndex()) {
                                val startMonth = emiMonthNumber(loanStartMonth, entry.startMonth)
                                val endMonth = entry.endMonth?.let {
                                    emiMonthNumber(loanStartMonth, it)
                                }

                                prepaymentError = when {
                                    startMonth < 1 ->
                                        "Prepayment ${index + 1}: start month is before loan start"
                                    startMonth > tenureMonths ->
                                        "Prepayment ${index + 1}: start month exceeds loan tenure"
                                    endMonth != null && endMonth < startMonth ->
                                        "Prepayment ${index + 1}: end month must be after start month"
                                    else -> null
                                }
                                if (prepaymentError != null) break

                                parsedPrepayments += PrepaymentEntry(
                                    amount = entry.amount,
                                    frequency = entry.frequency,
                                    startMonth = startMonth,
                                    endMonth = if (entry.frequency == PrepaymentFrequency.ONE_TIME) {
                                        null
                                    } else {
                                        endMonth ?: tenureMonths
                                    }
                                )
                            }

                            errorMessage = when {
                                principal == null || principal <= 0 -> "Enter a valid loan amount"
                                interestRate == null || interestRate < 0 -> "Enter a valid interest rate"
                                tenure == null || tenure <= 0 -> "Enter a valid loan tenure"
                                prepaymentError != null -> prepaymentError
                                else -> null
                            }

                            if (errorMessage == null) {
                                result = LoanEmiCalculator.calculate(
                                    principal = principal!!,
                                    annualRatePercent = interestRate!!,
                                    tenure = tenure!!,
                                    tenureUnit = tenureUnit,
                                    prepayments = parsedPrepayments,
                                    prepaymentEffect = prepaymentEffect
                                )
                                tableExpanded = true
                                statusMessage = null
                            } else {
                                result = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Calculate")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val emiResult = result
                                val principal = loanAmountText.toDoubleOrNull()
                                val interestRate = interestRateText.toDoubleOrNull()
                                val tenure = tenureText.toIntOrNull()
                                if (emiResult == null || principal == null || interestRate == null || tenure == null) {
                                    errorMessage = "Calculate first to download JSON"
                                    statusMessage = null
                                    return@Button
                                }
                                val json = CalculatorScenarioJson.serializeLoanEmi(
                                    LoanScenarioExport(
                                        input = LoanScenarioInput(
                                            loanAmount = principal,
                                            interestRate = interestRate,
                                            tenure = tenure,
                                            tenureUnit = tenureUnit,
                                            loanStartMonth = loanStartMonth,
                                            prepaymentEffect = prepaymentEffect,
                                            prepayments = addedPrepayments.map { entry ->
                                                LoanScenarioPrepayment(
                                                    amount = entry.amount,
                                                    frequency = entry.frequency,
                                                    startMonth = entry.startMonth,
                                                    endMonth = entry.endMonth
                                                )
                                            }
                                        ),
                                        output = emiResult
                                    )
                                )
                                pendingDownloadJson = json
                                errorMessage = null
                                downloadLauncher.launch(CalculatorScenarioJson.loanEmiFileName())
                            },
                            enabled = result != null,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download JSON")
                        }
                        OutlinedButton(
                            onClick = {
                                errorMessage = null
                                statusMessage = null
                                loadLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.UploadFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Load JSON")
                        }
                    }
                }
            }
        }

        result?.let { emiResult ->
            item {
                EmiSummaryCard(result = emiResult)
            }
            if (emiResult.yearRows.isNotEmpty()) {
                item {
                    EmiScheduleResultTable(
                        rows = emiResult.yearRows,
                        startYear = loanStartMonth.year,
                        showPrepayment = emiResult.totalPrepayment > 0,
                        expanded = tableExpanded,
                        onToggleExpanded = { tableExpanded = !tableExpanded }
                    )
                }
            }
        }
    }
}

@Composable
internal fun MonthYearField(
    label: String,
    value: YearMonth?,
    onValueChange: (YearMonth) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val display = value?.let { formatYearMonth(it) }.orEmpty()

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("Select month") },
            trailingIcon = {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showPicker = true }
        )
    }

    if (showPicker) {
        MonthYearPickerDialog(
            initial = value ?: YearMonth.now(),
            onDismiss = { showPicker = false },
            onConfirm = {
                onValueChange(it)
                showPicker = false
            }
        )
    }
}

@Composable
internal fun MonthYearPickerDialog(
    initial: YearMonth,
    onDismiss: () -> Unit,
    onConfirm: (YearMonth) -> Unit,
    onClear: (() -> Unit)? = null
) {
    var viewingYear by remember { mutableStateOf(initial.year) }
    var selected by remember { mutableStateOf(initial) }
    val months = listOf(
        "Jan", "Feb", "Mar", "Apr",
        "May", "Jun", "Jul", "Aug",
        "Sep", "Oct", "Nov", "Dec"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select month") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewingYear -= 1 }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous year")
                    }
                    Text(
                        viewingYear.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = { viewingYear += 1 }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next year")
                    }
                }

                months.chunked(4).forEachIndexed { rowIndex, rowMonths ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowMonths.forEachIndexed { colIndex, monthLabel ->
                            val monthValue = rowIndex * 4 + colIndex + 1
                            val candidate = YearMonth.of(viewingYear, monthValue)
                            val isSelected = selected == candidate
                            FilterChip(
                                selected = isSelected,
                                onClick = { selected = candidate },
                                label = {
                                    Text(
                                        monthLabel,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text("OK")
            }
        },
        dismissButton = {
            Row {
                if (onClear != null) {
                    TextButton(onClick = onClear) {
                        Text("Clear")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
internal fun PrepaymentsSection(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    prepaymentEffect: PrepaymentEffect,
    onPrepaymentEffectChange: (PrepaymentEffect) -> Unit,
    draft: DraftPrepayment,
    onDraftChange: (DraftPrepayment) -> Unit,
    draftError: String?,
    onAddDraft: () -> Unit,
    addedPrepayments: List<AddedPrepayment>,
    addedExpanded: Boolean,
    onToggleAddedExpanded: () -> Unit,
    onRemovePrepayment: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Prepayments",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (addedPrepayments.isEmpty()) {
                            "Add one-time, monthly, or yearly prepayments"
                        } else {
                            "${addedPrepayments.size} prepayment${if (addedPrepayments.size == 1) "" else "s"} added"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse prepayments" else "Expand prepayments"
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Prepayment effect", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = prepaymentEffect == PrepaymentEffect.REDUCE_TENURE,
                            onClick = { onPrepaymentEffectChange(PrepaymentEffect.REDUCE_TENURE) },
                            label = { Text("Reduce tenure") }
                        )
                        FilterChip(
                            selected = prepaymentEffect == PrepaymentEffect.REDUCE_EMI,
                            onClick = { onPrepaymentEffectChange(PrepaymentEffect.REDUCE_EMI) },
                            label = { Text("Reduce EMI") }
                        )
                    }

                    if (addedPrepayments.isNotEmpty()) {
                        AddedPrepaymentsSection(
                            prepayments = addedPrepayments,
                            expanded = addedExpanded,
                            onToggleExpanded = onToggleAddedExpanded,
                            onRemove = onRemovePrepayment
                        )
                    }

                    PrepaymentDraftCard(
                        draft = draft,
                        error = draftError,
                        onDraftChange = onDraftChange,
                        onAdd = onAddDraft
                    )
                }
            }
        }
    }
}

@Composable
internal fun AddedPrepaymentsSection(
    prepayments: List<AddedPrepayment>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onRemove: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Prepayments added",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${prepayments.size} ${if (prepayments.size == 1) "entry" else "entries"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider()
                    prepayments.forEachIndexed { index, entry ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        }
                        AddedPrepaymentRow(
                            entry = entry,
                            onRemove = { onRemove(entry.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AddedPrepaymentRow(
    entry: AddedPrepayment,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                FormatUtils.formatCompactInr(entry.amount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                buildString {
                    append(frequencyLabel(entry.frequency))
                    append(" · ")
                    append(formatYearMonth(entry.startMonth))
                    if (entry.frequency != PrepaymentFrequency.ONE_TIME) {
                        append(" → ")
                        append(entry.endMonth?.let { formatYearMonth(it) } ?: "loan end")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove prepayment",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
internal fun PrepaymentDraftCard(
    draft: DraftPrepayment,
    error: String?,
    onDraftChange: (DraftPrepayment) -> Unit,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Add prepayment",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            OutlinedTextField(
                value = draft.amountText,
                onValueChange = { onDraftChange(draft.copy(amountText = it)) },
                label = { Text("Prepayment amount") },
                placeholder = { Text("e.g. 100000") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("Frequency", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = draft.frequency == PrepaymentFrequency.ONE_TIME,
                    onClick = {
                        onDraftChange(
                            draft.copy(
                                frequency = PrepaymentFrequency.ONE_TIME,
                                endMonth = null
                            )
                        )
                    },
                    label = { Text("One-time") }
                )
                FilterChip(
                    selected = draft.frequency == PrepaymentFrequency.MONTHLY,
                    onClick = { onDraftChange(draft.copy(frequency = PrepaymentFrequency.MONTHLY)) },
                    label = { Text("Monthly") }
                )
                FilterChip(
                    selected = draft.frequency == PrepaymentFrequency.YEARLY,
                    onClick = { onDraftChange(draft.copy(frequency = PrepaymentFrequency.YEARLY)) },
                    label = { Text("Yearly") }
                )
            }

            MonthYearField(
                label = "Start month",
                value = draft.startMonth,
                onValueChange = { onDraftChange(draft.copy(startMonth = it)) }
            )

            if (draft.frequency != PrepaymentFrequency.ONE_TIME) {
                ClearableMonthYearField(
                    label = "End month (optional)",
                    value = draft.endMonth,
                    onValueChange = { onDraftChange(draft.copy(endMonth = it)) },
                    onClear = { onDraftChange(draft.copy(endMonth = null)) }
                )
            }

            error?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add")
            }
        }
    }
}

@Composable
internal fun ClearableMonthYearField(
    label: String,
    value: YearMonth?,
    onValueChange: (YearMonth) -> Unit,
    onClear: () -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val display = value?.let { formatYearMonth(it) }.orEmpty()

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("Leave blank for loan end") },
            trailingIcon = {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showPicker = true }
        )
    }

    if (showPicker) {
        MonthYearPickerDialog(
            initial = value ?: YearMonth.now(),
            onDismiss = { showPicker = false },
            onConfirm = {
                onValueChange(it)
                showPicker = false
            },
            onClear = {
                onClear()
                showPicker = false
            }
        )
    }
}

@Composable
internal fun EmiSummaryCard(result: EmiResult) {
    val principalPercent = if (result.totalPayment > 0) {
        result.totalPrincipal / result.totalPayment * 100
    } else {
        0.0
    }
    val interestPercent = if (result.totalPayment > 0) {
        result.totalInterest / result.totalPayment * 100
    } else {
        0.0
    }
    val tenureLabel = formatEmiTenure(result.tenureMonths)
    val originalTenureLabel = formatEmiTenure(result.originalTenureMonths)
    val tenureReduced = result.tenureMonths < result.originalTenureMonths

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Monthly EMI",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    FormatUtils.formatCompactInr(result.monthlyEmi),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            EmiSummaryRow(
                label = "Principal payable",
                amount = result.totalPrincipal,
                percent = principalPercent
            )
            EmiSummaryRow(
                label = "Interest payable",
                amount = result.totalInterest,
                percent = interestPercent,
                amountColor = LiabilityColor
            )
            if (result.totalPrepayment > 0) {
                EmiSummaryRow(
                    label = "Total prepayment",
                    amount = result.totalPrepayment,
                    amountColor = AssetPositiveColor
                )
                EmiSummaryRow(
                    label = "Interest saved",
                    amount = result.interestSaved,
                    subtitle = "vs no prepayment",
                    amountColor = AssetPositiveColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            EmiSummaryRow(
                label = "Total amount",
                amount = result.totalPayment,
                subtitle = when {
                    result.totalPrepayment > 0 && tenureReduced ->
                        "$tenureLabel (was $originalTenureLabel)"
                    else -> tenureLabel
                },
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

internal fun formatEmiTenure(months: Int): String {
    if (months <= 0) return "0 months"
    val years = months / 12
    val remainingMonths = months % 12
    return when {
        years == 0 -> "$remainingMonths ${if (remainingMonths == 1) "month" else "months"}"
        remainingMonths == 0 -> "$years ${if (years == 1) "year" else "years"}"
        else -> {
            val yearPart = "$years ${if (years == 1) "year" else "years"}"
            val monthPart = "$remainingMonths ${if (remainingMonths == 1) "month" else "months"}"
            "$yearPart $monthPart"
        }
    }
}

@Composable
internal fun EmiSummaryRow(
    label: String,
    amount: Double,
    percent: Double? = null,
    subtitle: String? = null,
    amountColor: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight = FontWeight.Medium
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                FormatUtils.formatCompactInr(amount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = fontWeight,
                color = amountColor
            )
            percent?.let {
                Text(
                    FormatUtils.formatPercent(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
internal fun EmiScheduleResultTable(
    rows: List<EmiYearRow>,
    startYear: Int,
    showPrepayment: Boolean,
    expanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Year-wise repayment schedule",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    val lastRow = rows.last()
                    Text(
                        "Remaining: ${FormatUtils.formatCompactInr(lastRow.remainingPrincipal)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse table" else "Expand table"
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider()
                    EmiScheduleTableHeader(showPrepayment = showPrepayment)
                    rows.forEachIndexed { index, row ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        }
                        EmiScheduleYearRow(
                            row = row,
                            displayYear = startYear + row.year - 1,
                            showPrepayment = showPrepayment
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
internal fun EmiScheduleTableHeader(showPrepayment: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        EmiScheduleTableCell(
            text = "Year",
            modifier = Modifier.weight(0.7f),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start
        )
        EmiScheduleTableCell(
            text = "Principal",
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
        EmiScheduleTableCell(
            text = "Interest",
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
        if (showPrepayment) {
            EmiScheduleTableCell(
                text = "Prepay",
                modifier = Modifier.weight(0.9f),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End
            )
        }
        EmiScheduleTableCell(
            text = "Remaining",
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
    }
}

@Composable
internal fun EmiScheduleYearRow(
    row: EmiYearRow,
    displayYear: Int,
    showPrepayment: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        EmiScheduleTableCell(
            text = displayYear.toString(),
            modifier = Modifier.weight(0.7f),
            textAlign = TextAlign.Start
        )
        EmiScheduleTableCell(
            text = FormatUtils.formatCompactInr(row.principal),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
        EmiScheduleTableCell(
            text = FormatUtils.formatCompactInr(row.interest),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            color = LiabilityColor
        )
        if (showPrepayment) {
            EmiScheduleTableCell(
                text = if (row.prepayment > 0) FormatUtils.formatCompactInr(row.prepayment) else "—",
                modifier = Modifier.weight(0.9f),
                textAlign = TextAlign.End,
                color = if (row.prepayment > 0) AssetPositiveColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
        EmiScheduleTableCell(
            text = FormatUtils.formatCompactInr(row.remainingPrincipal),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
internal fun EmiScheduleTableCell(
    text: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Start,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = fontWeight,
        textAlign = textAlign,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
