package com.networth.tracker.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.networth.tracker.data.AssetCategory
import com.networth.tracker.data.Currency
import com.networth.tracker.data.cas.CasHolding
import com.networth.tracker.data.cas.CasParseResult
import com.networth.tracker.util.FormatUtils
import com.networth.tracker.viewmodel.AddEditAssetViewModel
import com.networth.tracker.viewmodel.AssetFormState

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
                    allowedCategories = viewModel.resolvedAllowedCategories(),
                    selected = formState.category,
                    onSelect = viewModel::onCategoryChange
                )

                if (formState.category == AssetCategory.MUTUAL_FUNDS && formState.id == 0L) {
                    CasImportSection(
                        formState = formState,
                        onPasswordChange = viewModel::onCasPasswordChange,
                        onReplaceChange = viewModel::onCasReplaceExistingChange,
                        onPdfSelected = viewModel::onCasPdfSelected,
                        onParse = viewModel::parseSelectedCas
                    )
                }

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

                if (formState.category.isLoan) {
                    OutlinedTextField(
                        value = formState.investedAmount,
                        onValueChange = viewModel::onInvestedAmountChange,
                        label = { Text("Original Amount") },
                        placeholder = { Text("Total loan amount sanctioned") },
                        isError = formState.investedAmountError != null,
                        supportingText = formState.investedAmountError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else if (!formState.category.isLiability) {
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
                            when {
                                formState.category.isLoan -> "Outstanding Amount"
                                formState.category.isLiability -> "Outstanding Amount"
                                else -> "Current Amount"
                            }
                        )
                    },
                    isError = formState.currentAmountError != null,
                    supportingText = formState.currentAmountError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (formState.category.isLoan) {
                    OutlinedTextField(
                        value = formState.interestRate,
                        onValueChange = viewModel::onInterestRateChange,
                        label = { Text("Rate of Interest (%)") },
                        placeholder = { Text("e.g. 8.5") },
                        isError = formState.interestRateError != null,
                        supportingText = formState.interestRateError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    DateTakenField(
                        dateTakenMillis = formState.dateTakenMillis,
                        onDateSelected = viewModel::onDateTakenChange
                    )
                }

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

    formState.casPreview?.let { preview ->
        CasImportPreviewDialog(
            preview = preview,
            replaceExisting = formState.casReplaceExisting,
            importing = formState.casImporting,
            onDismiss = viewModel::clearCasPreview,
            onConfirm = viewModel::confirmCasImport
        )
    }
}

@Composable
private fun CasImportSection(
    formState: AssetFormState,
    onPasswordChange: (String) -> Unit,
    onReplaceChange: (Boolean) -> Unit,
    onPdfSelected: (Uri) -> Unit,
    onParse: () -> Unit
) {
    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) onPdfSelected(uri)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Import from CAS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Download your Consolidated Account Statement from MF Central, CAMS, or KFintech, then import it here. Password is usually your PAN.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = formState.casPassword,
                onValueChange = onPasswordChange,
                label = { Text("CAS password (PAN)") },
                placeholder = { Text("ABCDE1234F") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = formState.casReplaceExisting,
                    onCheckedChange = onReplaceChange
                )
                Text(
                    "Replace existing mutual funds",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { onReplaceChange(!formState.casReplaceExisting) }
                )
            }

            OutlinedButton(
                onClick = { pdfPicker.launch("application/pdf") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !formState.casParsing
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (formState.casPendingUri != null) "CAS PDF selected — change file"
                    else "Choose CAS PDF"
                )
            }

            Button(
                onClick = onParse,
                modifier = Modifier.fillMaxWidth(),
                enabled = formState.casPendingUri != null &&
                    formState.casPassword.isNotBlank() &&
                    !formState.casParsing
            ) {
                if (formState.casParsing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Parsing…")
                } else {
                    Text("Parse & preview holdings")
                }
            }

            formState.casError?.let { error ->
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun CasImportPreviewDialog(
    preview: CasParseResult,
    replaceExisting: Boolean,
    importing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!importing) onDismiss() },
        title = { Text("Import ${preview.holdings.size} mutual funds") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (preview.sourceHint.isNotBlank() || preview.statementAsOf.isNotBlank()) {
                    Text(
                        buildString {
                            if (preview.sourceHint.isNotBlank()) append(preview.sourceHint)
                            if (preview.statementAsOf.isNotBlank()) {
                                if (isNotEmpty()) append(" · ")
                                append("As of ${preview.statementAsOf}")
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                CasTotalsRow(preview)

                Text(
                    if (replaceExisting) {
                        "This will replace all existing Mutual Funds entries."
                    } else {
                        "This will add these funds alongside existing Mutual Funds."
                    },
                    style = MaterialTheme.typography.bodySmall
                )

                HorizontalDivider()

                LazyColumn(
                    modifier = Modifier.heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(preview.holdings, key = { "${it.folio}-${it.schemeName}" }) { holding ->
                        CasHoldingRow(holding)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !importing) {
                Text(if (importing) "Importing…" else "Import all")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !importing) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun CasTotalsRow(preview: CasParseResult) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Invested: ${FormatUtils.formatCompactInr(preview.totalInvested)}")
        Text("Current: ${FormatUtils.formatCompactInr(preview.totalCurrent)}")
        Text(
            "Return: ${FormatUtils.formatSignedInr(preview.totalReturn)} " +
                "(${FormatUtils.formatReturnPercent(preview.returnPercent)})",
            fontWeight = FontWeight.SemiBold,
            color = if (preview.totalReturn >= 0) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        )
    }
}

@Composable
private fun CasHoldingRow(holding: CasHolding) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            holding.schemeName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2
        )
        if (holding.folio.isNotBlank()) {
            Text(
                "Folio ${holding.folio}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "Invested ${FormatUtils.formatCompactInr(holding.investedAmount)} · " +
                "Current ${FormatUtils.formatCompactInr(holding.currentAmount)} · " +
                FormatUtils.formatReturnPercent(holding.returnPercent),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTakenField(
    dateTakenMillis: Long,
    onDateSelected: (Long) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val displayDate = FormatUtils.formatDate(dateTakenMillis).ifBlank { "Select date" }
    val openPicker = { showPicker = true }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = displayDate,
            onValueChange = {},
            readOnly = true,
            label = { Text("Date Taken") },
            trailingIcon = {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClick = openPicker)
        )
    }

    if (showPicker) {
        val initialMillis = dateTakenMillis.takeIf { it > 0 } ?: System.currentTimeMillis()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(onDateSelected)
                        showPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun CategorySelector(
    allowedCategories: List<AssetCategory>,
    selected: AssetCategory,
    onSelect: (AssetCategory) -> Unit
) {
    val assetCategories = allowedCategories.filter { !it.isLiability }
    val liabilityCategories = allowedCategories.filter { it.isLiability }

    if (assetCategories.isNotEmpty() && liabilityCategories.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Assets",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            CategoryChipRow(assetCategories, selected, onSelect)

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Liabilities",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            CategoryChipRow(liabilityCategories, selected, onSelect)
        }
        return
    }

    CategoryChipRow(allowedCategories, selected, onSelect)
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
