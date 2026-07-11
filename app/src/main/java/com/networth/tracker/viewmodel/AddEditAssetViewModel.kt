package com.networth.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
    val currency: Currency = Currency.USD,
    val notes: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val nameError: String? = null,
    val investedAmountError: String? = null,
    val currentAmountError: String? = null
)

class AddEditAssetViewModel(
    private val repository: AssetRepository,
    private val assetId: Long?
) : ViewModel() {

    private val _formState = MutableStateFlow(AssetFormState())
    val formState: StateFlow<AssetFormState> = _formState.asStateFlow()

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
                            investedAmount = asset.investedAmount.toString(),
                            currentAmount = asset.amount.toString(),
                            currency = asset.currency,
                            notes = asset.notes
                        )
                    }
                }
                _formState.update { it.copy(isLoading = false) }
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
                currentAmountError = null
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
            _formState.update { it.copy(currentAmountError = "Enter a valid current amount") }
            valid = false
        }

        var investedAmount = 0.0
        if (!state.category.isLiability) {
            val parsedInvested = state.investedAmount.toDoubleOrNull()
            if (parsedInvested == null || parsedInvested <= 0) {
                _formState.update { it.copy(investedAmountError = "Enter a valid invested amount") }
                valid = false
            } else {
                investedAmount = parsedInvested
            }
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
                    notes = state.notes.trim()
                )
            )
            _formState.update { it.copy(isSaved = true) }
        }
        return true
    }
}

class AddEditAssetViewModelFactory(
    private val repository: AssetRepository,
    private val assetId: Long?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEditAssetViewModel::class.java)) {
            return AddEditAssetViewModel(repository, assetId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
