package com.networth.tracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.networth.tracker.data.InvestmentMode
import com.networth.tracker.data.InvestmentProjectionCalculator
import com.networth.tracker.data.InvestmentYearRow
import com.networth.tracker.data.ReturnCalculator
import com.networth.tracker.data.ReturnMetrics
import com.networth.tracker.ui.theme.AssetPositiveColor
import com.networth.tracker.ui.theme.LiabilityColor
import com.networth.tracker.util.FormatUtils
import com.networth.tracker.viewmodel.DashboardViewModel

private enum class DashboardSection {
    HOME,
    BREAKDOWN,
    ASSETS,
    LIABILITIES,
    INVESTMENT_CALCULATOR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAddAsset: (AssetAddContext) -> Unit,
    onEditAsset: (Long) -> Unit,
    onAddBankAccount: (BankAccountAddContext) -> Unit,
    onEditBankAccount: (Long) -> Unit
) {
    val assets by viewModel.assets.collectAsStateWithLifecycle()
    val bankAccounts by viewModel.bankAccounts.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val exchangeRateState by viewModel.exchangeRateState.collectAsStateWithLifecycle()
    var assetToDelete by remember { mutableStateOf<AssetEntity?>(null) }
    var bankAccountToDelete by remember { mutableStateOf<BankAccountEntity?>(null) }
    var selectedSection by remember { mutableStateOf(DashboardSection.HOME) }
    var menuExpanded by remember { mutableStateOf(false) }

    val assetTabAssets = remember(assets) { assets.filter { !it.category.isLiability } }
    val liabilityAssets = remember(assets) { assets.filter { it.category.isLiability } }
    val assetBankAccounts = remember(bankAccounts) { bankAccounts.filter { !it.accountType.isLiability } }
    val liabilityBankAccounts = remember(bankAccounts) { bankAccounts.filter { it.accountType.isLiability } }

    val screenTitle = when (selectedSection) {
        DashboardSection.HOME -> "Net Worth Tracker"
        DashboardSection.BREAKDOWN -> "Breakdown"
        DashboardSection.ASSETS -> "Assets"
        DashboardSection.LIABILITIES -> "Liabilities"
        DashboardSection.INVESTMENT_CALCULATOR -> "Investment Calculator"
    }

    Scaffold(
        topBar = {
            DashboardTopBar(
                screenTitle = screenTitle,
                menuExpanded = menuExpanded,
                onMenuClick = { menuExpanded = true },
                onMenuDismiss = { menuExpanded = false },
                onSectionSelected = { section ->
                    selectedSection = section
                    menuExpanded = false
                }
            )
        },
        floatingActionButton = {
            when (selectedSection) {
                DashboardSection.ASSETS -> {
                    FloatingActionButton(
                        onClick = { onAddAsset(AssetAddContext.ASSETS) },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add asset")
                    }
                }
                DashboardSection.LIABILITIES -> {
                    FloatingActionButton(
                        onClick = { onAddAsset(AssetAddContext.LIABILITIES) },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add liability")
                    }
                }
                else -> Unit
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedSection) {
            DashboardSection.HOME -> HomeTabContent(
                summary = summary,
                exchangeRateState = exchangeRateState,
                onRefreshExchangeRate = viewModel::refreshExchangeRate
            )
            DashboardSection.BREAKDOWN -> BreakdownTabContent(
                summary = summary,
                assets = assets,
                bankAccounts = bankAccounts,
                exchangeRateState = exchangeRateState,
                onEditAsset = onEditAsset,
                onEditBankAccount = onEditBankAccount,
                onDeleteAsset = { assetToDelete = it },
                onDeleteBankAccount = { bankAccountToDelete = it }
            )
            DashboardSection.ASSETS -> AssetsTabContent(
                assets = assetTabAssets,
                bankAccounts = assetBankAccounts,
                exchangeRateState = exchangeRateState,
                onAddAsset = { onAddAsset(AssetAddContext.ASSETS) },
                onEditAsset = onEditAsset,
                onAddBankAccount = { onAddBankAccount(BankAccountAddContext.ASSETS) },
                onEditBankAccount = onEditBankAccount,
                onDeleteAsset = { assetToDelete = it },
                onDeleteBankAccount = { bankAccountToDelete = it }
            )
            DashboardSection.LIABILITIES -> LiabilitiesTabContent(
                assets = liabilityAssets,
                bankAccounts = liabilityBankAccounts,
                exchangeRateState = exchangeRateState,
                onAddAsset = { onAddAsset(AssetAddContext.LIABILITIES) },
                onEditAsset = onEditAsset,
                onAddBankAccount = { onAddBankAccount(BankAccountAddContext.LIABILITIES) },
                onEditBankAccount = onEditBankAccount,
                onDeleteAsset = { assetToDelete = it },
                onDeleteBankAccount = { bankAccountToDelete = it }
            )
            DashboardSection.INVESTMENT_CALCULATOR -> InvestmentCalculatorTabContent()
            }
        }
    }

    assetToDelete?.let { asset ->
        AlertDialog(
            onDismissRequest = { assetToDelete = null },
            title = { Text("Delete entry?") },
            text = { Text("Remove \"${asset.name}\" from your portfolio?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAsset(asset)
                    assetToDelete = null
                }) {
                    Text("Delete", color = LiabilityColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { assetToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    bankAccountToDelete?.let { account ->
        AlertDialog(
            onDismissRequest = { bankAccountToDelete = null },
            title = { Text("Delete bank account?") },
            text = { Text("Remove \"${account.accountName}\" at ${account.bankName}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBankAccount(account)
                    bankAccountToDelete = null
                }) {
                    Text("Delete", color = LiabilityColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { bankAccountToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DashboardTopBar(
    screenTitle: String,
    menuExpanded: Boolean,
    onMenuClick: () -> Unit,
    onMenuDismiss: () -> Unit,
    onSectionSelected: (DashboardSection) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Box {
                    Row(
                        modifier = Modifier.clickable(onClick = onMenuClick),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Open sections menu",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            screenTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = onMenuDismiss
                    ) {
                        DropdownMenuItem(
                            text = { Text("Home") },
                            onClick = { onSectionSelected(DashboardSection.HOME) }
                        )
                        DropdownMenuItem(
                            text = { Text("Breakdown") },
                            onClick = { onSectionSelected(DashboardSection.BREAKDOWN) }
                        )
                        DropdownMenuItem(
                            text = { Text("Assets") },
                            onClick = { onSectionSelected(DashboardSection.ASSETS) }
                        )
                        DropdownMenuItem(
                            text = { Text("Liabilities") },
                            onClick = { onSectionSelected(DashboardSection.LIABILITIES) }
                        )
                        DropdownMenuItem(
                            text = { Text("Investment Calculator") },
                            onClick = { onSectionSelected(DashboardSection.INVESTMENT_CALCULATOR) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTabContent(
    summary: NetWorthSummary,
    exchangeRateState: ExchangeRateState,
    onRefreshExchangeRate: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { NetWorthHeader(summary) }
        item { AssetsLiabilitiesRow(summary) }
        item {
            ExchangeRateCard(
                state = exchangeRateState,
                onRefresh = onRefreshExchangeRate
            )
        }
        if (summary.portfolioReturn.hasReturnData) {
            item { PortfolioReturnCard(summary.portfolioReturn) }
        }
    }
}

@Composable
private fun InvestmentCalculatorTabContent() {
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
private fun InvestmentProjectionResultTable(
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
private fun InvestmentProjectionTableHeader() {
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
private fun InvestmentProjectionTableRow(row: InvestmentYearRow) {
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
private fun InvestmentProjectionTableCell(
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

@Composable
private fun BreakdownTabContent(
    summary: NetWorthSummary,
    assets: List<AssetEntity>,
    bankAccounts: List<BankAccountEntity>,
    exchangeRateState: ExchangeRateState,
    onEditAsset: (Long) -> Unit,
    onEditBankAccount: (Long) -> Unit,
    onDeleteAsset: (AssetEntity) -> Unit,
    onDeleteBankAccount: (BankAccountEntity) -> Unit
) {
    val categoryEntries = summary.categorySummaries.filter { it.entryCount > 0 }
    val outstandingEntries = summary.outstandingSummaries.filter { it.entryCount > 0 }
    val hasBreakdown = categoryEntries.isNotEmpty() || outstandingEntries.isNotEmpty()
    var selectedAssetCategory by remember { mutableStateOf<AssetCategory?>(null) }
    var selectedBankAccountType by remember { mutableStateOf<BankAccountType?>(null) }

    val filteredAssets = remember(assets, selectedAssetCategory) {
        selectedAssetCategory?.let { category ->
            assets.filter { it.category == category }
        } ?: emptyList()
    }
    val filteredBankAccounts = remember(bankAccounts, selectedBankAccountType) {
        selectedBankAccountType?.let { type ->
            bankAccounts.filter { it.accountType == type }
        } ?: emptyList()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            selectedBankAccountType != null -> {
                val accountType = selectedBankAccountType!!
                item {
                    GroupBackHeader(
                        title = accountType.displayName,
                        entryCount = filteredBankAccounts.size,
                        onBack = { selectedBankAccountType = null }
                    )
                }
                items(filteredBankAccounts, key = { "breakdown-bank-${it.id}" }) { account ->
                    BankAccountListItem(
                        account = account,
                        onClick = { onEditBankAccount(account.id) },
                        onDelete = { onDeleteBankAccount(account) }
                    )
                }
            }
            selectedAssetCategory != null -> {
                val category = selectedAssetCategory!!
                item {
                    GroupBackHeader(
                        title = category.displayName,
                        entryCount = filteredAssets.size,
                        onBack = { selectedAssetCategory = null }
                    )
                }
                items(filteredAssets, key = { "breakdown-asset-${it.id}" }) { asset ->
                    AssetListItem(
                        asset = asset,
                        usdToInrRate = exchangeRateState.rate,
                        onClick = { onEditAsset(asset.id) },
                        onDelete = { onDeleteAsset(asset) }
                    )
                }
            }
            hasBreakdown -> {
                items(
                    categoryEntries,
                    key = { "category-${it.category.name}" }
                ) { categorySummary ->
                    CategoryBreakdownCard(
                        summary = categorySummary,
                        totalAssets = summary.totalAssetsInInr,
                        onClick = { selectedAssetCategory = categorySummary.category }
                    )
                }

                items(
                    outstandingEntries,
                    key = { "outstanding-${it.accountType.name}" }
                ) { outstandingSummary ->
                    OutstandingBreakdownCard(
                        summary = outstandingSummary,
                        onClick = { selectedBankAccountType = outstandingSummary.accountType }
                    )
                }
            }
            else -> {
                item {
                    EmptyStateCard(
                        onAdd = null,
                        title = "No breakdown yet",
                        description = "Add assets or liabilities from the Assets or Liabilities sections"
                    )
                }
            }
        }
    }
}

@Composable
private fun AssetsTabContent(
    assets: List<AssetEntity>,
    bankAccounts: List<BankAccountEntity>,
    exchangeRateState: ExchangeRateState,
    onAddAsset: () -> Unit,
    onEditAsset: (Long) -> Unit,
    onAddBankAccount: () -> Unit,
    onEditBankAccount: (Long) -> Unit,
    onDeleteAsset: (AssetEntity) -> Unit,
    onDeleteBankAccount: (BankAccountEntity) -> Unit
) {
    val usdToInrRate = exchangeRateState.rate
    val totalAssetsInInr = remember(assets, usdToInrRate) {
        assets.sumOf { AssetRepository.toInr(it.amount, it.currency, usdToInrRate) }
    }
    val totalBankAccountsInInr = remember(bankAccounts, usdToInrRate) {
        bankAccounts.sumOf { AssetRepository.toInr(it.balance, it.currency, usdToInrRate) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TabTotalsRow(
                leftLabel = "Total Assets",
                leftAmount = totalAssetsInInr,
                leftColor = AssetPositiveColor,
                rightLabel = "Total Bank Accounts",
                rightAmount = totalBankAccountsInInr,
                rightColor = AssetPositiveColor
            )
        }
        item {
            SectionHeader(
                title = "Bank Accounts",
                onAdd = onAddBankAccount,
                addLabel = "Add"
            )
        }
        if (bankAccounts.isEmpty()) {
            item {
                EmptyBankAccountsCard(
                    onAdd = onAddBankAccount,
                    title = "No bank accounts yet",
                    description = "Tap Add to track savings, current, or salary account balances"
                )
            }
        } else {
            items(bankAccounts, key = { "bank-${it.id}" }) { account ->
                BankAccountListItem(
                    account = account,
                    onClick = { onEditBankAccount(account.id) },
                    onDelete = { onDeleteBankAccount(account) }
                )
            }
        }

        item {
            Text(
                "Investments",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (assets.isEmpty()) {
            item {
                EmptyStateCard(
                    onAdd = onAddAsset,
                    title = "No investments yet",
                    description = "Tap + to add stocks, mutual funds, gold, real estate, and more"
                )
            }
        } else {
            items(assets, key = { "asset-${it.id}" }) { asset ->
                AssetListItem(
                    asset = asset,
                    usdToInrRate = exchangeRateState.rate,
                    onClick = { onEditAsset(asset.id) },
                    onDelete = { onDeleteAsset(asset) }
                )
            }
        }
    }
}

@Composable
private fun LiabilitiesTabContent(
    assets: List<AssetEntity>,
    bankAccounts: List<BankAccountEntity>,
    exchangeRateState: ExchangeRateState,
    onAddAsset: () -> Unit,
    onEditAsset: (Long) -> Unit,
    onAddBankAccount: () -> Unit,
    onEditBankAccount: (Long) -> Unit,
    onDeleteAsset: (AssetEntity) -> Unit,
    onDeleteBankAccount: (BankAccountEntity) -> Unit
) {
    val usdToInrRate = exchangeRateState.rate
    val totalLiabilitiesInInr = remember(assets, usdToInrRate) {
        assets.sumOf { AssetRepository.toInr(it.amount, it.currency, usdToInrRate) }
    }
    val totalOutstandingInInr = remember(bankAccounts, usdToInrRate) {
        bankAccounts.sumOf { AssetRepository.toInr(it.balance, it.currency, usdToInrRate) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TabTotalsRow(
                leftLabel = "Total Liabilities",
                leftAmount = totalLiabilitiesInInr,
                leftColor = LiabilityColor,
                rightLabel = "Total Outstanding",
                rightAmount = totalOutstandingInInr,
                rightColor = LiabilityColor,
                amountColor = LiabilityColor
            )
        }
        item {
            SectionHeader(
                title = "Outstanding Credit",
                onAdd = onAddBankAccount,
                addLabel = "Add"
            )
        }
        if (bankAccounts.isEmpty()) {
            item {
                EmptyBankAccountsCard(
                    onAdd = onAddBankAccount,
                    title = "No outstanding credit yet",
                    description = "Tap Add to track credit card or overdraft balance"
                )
            }
        } else {
            items(bankAccounts, key = { "bank-${it.id}" }) { account ->
                BankAccountListItem(
                    account = account,
                    onClick = { onEditBankAccount(account.id) },
                    onDelete = { onDeleteBankAccount(account) }
                )
            }
        }

        item {
            Text(
                "Loans & Other Liabilities",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (assets.isEmpty()) {
            item {
                EmptyStateCard(
                    onAdd = onAddAsset,
                    title = "No liabilities yet",
                    description = "Tap + to add home loans and other outstanding balances"
                )
            }
        } else {
            items(assets, key = { "asset-${it.id}" }) { asset ->
                AssetListItem(
                    asset = asset,
                    usdToInrRate = exchangeRateState.rate,
                    onClick = { onEditAsset(asset.id) },
                    onDelete = { onDeleteAsset(asset) }
                )
            }
        }
    }
}

@Composable
private fun GroupBackHeader(
    title: String,
    entryCount: Int,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBack)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "$entryCount ${if (entryCount == 1) "entry" else "entries"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onAdd: () -> Unit,
    addLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        TextButton(onClick = onAdd) {
            Text(addLabel)
        }
    }
}

@Composable
private fun NetWorthHeader(summary: NetWorthSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Total Net Worth",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                FormatUtils.formatCompactInr(summary.netWorthInInr),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (summary.netWorthInInr >= 0) AssetPositiveColor else LiabilityColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                FormatUtils.formatInr(summary.netWorthInInr),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun TabTotalsRow(
    leftLabel: String,
    leftAmount: Double,
    leftColor: Color,
    rightLabel: String,
    rightAmount: Double,
    rightColor: Color,
    amountColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryChip(
            modifier = Modifier.weight(1f),
            label = leftLabel,
            amount = leftAmount,
            color = leftColor,
            amountColor = amountColor
        )
        SummaryChip(
            modifier = Modifier.weight(1f),
            label = rightLabel,
            amount = rightAmount,
            color = rightColor,
            amountColor = amountColor
        )
    }
}

@Composable
private fun AssetsLiabilitiesRow(summary: NetWorthSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryChip(
            modifier = Modifier.weight(1f),
            label = "Assets",
            amount = summary.totalAssetsInInr,
            color = AssetPositiveColor
        )
        SummaryChip(
            modifier = Modifier.weight(1f),
            label = "Liabilities",
            amount = summary.totalLiabilitiesInInr,
            color = LiabilityColor
        )
    }
}

@Composable
private fun SummaryChip(
    modifier: Modifier = Modifier,
    label: String,
    amount: Double,
    color: Color,
    amountColor: Color? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                FormatUtils.formatCompactInr(amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = amountColor ?: MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CategoryBreakdownCard(
    summary: CategorySummary,
    totalAssets: Double,
    onClick: () -> Unit
) {
    val fraction = if (summary.category.isLiability || totalAssets <= 0) {
        0f
    } else {
        (summary.totalInInr / totalAssets).toFloat().coerceIn(0f, 1f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIcon(summary.category)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    summary.category.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${summary.entryCount} ${if (summary.entryCount == 1) "entry" else "entries"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (!summary.category.isLiability && fraction > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }
            Text(
                FormatUtils.formatCompactInr(summary.totalInInr),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (summary.category.isLiability) LiabilityColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun OutstandingBreakdownCard(
    summary: BankAccountTypeSummary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BankAccountTypeIcon(summary.accountType)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    summary.accountType.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${summary.entryCount} ${if (summary.entryCount == 1) "entry" else "entries"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Text(
                FormatUtils.formatCompactInr(summary.totalInInr),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = LiabilityColor
            )
        }
    }
}

@Composable
private fun PortfolioReturnCard(metrics: ReturnMetrics) {
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
            Text(
                "Total Assets Return",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            ReturnMetricsGrid(metrics)
        }
    }
}

@Composable
private fun ReturnMetricsGrid(metrics: ReturnMetrics) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReturnMetricCell(
                modifier = Modifier.weight(1f),
                label = "Invested",
                value = FormatUtils.formatCompactInr(metrics.investedInInr)
            )
            ReturnMetricCell(
                modifier = Modifier.weight(1f),
                label = "Current",
                value = FormatUtils.formatCompactInr(metrics.currentInInr)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReturnMetricCell(
                modifier = Modifier.weight(1f),
                label = "Return",
                value = FormatUtils.formatSignedInr(metrics.returnInInr),
                valueColor = returnColor(metrics.returnInInr, MaterialTheme.colorScheme.onSurface)
            )
            ReturnMetricCell(
                modifier = Modifier.weight(1f),
                label = "Return %",
                value = FormatUtils.formatReturnPercent(metrics.returnPercent),
                valueColor = returnColor(metrics.returnInInr, MaterialTheme.colorScheme.onSurface)
            )
        }
    }
}

@Composable
private fun ReturnMetricCell(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

private fun returnColor(returnInInr: Double, neutral: Color): Color = when {
    returnInInr > 0 -> AssetPositiveColor
    returnInInr < 0 -> LiabilityColor
    else -> neutral
}

@Composable
private fun ExchangeRateCard(
    state: ExchangeRateState,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "USD → INR",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    "1 USD = ${FormatUtils.formatExchangeRate(state.rate)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                val statusText = when {
                    state.isLoading -> "Updating rate…"
                    state.error != null -> "Offline — using cached rate"
                    state.lastUpdated != null -> "Updated ${FormatUtils.formatRelativeTime(state.lastUpdated)}"
                    else -> "Tap refresh to fetch live rate"
                }
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.error != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    }
                )
            }
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh exchange rate")
                }
            }
        }
    }
}

@Composable
private fun BankAccountListItem(
    account: BankAccountEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val amountColor = if (account.accountType.isLiability) LiabilityColor else MaterialTheme.colorScheme.onSurface
    val maskedNumber = FormatUtils.maskAccountNumber(account.accountNumber)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BankAccountTypeIcon(account.accountType, size = 36)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        account.accountName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        account.bankName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        buildString {
                            append(account.accountType.displayName)
                            if (maskedNumber.isNotBlank()) {
                                append(" · ")
                                append(maskedNumber)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        FormatUtils.formatCurrency(account.balance, account.currency),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = amountColor
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
            if (account.accountType == BankAccountType.CREDIT_CARD) {
                Spacer(modifier = Modifier.height(8.dp))
                if (account.creditLimit > 0) {
                    LoanDetailRow(
                        label = "Credit Limit",
                        value = FormatUtils.formatCurrency(account.creditLimit, account.currency)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                account.creditUtilisationPercent()?.let { utilisation ->
                    LoanDetailRow(
                        label = "Utilisation",
                        value = FormatUtils.formatPercent(utilisation),
                        valueColor = if (utilisation >= 70) LiabilityColor else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            EntryNoteText(account.notes)
        }
    }
}

@Composable
private fun BankAccountTypeIcon(type: BankAccountType, size: Int = 40) {
    val icon = when (type) {
        BankAccountType.SAVINGS -> Icons.Default.Savings
        BankAccountType.CURRENT -> Icons.Default.AccountBalance
        BankAccountType.SALARY -> Icons.Default.Work
        BankAccountType.CREDIT_CARD -> Icons.Default.CreditCard
        BankAccountType.OVERDRAFT -> Icons.Default.AccountBalance
    }
    val bgColor = if (type.isLiability) {
        LiabilityColor.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    }
    val tint = if (type.isLiability) LiabilityColor else MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = type.displayName,
            tint = tint,
            modifier = Modifier.size((size * 0.55f).dp)
        )
    }
}

@Composable
private fun AssetListItem(
    asset: AssetEntity,
    usdToInrRate: Double,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val returnMetrics = ReturnCalculator.forAsset(asset, usdToInrRate)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CategoryIcon(asset.category, size = 36)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(asset.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                    Text(
                        asset.category.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            if (returnMetrics != null) {
                Spacer(modifier = Modifier.height(10.dp))
                ReturnMetricsGrid(returnMetrics)
            } else if (asset.category.isLoan) {
                Spacer(modifier = Modifier.height(8.dp))
                LoanDetailsGrid(asset)
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Outstanding",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        FormatUtils.formatCurrency(asset.amount, asset.currency),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = LiabilityColor
                    )
                }
            }
            EntryNoteText(asset.notes)
        }
    }
}

@Composable
private fun LoanDetailsGrid(asset: AssetEntity) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (asset.investedAmount > 0) {
            LoanDetailRow(
                label = "Original Amount",
                value = FormatUtils.formatCurrency(asset.investedAmount, asset.currency)
            )
        }
        LoanDetailRow(
            label = "Outstanding",
            value = FormatUtils.formatCurrency(asset.amount, asset.currency),
            valueColor = LiabilityColor
        )
        if (asset.interestRate > 0) {
            LoanDetailRow(
                label = "Rate of Interest",
                value = FormatUtils.formatPercent(asset.interestRate)
            )
        }
        if (asset.dateTakenMillis > 0) {
            LoanDetailRow(
                label = "Date Taken",
                value = FormatUtils.formatDate(asset.dateTakenMillis)
            )
        }
    }
}

@Composable
private fun LoanDetailRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun EntryNoteText(notes: String) {
    if (notes.isNotBlank()) {
        Text(
            notes,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun EmptyBankAccountsCard(
    onAdd: () -> Unit,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAdd),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun EmptyStateCard(
    onAdd: (() -> Unit)?,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onAdd != null) Modifier.clickable(onClick = onAdd) else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun CategoryIcon(category: AssetCategory, size: Int = 40) {
    val icon = categoryIcon(category)
    val bgColor = if (category.isLiability) {
        LiabilityColor.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    }
    val tint = if (category.isLiability) LiabilityColor else MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category.displayName,
            tint = tint,
            modifier = Modifier.size((size * 0.55f).dp)
        )
    }
}

private fun categoryIcon(category: AssetCategory): ImageVector = when (category) {
    AssetCategory.US_STOCKS -> Icons.AutoMirrored.Filled.ShowChart
    AssetCategory.MUTUAL_FUNDS -> Icons.Default.PieChart
    AssetCategory.INDIAN_STOCKS -> Icons.AutoMirrored.Filled.TrendingUp
    AssetCategory.EPF -> Icons.Default.AccountBalance
    AssetCategory.NPS -> Icons.Default.Savings
    AssetCategory.GOLD -> Icons.Default.Diamond
    AssetCategory.REAL_ESTATE -> Icons.Default.Home
    AssetCategory.HOME_LOAN -> Icons.Default.Home
    AssetCategory.VEHICLE_LOAN -> Icons.Default.DirectionsCar
    AssetCategory.PERSONAL_LOAN -> Icons.Default.Person
}
