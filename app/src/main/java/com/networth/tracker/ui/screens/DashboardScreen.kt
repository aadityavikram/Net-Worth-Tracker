package com.networth.tracker.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.networth.tracker.data.AssetAddContext
import com.networth.tracker.data.AssetEntity
import com.networth.tracker.data.BankAccountAddContext
import com.networth.tracker.data.BankAccountEntity
import com.networth.tracker.ui.screens.dashboard.AssetsTabContent
import com.networth.tracker.ui.screens.dashboard.BreakdownTabContent
import com.networth.tracker.ui.screens.dashboard.HomeTabContent
import com.networth.tracker.ui.screens.dashboard.InvestmentCalculatorTabContent
import com.networth.tracker.ui.screens.dashboard.LiabilitiesTabContent
import com.networth.tracker.ui.screens.dashboard.LoanEmiCalculatorTabContent
import com.networth.tracker.ui.theme.LiabilityColor
import com.networth.tracker.viewmodel.DashboardViewModel

private enum class DashboardSection {
    HOME,
    BREAKDOWN,
    ASSETS,
    LIABILITIES,
    INVESTMENT_CALCULATOR,
    LOAN_EMI_CALCULATOR
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    var showMassDeleteConfirm by remember { mutableStateOf(false) }
    var selectedSection by remember { mutableStateOf(DashboardSection.HOME) }
    var menuExpanded by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedAssetIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedBankAccountIds by remember { mutableStateOf(setOf<Long>()) }

    val assetTabAssets = remember(assets) { assets.filter { !it.category.isLiability } }
    val liabilityAssets = remember(assets) { assets.filter { it.category.isLiability } }
    val assetBankAccounts = remember(bankAccounts) { bankAccounts.filter { !it.accountType.isLiability } }
    val liabilityBankAccounts = remember(bankAccounts) { bankAccounts.filter { it.accountType.isLiability } }

    val selectableAssets = when (selectedSection) {
        DashboardSection.ASSETS -> assetTabAssets
        DashboardSection.LIABILITIES -> liabilityAssets
        else -> emptyList()
    }
    val selectableBankAccounts = when (selectedSection) {
        DashboardSection.ASSETS -> assetBankAccounts
        DashboardSection.LIABILITIES -> liabilityBankAccounts
        else -> emptyList()
    }
    val selectedCount = selectedAssetIds.size + selectedBankAccountIds.size
    val allSelectableCount = selectableAssets.size + selectableBankAccounts.size
    val allSelected = allSelectableCount > 0 && selectedCount == allSelectableCount
    val canSelect = selectedSection == DashboardSection.ASSETS ||
        selectedSection == DashboardSection.LIABILITIES

    fun clearSelection() {
        selectionMode = false
        selectedAssetIds = emptySet()
        selectedBankAccountIds = emptySet()
        showMassDeleteConfirm = false
    }

    fun enterSelectionMode(assetId: Long? = null, bankAccountId: Long? = null) {
        selectionMode = true
        selectedAssetIds = if (assetId != null) setOf(assetId) else emptySet()
        selectedBankAccountIds = if (bankAccountId != null) setOf(bankAccountId) else emptySet()
    }

    fun toggleAssetSelection(id: Long) {
        selectedAssetIds = if (id in selectedAssetIds) {
            selectedAssetIds - id
        } else {
            selectedAssetIds + id
        }
    }

    fun toggleBankAccountSelection(id: Long) {
        selectedBankAccountIds = if (id in selectedBankAccountIds) {
            selectedBankAccountIds - id
        } else {
            selectedBankAccountIds + id
        }
    }

    fun selectAllVisible() {
        selectedAssetIds = selectableAssets.map { it.id }.toSet()
        selectedBankAccountIds = selectableBankAccounts.map { it.id }.toSet()
    }

    fun deselectAll() {
        selectedAssetIds = emptySet()
        selectedBankAccountIds = emptySet()
    }

    val screenTitle = when {
        selectionMode -> "$selectedCount selected"
        selectedSection == DashboardSection.HOME -> "Net Worth Tracker"
        selectedSection == DashboardSection.BREAKDOWN -> "Breakdown"
        selectedSection == DashboardSection.ASSETS -> "Assets"
        selectedSection == DashboardSection.LIABILITIES -> "Liabilities"
        selectedSection == DashboardSection.INVESTMENT_CALCULATOR -> "Investment Calculator"
        selectedSection == DashboardSection.LOAN_EMI_CALCULATOR -> "Loan EMI Calculator"
        else -> "Net Worth Tracker"
    }

