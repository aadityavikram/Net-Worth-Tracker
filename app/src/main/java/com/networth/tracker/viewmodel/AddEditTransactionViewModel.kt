package com.networth.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.networth.tracker.data.AssetRepository
import com.networth.tracker.data.AssetTransactionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionFormState(
    val id: Long = 0,
    val assetId: Long = 0,
    val title: String = "",
    val amount: String = "",
    val investedAmount: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isLiability: Boolean = false,
    val showInvested: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val titleError: String? = null,
    val amountError: String? = null
)

class AddEditTransactionViewModel(
    private val repository: AssetRepository,
    private val assetId: Long,
    private val transactionId: Long?
) : ViewModel() {

    private val _formState = MutableStateFlow(TransactionFormState(assetId = assetId))
    val formState: StateFlow<TransactionFormState> = _formState.asStateFlow()

    init {
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }
            val asset = repository.getAsset(assetId)
            val isLiability = asset?.category?.isLiability == true
            val showInvested = asset != null && !asset.category.isLiability
            if (transactionId != null && transactionId > 0) {
                repository.getTransaction(transactionId)?.let { tx ->
                    _formState.update {
                        TransactionFormState(
                            id = tx.id,
                            assetId = tx.assetId,
                            title = tx.title,
                            amount = tx.amount.toString(),
                            investedAmount = if (tx.investedAmount > 0) tx.investedAmount.toString() else "",
                            dateMillis = tx.dateMillis,
                            notes = tx.notes,
                            isLiability = isLiability,
                            showInvested = showInvested
                        )
                    }
                }
            } else {
                _formState.update {
                    it.copy(
                        isLiability = isLiability,
                        showInvested = showInvested,
                        title = if (isLiability) "Outstanding update" else "Buy / SIP",
                        dateMillis = System.currentTimeMillis()
                    )
                }
            }
            _formState.update { it.copy(isLoading = false) }
        }
    }

    fun onTitleChange(value: String) {
        _formState.update { it.copy(title = value, titleError = null) }
    }

    fun onAmountChange(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            _formState.update { it.copy(amount = value, amountError = null) }
        }
    }

    fun onInvestedAmountChange(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            _formState.update { it.copy(investedAmount = value) }
        }
    }

    fun onDateChange(millis: Long) {
        _formState.update { it.copy(dateMillis = millis) }
    }

    fun onNotesChange(value: String) {
        _formState.update { it.copy(notes = value) }
    }

    fun save(): Boolean {
        val state = _formState.value
        var valid = true
        if (state.title.isBlank()) {
            _formState.update { it.copy(titleError = "Title is required") }
            valid = false
        }
        val amount = state.amount.toDoubleOrNull()
        if (amount == null || amount < 0) {
            _formState.update { it.copy(amountError = "Enter a valid amount") }
            valid = false
        }
        if (!valid || amount == null) return false

        viewModelScope.launch {
            repository.saveTransaction(
                AssetTransactionEntity(
                    id = state.id,
                    assetId = state.assetId,
                    title = state.title.trim(),
                    amount = amount,
                    investedAmount = state.investedAmount.toDoubleOrNull() ?: 0.0,
                    dateMillis = state.dateMillis,
                    notes = state.notes.trim()
                )
            )
            _formState.update { it.copy(isSaved = true) }
        }
        return true
    }
}

class AddEditTransactionViewModelFactory(
    private val repository: AssetRepository,
    private val assetId: Long,
    private val transactionId: Long?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEditTransactionViewModel::class.java)) {
            return AddEditTransactionViewModel(repository, assetId, transactionId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
