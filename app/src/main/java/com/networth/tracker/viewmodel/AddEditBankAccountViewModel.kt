package com.networth.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.networth.tracker.data.AssetRepository
import com.networth.tracker.data.BankAccountEntity
import com.networth.tracker.data.BankAccountType
import com.networth.tracker.data.Currency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BankAccountFormState(
    val id: Long = 0,
    val bankName: String = "",
    val accountName: String = "",
    val accountNumber: String = "",
    val accountType: BankAccountType = BankAccountType.SAVINGS,
    val balance: String = "",
    val currency: Currency = Currency.INR,
    val notes: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val bankNameError: String? = null,
    val accountNameError: String? = null,
    val balanceError: String? = null
)

class AddEditBankAccountViewModel(
    private val repository: AssetRepository,
    private val bankAccountId: Long?
) : ViewModel() {

    private val _formState = MutableStateFlow(BankAccountFormState())
    val formState: StateFlow<BankAccountFormState> = _formState.asStateFlow()

    init {
        if (bankAccountId != null && bankAccountId > 0) {
            viewModelScope.launch {
                _formState.update { it.copy(isLoading = true) }
                repository.getBankAccount(bankAccountId)?.let { account ->
                    _formState.update {
                        BankAccountFormState(
                            id = account.id,
                            bankName = account.bankName,
                            accountName = account.accountName,
                            accountNumber = account.accountNumber,
                            accountType = account.accountType,
                            balance = account.balance.toString(),
                            currency = account.currency,
                            notes = account.notes
                        )
                    }
                }
                _formState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onBankNameChange(value: String) {
        _formState.update { it.copy(bankName = value, bankNameError = null) }
    }

    fun onAccountNameChange(value: String) {
        _formState.update { it.copy(accountName = value, accountNameError = null) }
    }

    fun onAccountNumberChange(value: String) {
        _formState.update { it.copy(accountNumber = value) }
    }

    fun onAccountTypeChange(type: BankAccountType) {
        _formState.update { it.copy(accountType = type, balanceError = null) }
    }

    fun onBalanceChange(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            _formState.update { it.copy(balance = value, balanceError = null) }
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

        if (state.bankName.isBlank()) {
            _formState.update { it.copy(bankNameError = "Bank name is required") }
            valid = false
        }

        if (state.accountName.isBlank()) {
            _formState.update { it.copy(accountNameError = "Account name is required") }
            valid = false
        }

        val balance = state.balance.toDoubleOrNull()
        if (balance == null || balance <= 0) {
            _formState.update { it.copy(balanceError = "Enter a valid balance") }
            valid = false
        }

        if (!valid || balance == null) return false

        viewModelScope.launch {
            repository.saveBankAccount(
                BankAccountEntity(
                    id = state.id,
                    bankName = state.bankName.trim(),
                    accountName = state.accountName.trim(),
                    accountNumber = state.accountNumber.trim(),
                    accountType = state.accountType,
                    balance = balance,
                    currency = state.currency,
                    notes = state.notes.trim()
                )
            )
            _formState.update { it.copy(isSaved = true) }
        }
        return true
    }
}

class AddEditBankAccountViewModelFactory(
    private val repository: AssetRepository,
    private val bankAccountId: Long?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEditBankAccountViewModel::class.java)) {
            return AddEditBankAccountViewModel(repository, bankAccountId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