    Scaffold(
        topBar = {
            DashboardTopBar(
                screenTitle = screenTitle,
                menuExpanded = menuExpanded,
                selectionMode = selectionMode,
                canSelect = canSelect && allSelectableCount > 0,
                selectedCount = selectedCount,
                allSelected = allSelected,
                onMenuClick = { menuExpanded = true },
                onMenuDismiss = { menuExpanded = false },
                onSectionSelected = { section ->
                    clearSelection()
                    selectedSection = section
                    menuExpanded = false
                },
                onEnterSelection = { selectionMode = true },
                onExitSelection = { clearSelection() },
                onToggleSelectAll = {
                    if (allSelected) deselectAll() else selectAllVisible()
                },
                onDeleteSelected = { showMassDeleteConfirm = true }
            )
        },
        floatingActionButton = {
            if (!selectionMode) {
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
                    selectionMode = selectionMode,
                    selectedAssetIds = selectedAssetIds,
                    selectedBankAccountIds = selectedBankAccountIds,
                    onAddAsset = { onAddAsset(AssetAddContext.ASSETS) },
                    onEditAsset = onEditAsset,
                    onAddBankAccount = { onAddBankAccount(BankAccountAddContext.ASSETS) },
                    onEditBankAccount = onEditBankAccount,
                    onDeleteAsset = { assetToDelete = it },
                    onDeleteBankAccount = { bankAccountToDelete = it },
                    onToggleAssetSelection = ::toggleAssetSelection,
                    onToggleBankAccountSelection = ::toggleBankAccountSelection,
                    onEnterSelectionWithAsset = { enterSelectionMode(assetId = it) },
                    onEnterSelectionWithBankAccount = { enterSelectionMode(bankAccountId = it) }
                )
                DashboardSection.LIABILITIES -> LiabilitiesTabContent(
                    assets = liabilityAssets,
                    bankAccounts = liabilityBankAccounts,
                    exchangeRateState = exchangeRateState,
                    selectionMode = selectionMode,
                    selectedAssetIds = selectedAssetIds,
                    selectedBankAccountIds = selectedBankAccountIds,
                    onAddAsset = { onAddAsset(AssetAddContext.LIABILITIES) },
                    onEditAsset = onEditAsset,
                    onAddBankAccount = { onAddBankAccount(BankAccountAddContext.LIABILITIES) },
                    onEditBankAccount = onEditBankAccount,
                    onDeleteAsset = { assetToDelete = it },
                    onDeleteBankAccount = { bankAccountToDelete = it },
                    onToggleAssetSelection = ::toggleAssetSelection,
                    onToggleBankAccountSelection = ::toggleBankAccountSelection,
                    onEnterSelectionWithAsset = { enterSelectionMode(assetId = it) },
                    onEnterSelectionWithBankAccount = { enterSelectionMode(bankAccountId = it) }
                )
                DashboardSection.INVESTMENT_CALCULATOR -> InvestmentCalculatorTabContent()
                DashboardSection.LOAN_EMI_CALCULATOR -> LoanEmiCalculatorTabContent()
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

    if (showMassDeleteConfirm) {
        val entryLabel = if (selectedCount == 1) "entry" else "entries"
        AlertDialog(
            onDismissRequest = { showMassDeleteConfirm = false },
            title = { Text("Delete $selectedCount $entryLabel?") },
            text = {
                Text("Remove the selected $entryLabel from your portfolio? This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    val assetsToRemove = selectableAssets.filter { it.id in selectedAssetIds }
                    val accountsToRemove = selectableBankAccounts.filter { it.id in selectedBankAccountIds }
                    viewModel.deleteAssets(assetsToRemove)
                    viewModel.deleteBankAccounts(accountsToRemove)
                    clearSelection()
                }) {
                    Text("Delete", color = LiabilityColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMassDeleteConfirm = false }) {
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
    selectionMode: Boolean,
    canSelect: Boolean,
    selectedCount: Int,
    allSelected: Boolean,
    onMenuClick: () -> Unit,
    onMenuDismiss: () -> Unit,
    onSectionSelected: (DashboardSection) -> Unit,
    onEnterSelection: () -> Unit,
    onExitSelection: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectionMode) {
                    IconButton(onClick = onExitSelection) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel selection",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text(
                        screenTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    )
                    IconButton(onClick = onToggleSelectAll) {
                        Icon(
                            if (allSelected) Icons.Default.CheckBox else Icons.Default.SelectAll,
                            contentDescription = if (allSelected) "Deselect all" else "Select all",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(
                        onClick = onDeleteSelected,
                        enabled = selectedCount > 0
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete selected",
                            tint = if (selectedCount > 0) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f)
                            }
                        )
                    }
                } else {
                    Box(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier
                                .clickable(onClick = onMenuClick)
                                .padding(horizontal = 8.dp, vertical = 8.dp),
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
                            DropdownMenuItem(
                                text = { Text("Loan EMI Calculator") },
                                onClick = { onSectionSelected(DashboardSection.LOAN_EMI_CALCULATOR) }
                            )
                        }
                    }
                    if (canSelect) {
                        TextButton(onClick = onEnterSelection) {
                            Text(
                                "Select",
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
