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
internal fun BreakdownTabContent(
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
