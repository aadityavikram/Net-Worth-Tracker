package com.networth.tracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.networth.tracker.data.AssetCategory
import com.networth.tracker.data.Currency
import com.networth.tracker.viewmodel.AddEditAssetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAssetScreen(
    viewModel: AddEditAssetViewModel,
    onNavigateBack: () -> Unit
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()

    LaunchedEffect(formState.isSaved) {
        if (formState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (formState.id > 0) "Edit Entry" else "Add Entry")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (formState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Category", style = MaterialTheme.typography.titleSmall)
                CategorySelector(
                    selected = formState.category,
                    onSelect = viewModel::onCategoryChange
                )

                OutlinedTextField(
                    value = formState.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Name") },
                    placeholder = { Text("e.g. AAPL, HDFC Flexi Cap, Flat in Mumbai") },
                    isError = formState.nameError != null,
                    supportingText = formState.nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (!formState.category.isLiability) {
                    OutlinedTextField(
                        value = formState.investedAmount,
                        onValueChange = viewModel::onInvestedAmountChange,
                        label = { Text("Invested Amount") },
                        placeholder = { Text("Total amount invested") },
                        isError = formState.investedAmountError != null,
                        supportingText = formState.investedAmountError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = formState.currentAmount,
                    onValueChange = viewModel::onCurrentAmountChange,
                    label = {
                        Text(
                            if (formState.category.isLiability) "Outstanding Amount" else "Current Amount"
                        )
                    },
                    isError = formState.currentAmountError != null,
                    supportingText = formState.currentAmountError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Currency", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Currency.entries.forEach { currency ->
                        FilterChip(
                            selected = formState.currency == currency,
                            onClick = { viewModel.onCurrencyChange(currency) },
                            label = { Text("${currency.symbol} ${currency.code}") }
                        )
                    }
                }

                OutlinedTextField(
                    value = formState.notes,
                    onValueChange = viewModel::onNotesChange,
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.save() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (formState.id > 0) "Update Entry" else "Save Entry")
                }
            }
        }
    }
}

@Composable
private fun CategorySelector(
    selected: AssetCategory,
    onSelect: (AssetCategory) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Assets",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        CategoryChipRow(AssetCategory.assets, selected, onSelect)

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            "Liabilities",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        CategoryChipRow(AssetCategory.liabilities, selected, onSelect)
    }
}

@Composable
private fun CategoryChipRow(
    categories: List<AssetCategory>,
    selected: AssetCategory,
    onSelect: (AssetCategory) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { category ->
                    FilterChip(
                        selected = selected == category,
                        onClick = { onSelect(category) },
                        label = { Text(category.displayName) },
                        leadingIcon = {
                            CategoryIcon(category, size = 24)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
