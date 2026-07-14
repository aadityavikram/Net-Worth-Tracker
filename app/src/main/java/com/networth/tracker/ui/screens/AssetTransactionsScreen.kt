package com.networth.tracker.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.networth.tracker.data.AssetRepository
import com.networth.tracker.data.AssetTransactionEntity
import com.networth.tracker.ui.screens.dashboard.EmptyStateCard
import com.networth.tracker.ui.theme.LiabilityColor
import com.networth.tracker.util.FormatUtils
import com.networth.tracker.viewmodel.AssetTransactionsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetTransactionsScreen(
    viewModel: AssetTransactionsViewModel,
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    onEditAsset: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val asset by viewModel.asset.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val exchangeRateState by viewModel.exchangeRateState.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<AssetTransactionEntity?>(null) }
    val holding = asset

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(holding?.name ?: "Transactions")
                        holding?.let {
                            Text(
                                FormatUtils.formatCompactInr(
                                    AssetRepository.toInr(
                                        it.amount,
                                        it.currency,
                                        exchangeRateState.rate
                                    )
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEditAsset) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit holding")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) {
                Icon(Icons.Default.Add, contentDescription = "Add transaction")
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
                Text(
                    when {
                        holding?.category?.isLiability == true ->
                            "Each transaction is outstanding balance as of that date. The graph follows these dates."
                        holding?.usesValueCheckpoints() == true ->
                            "Each transaction is value as of that date (e.g. purchase then current valuation)."
                        else ->
                            "Add buys/SIPs under this holding. The graph follows each transaction date."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
            if (transactions.isEmpty()) {
                item {
                    EmptyStateCard(
                        onAdd = onAddTransaction,
                        title = "No transactions yet",
                        description = "Tap + to add the first transaction for this holding"
                    )
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    TransactionListItem(
                        transaction = tx,
                        currencyCode = holding?.currency?.code ?: "INR",
                        isLiability = holding?.category?.isLiability == true,
                        onClick = { onEditTransaction(tx.id) },
                        onDelete = { toDelete = tx }
                    )
                }
            }
        }
    }

    toDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete transaction?") },
            text = { Text("Remove \"${tx.title.ifBlank { "transaction" }}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        toDelete = null
                    }
                ) {
                    Text("Delete", color = LiabilityColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TransactionListItem(
    transaction: AssetTransactionEntity,
    currencyCode: String,
    isLiability: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    transaction.title.ifBlank { "Transaction" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    FormatUtils.formatDate(transaction.dateMillis).ifBlank { "No date" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (transaction.notes.isNotBlank()) {
                    Text(
                        transaction.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            Text(
                FormatUtils.formatCurrency(transaction.amount, 
                    com.networth.tracker.data.Currency.entries.firstOrNull { it.code == currencyCode }
                        ?: com.networth.tracker.data.Currency.INR
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isLiability) LiabilityColor else MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LiabilityColor)
            }
        }
    }
}
