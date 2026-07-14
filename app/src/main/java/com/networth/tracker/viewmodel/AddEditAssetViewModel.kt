package com.networth.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.networth.tracker.data.AssetAddContext
import com.networth.tracker.data.AssetCategory
import com.networth.tracker.data.AssetEntity
import com.networth.tracker.data.AssetRepository
import com.networth.tracker.data.Currency
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
    val valuationDateMillis: Long = System.currentTimeMillis(),
    val currency: Currency = Currency.USD,
    val notes: String = "",
    /** True when this holding has ledger rows; amounts must change via transactions. */
    val amountsLocked: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val nameError: String? = null,
    val investedAmountError: String? = null,
    val currentAmountError: String? = null,
    val interestRateError: String? = null
)

class AddEditAssetViewModel(
    private val repository: AssetRepository,
    private val assetId: Long?,
    private val addContext: AssetAddContext?
) : ViewModel() {

    private val _formState = MutableStateFlow(
        AssetFormState(dateTakenMillis = System.currentTimeMillis())
    )
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
                val asset = repository.getAsset(assetId)
                val amountsLocked = repository.hasTransactionsForAsset(assetId)
                asset?.let { loaded ->
                    _formState.update {
                        AssetFormState(
                            id = loaded.id,
                            name = loaded.name,
                            category = loaded.category,
                            investedAmount = if (loaded.category.isLoan && loaded.investedAmount > 0) {
                                loaded.investedAmount.toString()
                            } else if (!loaded.category.isLiability && loaded.investedAmount > 0) {
                                loaded.investedAmount.toString()
                            } else {
                                ""
                            },
                            currentAmount = loaded.amount.toString(),
                            interestRate = if (loaded.interestRate > 0) loaded.interestRate.toString() else "",
                            dateTakenMillis = loaded.dateTakenMillis.takeIf { it > 0 } ?: loaded.updatedAt,
                            valuationDateMillis = loaded.valuationDateMillis.takeIf { it > 0 }
                                ?: System.currentTimeMillis(),
                            currency = loaded.currency,
                            notes = loaded.notes,
                            amountsLocked = amountsLocked
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
                    currency = defaultCategory.defaultCurrency,
                    dateTakenMillis = System.currentTimeMillis(),
                    valuationDateMillis = System.currentTimeMillis()
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
                interestRateError = null
            )
        }
    }

    fun onInvestedAmountChange(amount: String) {
        if (_formState.value.amountsLocked) return
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d*$"))) {
            _formState.update { it.copy(investedAmount = amount, investedAmountError = null) }
        }
    }

    fun onCurrentAmountChange(amount: String) {
        if (_formState.value.amountsLocked) return
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

    fun onValuationDateChange(millis: Long) {
        if (_formState.value.amountsLocked) return
        _formState.update { it.copy(valuationDateMillis = millis) }
    }

    fun onCurrencyChange(currency: Currency) {
        _formState.update { it.copy(currency = currency) }
    }

    fun onNotesChange(notes: String) {
        _formState.update { it.copy(notes = notes) }
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
                    dateTakenMillis = state.dateTakenMillis,
                    valuationDateMillis = if (state.category == AssetCategory.REAL_ESTATE) {
                        state.valuationDateMillis.takeIf { it > 0 } ?: System.currentTimeMillis()
                    } else {
                        state.valuationDateMillis
                    }
                )
            )
            _formState.update { it.copy(isSaved = true) }
        }
        return true
    }
}

class AddEditAssetViewModelFactory(
    private val repository: AssetRepository,
    private val assetId: Long?,
    private val addContext: AssetAddContext?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEditAssetViewModel::class.java)) {
            return AddEditAssetViewModel(repository, assetId, addContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
