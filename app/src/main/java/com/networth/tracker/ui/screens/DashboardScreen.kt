package com.networth.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import com.networth.tracker.data.AssetCategory
import com.networth.tracker.data.AssetEntity
import com.networth.tracker.data.CategorySummary
import com.networth.tracker.data.ExchangeRateState
import com.networth.tracker.data.NetWorthSummary
import com.networth.tracker.data.ReturnCalculator
import com.networth.tracker.data.ReturnMetrics
import com.networth.tracker.ui.theme.AssetPositiveColor
import com.networth.tracker.ui.theme.LiabilityColor
import com.networth.tracker.util.FormatUtils
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.networth.tracker.data.BackupInfo
import com.networth.tracker.data.PortfolioBackupStore
import com.networth.tracker.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    backupStore: PortfolioBackupStore,
    onAddAsset: () -> Unit,
    onEditAsset: (Long) -> Unit
) {
    val assets by viewModel.assets.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val exchangeRateState by viewModel.exchangeRateState.collectAsStateWithLifecycle()
    val backupInfo by viewModel.backupInfo.collectAsStateWithLifecycle()
    val backupMessage by viewModel.backupMessage.collectAsStateWithLifecycle()
    val needsFolderAccess by viewModel.needsFolderAccess.collectAsStateWithLifecycle()
    val isBackupBusy by viewModel.isBackupBusy.collectAsStateWithLifecycle()
    var assetToDelete by remember { mutableStateOf<AssetEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showFolderDialog by remember { mutableStateOf(false) }

    val backupFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.configureBackupFolder(it) }
    }

    LaunchedEffect(backupMessage) {
        backupMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearBackupMessage()
        }
    }

    LaunchedEffect(needsFolderAccess) {
        if (needsFolderAccess) showFolderDialog = true
    }

    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("Select Documents folder") },
            text = {
                Text(
                    "To save JSON backups that survive uninstall, select the Documents folder once. Backups will be stored in Documents/NetWorthTracker/."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showFolderDialog = false
                    backupFolderLauncher.launch(backupStore.suggestedBackupTreeInitialUri())
                }) {
                    Text("Select Documents")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Net Worth Tracker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAsset,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add entry")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                NetWorthHeader(summary)
            }

            item {
                AssetsLiabilitiesRow(summary)
            }

            item {
                ExchangeRateCard(
                    state = exchangeRateState,
                    onRefresh = viewModel::refreshExchangeRate
                )
            }

            if (summary.portfolioReturn.hasReturnData) {
                item {
                    PortfolioReturnCard(summary.portfolioReturn)
                }
            }

            item {
                BackupRestoreCard(
                    backupInfo = backupInfo,
                    isBusy = isBackupBusy,
                    onBackup = viewModel::createBackup,
                    onRestore = viewModel::restoreLatestBackup,
                    onSelectFolder = {
                        backupFolderLauncher.launch(backupStore.suggestedBackupTreeInitialUri())
                    }
                )
            }

            if (summary.categorySummaries.any { it.entryCount > 0 }) {
                item {
                    Text(
                        "Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(summary.categorySummaries.filter { it.entryCount > 0 }) { categorySummary ->
                    CategoryBreakdownCard(categorySummary, summary.totalAssetsInInr)
                }
            }

            item {
                Text(
                    "All Entries",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (assets.isEmpty()) {
                item {
                    EmptyStateCard(onAddAsset)
                }
            } else {
                items(assets, key = { it.id }) { asset ->
                    AssetListItem(
                        asset = asset,
                        usdToInrRate = exchangeRateState.rate,
                        onClick = { onEditAsset(asset.id) },
                        onDelete = { assetToDelete = asset }
                    )
                }
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
    color: Color
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
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CategoryBreakdownCard(summary: CategorySummary, totalAssets: Double) {
    val fraction = if (summary.category.isLiability || totalAssets <= 0) {
        0f
    } else {
        (summary.totalInInr / totalAssets).toFloat().coerceIn(0f, 1f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                if (summary.category.isLiability) {
                    "-${FormatUtils.formatCompactInr(summary.totalInInr)}"
                } else {
                    FormatUtils.formatCompactInr(summary.totalInInr)
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (summary.category.isLiability) LiabilityColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BackupRestoreCard(
    backupInfo: BackupInfo,
    isBusy: Boolean,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onSelectFolder: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        "JSON Backup",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        backupInfo.folderPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            if (backupInfo.latestFileName != null) {
                Text(
                    "Latest: ${backupInfo.latestFileName} (${backupInfo.backupCount} total)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            } else {
                Text(
                    "No backups yet. Tap Backup to create a timestamped JSON file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }

            Text(
                "JSON files survive app uninstall. Restore loads the most recent backup.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )

            if (isBusy) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onBackup) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Backup")
                    }
                    OutlinedButton(onClick = onRestore) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore")
                    }
                }
                OutlinedButton(onClick = onSelectFolder, modifier = Modifier.fillMaxWidth()) {
                    Text("Choose Documents folder")
                }
            }
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
        }
    }
}

@Composable
private fun EmptyStateCard(onAddAsset: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAddAsset),
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
                "No entries yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tap + to add your first asset or liability",
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
    AssetCategory.HOME_LOAN -> Icons.Default.CreditCard
}
