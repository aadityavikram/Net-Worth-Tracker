package com.networth.tracker.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.networth.tracker.data.AssetAddContext
import com.networth.tracker.data.AssetCategory
import com.networth.tracker.data.AssetEntity
import com.networth.tracker.data.AssetRepository
import com.networth.tracker.data.Currency
import com.networth.tracker.data.cas.CasImportException
import com.networth.tracker.data.cas.CasParseResult
import com.networth.tracker.data.cas.CasPdfImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AssetFormState(
    val id: Long = 0,
    val name: String = "",
    val category: AssetCategory = AssetCategory.US_STOCKS,
    val investedAmount: String = "",
    val currentAmount: String = "",
    val interestRate: String = "",
    val dateTakenMillis: Long = 0L,
    val currency: Currency = Currency.USD,
    val notes: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val nameError: String? = null,
    val investedAmountError: String? = null,
    val currentAmountError: String? = null,
    val interestRateError: String? = null,
    val casPassword: String = "",
    val casParsing: Boolean = false,
    val casImporting: Boolean = false,
    val casError: String? = null,
    val casPreview: CasParseResult? = null,
    val casReplaceExisting: Boolean = true,
    val casPendingUri: Uri? = null
)

class AddEditAssetViewModel(
    application: Application,
    private val repository: AssetRepository,
    private val assetId: Long?,
    private val addContext: AssetAddContext?
) : AndroidViewModel(application) {

    private val _formState = MutableStateFlow(AssetFormState())
    val formState: StateFlow<AssetFormState> = _formState.asStateFlow()

    fun resolvedAllowedCategories(): List<AssetCategory> {
        addContext?.allowedCategories?.let { return it }
        val category = _formState.value.category
        return if (category.isLiability) {
            AssetCategory.liabilities
        } else {
            AssetCategory.assets
        }
    }

    init {
        if (assetId != null && assetId > 0) {
            viewModelScope.launch {
                _formState.update { it.copy(isLoading = true) }
                repository.getAsset(assetId)?.let { asset ->
                    _formState.update {
                        AssetFormState(
                            id = asset.id,
                            name = asset.name,
                            category = asset.category,
                            investedAmount = if (asset.category.isLoan && asset.investedAmount > 0) {
                                asset.investedAmount.toString()
                            } else if (!asset.category.isLiability && asset.investedAmount > 0) {
                                asset.investedAmount.toString()
                            } else {
                                ""
                            },
                            currentAmount = asset.amount.toString(),
                            interestRate = if (asset.interestRate > 0) asset.interestRate.toString() else "",
                            dateTakenMillis = asset.dateTakenMillis,
                            currency = asset.currency,
                            notes = asset.notes
                        )
                    }
                }
                _formState.update { it.copy(isLoading = false) }
            }
        } else if (addContext != null) {
            val defaultCategory = addContext.allowedCategories.first()
            _formState.update {
                it.copy(
                    category = defaultCategory,
                    currency = defaultCategory.defaultCurrency
                )
            }
        }
    }

    fun onNameChange(name: String) {
        _formState.update { it.copy(name = name, nameError = null) }
    }

    fun onCategoryChange(category: AssetCategory) {
        _formState.update {
            it.copy(
                category = category,
                currency = category.defaultCurrency,
                investedAmountError = null,
                currentAmountError = null,
                interestRateError = null,
                casError = null,
                casPreview = null,
                casPendingUri = null
            )
        }
    }

    fun onInvestedAmountChange(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d*$"))) {
            _formState.update { it.copy(investedAmount = amount, investedAmountError = null) }
        }
    }

    fun onCurrentAmountChange(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d*$"))) {
            _formState.update { it.copy(currentAmount = amount, currentAmountError = null) }
        }
    }

    fun onInterestRateChange(rate: String) {
        if (rate.isEmpty() || rate.matches(Regex("^\\d*\\.?\\d*$"))) {
            _formState.update { it.copy(interestRate = rate, interestRateError = null) }
        }
    }

    fun onDateTakenChange(millis: Long) {
        _formState.update { it.copy(dateTakenMillis = millis) }
    }

    fun onCurrencyChange(currency: Currency) {
        _formState.update { it.copy(currency = currency) }
    }

    fun onNotesChange(notes: String) {
        _formState.update { it.copy(notes = notes) }
    }

    fun onCasPasswordChange(password: String) {
        _formState.update { it.copy(casPassword = password, casError = null) }
    }

    fun onCasReplaceExistingChange(replace: Boolean) {
        _formState.update { it.copy(casReplaceExisting = replace) }
    }

    fun onCasPdfSelected(uri: Uri) {
        _formState.update {
            it.copy(
                casPendingUri = uri,
                casPreview = null,
                casError = null
            )
        }
    }

    fun clearCasPreview() {
        _formState.update {
            it.copy(
                casPreview = null,
                casError = null,
                casPendingUri = null,
                casParsing = false,
                casImporting = false
            )
        }
    }

    fun parseSelectedCas() {
        val state = _formState.value
        val uri = state.casPendingUri ?: return
        if (state.casPassword.isBlank()) {
            _formState.update { it.copy(casError = "Enter your PAN used as the CAS PDF password") }
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(casParsing = true, casError = null, casPreview = null) }
            try {
                val result = CasPdfImporter.importFromUri(
                    context = getApplication(),
                    uri = uri,
                    password = state.casPassword
                )
                val uniqueHoldings = result.holdings
                    .groupBy {
                        val folio = it.folio.replace(" ", "")
                        when {
                            folio.isNotBlank() && it.isin.isNotBlank() -> "$folio|${it.isin}"
                            folio.isNotBlank() -> folio
                            else -> "${it.isin}|${"%.2f".format(it.investedAmount)}|${"%.2f".format(it.currentAmount)}"
                        }
                    }
                    .map { (_, group) ->
                        group.maxBy {
                            (if (it.isin.isNotBlank()) 100 else 0) +
                                (if (!it.schemeName.startsWith("Mutual Fund")) 50 else 0) +
                                it.units
                        }
                    }
                _formState.update {
                    it.copy(
                        casParsing = false,
                        casPreview = result.copy(holdings = uniqueHoldings),
                        casError = null
                    )
                }
            } catch (e: CasImportException) {
                _formState.update {
                    it.copy(casParsing = false, casError = e.message, casPreview = null)
                }
            } catch (e: Exception) {
                _formState.update {
                    it.copy(
                        casParsing = false,
                        casError = e.message ?: "Failed to parse CAS",
                        casPreview = null
                    )
                }
            }
        }
    }

    fun confirmCasImport() {
        val preview = _formState.value.casPreview ?: return
        val replace = _formState.value.casReplaceExisting
        viewModelScope.launch {
            _formState.update { it.copy(casImporting = true, casError = null) }
            try {
                // Preview list should also be unique by folio+ISIN.
            val unique = preview.holdings
                .groupBy {
                    val folio = it.folio.replace(" ", "")
                    when {
                        folio.isNotBlank() && it.isin.isNotBlank() -> "$folio|${it.isin}"
                        folio.isNotBlank() -> "$folio|${"%.2f".format(it.currentAmount)}"
                        else -> "${it.isin}|${"%.2f".format(it.currentAmount)}"
                    }
                }
                .map { (_, group) -> group.maxBy { it.units + it.investedAmount } }
            repository.importMutualFundHoldings(unique, replaceExisting = replace)
                _formState.update {
                    it.copy(
                        casImporting = false,
                        casPreview = null,
                        casPendingUri = null,
                        isSaved = true
                    )
                }
            } catch (e: Exception) {
                _formState.update {
                    it.copy(
                        casImporting = false,
                        casError = e.message ?: "Failed to save mutual funds"
                    )
                }
            }
        }
    }

    fun save(): Boolean {
        val state = _formState.value
        var valid = true

        if (state.name.isBlank()) {
            _formState.update { it.copy(nameError = "Name is required") }
            valid = false
        }

        val currentAmount = state.currentAmount.toDoubleOrNull()
        if (currentAmount == null || currentAmount <= 0) {
            _formState.update { it.copy(currentAmountError = "Enter a valid amount") }
            valid = false
        }

        var investedAmount = 0.0
        if (state.category.isLoan) {
            val parsedOriginal = state.investedAmount.toDoubleOrNull()
            if (parsedOriginal == null || parsedOriginal <= 0) {
                _formState.update { it.copy(investedAmountError = "Enter a valid original amount") }
                valid = false
            } else {
                investedAmount = parsedOriginal
            }
        } else if (!state.category.isLiability) {
            val parsedInvested = state.investedAmount.toDoubleOrNull()
            if (parsedInvested == null || parsedInvested <= 0) {
                _formState.update { it.copy(investedAmountError = "Enter a valid invested amount") }
                valid = false
            } else {
                investedAmount = parsedInvested
            }
        }

        val interestRate = state.interestRate.toDoubleOrNull()
        if (state.interestRate.isNotBlank() && (interestRate == null || interestRate < 0)) {
            _formState.update { it.copy(interestRateError = "Enter a valid interest rate") }
            valid = false
        }

        if (!valid || currentAmount == null) return false

        viewModelScope.launch {
            repository.saveAsset(
                AssetEntity(
                    id = state.id,
                    name = state.name.trim(),
                    category = state.category,
                    amount = currentAmount,
                    investedAmount = investedAmount,
                    currency = state.currency,
                    notes = state.notes.trim(),
                    interestRate = interestRate ?: 0.0,
                    dateTakenMillis = state.dateTakenMillis
                )
            )
            _formState.update { it.copy(isSaved = true) }
        }
        return true
    }
}

class AddEditAssetViewModelFactory(
    private val application: Application,
    private val repository: AssetRepository,
    private val assetId: Long?,
    private val addContext: AssetAddContext?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEditAssetViewModel::class.java)) {
            return AddEditAssetViewModel(application, repository, assetId, addContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
