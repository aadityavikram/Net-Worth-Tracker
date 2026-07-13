package com.networth.tracker.ui.screens.dashboard


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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
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
import com.networth.tracker.data.CategorySummary
import com.networth.tracker.data.ExchangeRateState
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

@Composable
internal fun InvestmentCalculatorTabContent() {
    var mode by remember { mutableStateOf(InvestmentMode.SIP) }
    var amountText by remember { mutableStateOf("") }
    var annualReturnText by remember { mutableStateOf("") }
    var yearsText by remember { mutableStateOf("") }
    var stepUpText by remember { mutableStateOf("0") }
    var resultRows by remember { mutableStateOf<List<InvestmentYearRow>?>(null) }
    var tableExpanded by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                        "Project returns from SIP or lumpsum investing",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Text("Investment type", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = mode == InvestmentMode.SIP,
                            onClick = { mode = InvestmentMode.SIP },
                            label = { Text("SIP") }
                        )
                        FilterChip(
                            selected = mode == InvestmentMode.LUMPSUM,
                            onClick = { mode = InvestmentMode.LUMPSUM },
                            label = { Text("Lumpsum") }
                        )
                    }

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = {
                            Text(
                                if (mode == InvestmentMode.SIP) "Monthly SIP amount" else "Lumpsum amount"
                            )
                        },
                        placeholder = { Text("e.g. 10000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = annualReturnText,
                        onValueChange = { annualReturnText = it },
                        label = { Text("Annual return (%)") },
                        placeholder = { Text("e.g. 12") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = yearsText,
                        onValueChange = { yearsText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Number of years") },
                        placeholder = { Text("e.g. 10") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = stepUpText,
                        onValueChange = { stepUpText = it },
                        label = { Text("Annual step-up (%)") },
                        placeholder = { Text("e.g. 10") },
                        enabled = mode == InvestmentMode.SIP,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = {
                            if (mode == InvestmentMode.LUMPSUM) {
                                Text("Step-up applies to SIP only")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    errorMessage?.let { message ->
                        Text(
                            message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull()
                            val annualReturn = annualReturnText.toDoubleOrNull()
                            val years = yearsText.toIntOrNull()
                            val stepUp = stepUpText.toDoubleOrNull() ?: 0.0

                            errorMessage = when {
                                amount == null || amount <= 0 -> "Enter a valid amount"
                                annualReturn == null -> "Enter a valid annual return"
                                years == null || years <= 0 -> "Enter a valid number of years"
                                mode == InvestmentMode.SIP && stepUpText.isNotBlank() && stepUpText.toDoubleOrNull() == null ->
                                    "Enter a valid step-up percentage"
                                else -> null
                            }

                            if (errorMessage == null) {
                                resultRows = InvestmentProjectionCalculator.calculate(
                                    mode = mode,
                                    amount = amount!!,
                                    annualReturnPercent = annualReturn!!,
                                    years = years!!,
                                    stepUpPercent = if (mode == InvestmentMode.SIP) stepUp else 0.0
                                )
                                tableExpanded = true
                            } else {
                                resultRows = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Calculate")
                    }
                }
            }
        }

        resultRows?.let { rows ->
            if (rows.isNotEmpty()) {
                item {
                    InvestmentProjectionResultTable(
                        rows = rows,
                        expanded = tableExpanded,
                        onToggleExpanded = { tableExpanded = !tableExpanded }
                    )
                }
            }
        }
    }
}

@Composable
internal fun InvestmentProjectionResultTable(
    rows: List<InvestmentYearRow>,
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
                        "Year-wise breakdown",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    val lastRow = rows.last()
                    Text(
                        "Final: ${FormatUtils.formatCompactInr(lastRow.final)}",
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
                    InvestmentProjectionTableHeader()
                    rows.forEachIndexed { index, row ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        }
                        InvestmentProjectionTableRow(row)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
internal fun InvestmentProjectionTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        InvestmentProjectionTableCell(
            text = "Year",
            modifier = Modifier.weight(0.7f),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Start
        )
        InvestmentProjectionTableCell(
            text = "Invested",
            modifier = Modifier.weight(1.2f),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
        InvestmentProjectionTableCell(
            text = "Returns",
            modifier = Modifier.weight(1.2f),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
        InvestmentProjectionTableCell(
            text = "Final",
            modifier = Modifier.weight(1.2f),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
    }
}

@Composable
internal fun InvestmentProjectionTableRow(row: InvestmentYearRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        InvestmentProjectionTableCell(
            text = row.year.toString(),
            modifier = Modifier.weight(0.7f),
            textAlign = TextAlign.Start
        )
        InvestmentProjectionTableCell(
            text = FormatUtils.formatCompactInr(row.invested),
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.End
        )
        InvestmentProjectionTableCell(
            text = FormatUtils.formatSignedInr(row.yearReturn),
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.End,
            color = returnColor(row.yearReturn, MaterialTheme.colorScheme.onSurface)
        )
        InvestmentProjectionTableCell(
            text = FormatUtils.formatCompactInr(row.final),
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
internal fun InvestmentProjectionTableCell(
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
