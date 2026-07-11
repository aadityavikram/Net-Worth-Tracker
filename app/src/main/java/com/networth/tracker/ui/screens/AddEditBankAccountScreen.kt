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
import com.networth.tracker.data.BankAccountType
import com.networth.tracker.data.Currency
import com.networth.tracker.viewmodel.AddEditBankAccountViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBankAccountScreen(
    viewModel: AddEditBankAccountViewModel,
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
                    Text(if (formState.id > 0) "Edit Bank Account" else "Add Bank Account")
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
                Text("Account Type", style = MaterialTheme.typography.titleSmall)
                BankAccountTypeSelector(
                    selected = formState.accountType,
                    onSelect = viewModel::onAccountTypeChange
                )

                OutlinedTextField(
                    value = formState.bankName,
                    onValueChange = viewModel::onBankNameChange,
                    label = { Text("Bank Name") },
                    placeholder = { Text("e.g. HDFC, SBI, Chase") },
                    isError = formState.bankNameError != null,
                    supportingText = formState.bankNameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = formState.accountName,
                    onValueChange = viewModel::onAccountNameChange,
                    label = { Text("Account Name") },
                    placeholder = { Text("e.g. Salary Account, Primary Savings") },
                    isError = formState.accountNameError != null,
                    supportingText = formState.accountNameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = formState.accountNumber,
                    onValueChange = viewModel::onAccountNumberChange,
                    label = { Text("Account Number (optional)") },
                    placeholder = { Text("Last 4 digits shown in list") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = formState.balance,
                    onValueChange = viewModel::onBalanceChange,
                    label = {
                        Text(
                            if (formState.accountType.isLiability) "Outstanding Balance" else "Current Balance"
                        )
                    },
                    isError = formState.balanceError != null,
                    supportingText = formState.balanceError?.let { { Text(it) } },
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
                    Text(if (formState.id > 0) "Update Account" else "Save Account")
                }
            }
        }
    }
}

@Composable
private fun BankAccountTypeSelector(
    selected: BankAccountType,
    onSelect: (BankAccountType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Assets",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        BankAccountTypeChipRow(BankAccountType.assets, selected, onSelect)

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            "Liabilities",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        BankAccountTypeChipRow(BankAccountType.liabilities, selected, onSelect)
    }
}

@Composable
private fun BankAccountTypeChipRow(
    types: List<BankAccountType>,
    selected: BankAccountType,
    onSelect: (BankAccountType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        types.forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = { Text(type.displayName) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
