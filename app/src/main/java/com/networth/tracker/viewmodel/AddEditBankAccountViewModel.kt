package com.networth.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.networth.tracker.data.AssetRepository
import com.networth.tracker.data.BankAccountAddContext
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
    val creditLimit: String = "",
    val asOfDateMillis: Long = System.currentTimeMillis(),
    val currency: Currency = Currency.INR,
    val notes: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val bankNameError: String? = null,
    val accountNameError: String? = null,
    val balanceError: String? = null,
    val creditLimitError: String? = null
) {
    val creditUtilisationPercent: Double?
        get() {
            if (accountType != BankAccountType.CREDIT_CARD) return null
            val limit = creditLimit.toDoubleOrNull() ?: return null
            val outstanding = balance.toDoubleOrNull() ?: return null
            if (limit <= 0) return null
            return (outstanding / limit) * 100.0
        }
}

class AddEditBankAccountViewModel(
    private val repository: AssetRepository,
    private val bankAccountId: Long?,
    private val addContext: BankAccountAddContext?
) : ViewModel() {

    private val _formState = MutableStateFlow(BankAccountFormState())
    val formState: StateFlow<BankAccountFormState> = _formState.asStateFlow()

    val allowedAccountTypes: List<BankAccountType>
        get() = addContext?.allowedTypes ?: emptyList()

    fun resolvedAllowedAccountTypes(): List<BankAccountType> {
        if (allowedAccountTypes.isNotEmpty()) return allowedAccountTypes
        val accountType = _formState.value.accountType
        return if (accountType.isLiability) {
            BankAccountType.liabilities
        } else {
            BankAccountType.assets
        }
    }

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
                            creditLimit = if (account.creditLimit > 0) {
                                account.creditLimit.toString()
                            } else {
                                ""
                            },
                            asOfDateMillis = account.asOfDateMillis.takeIf { it > 0 }
                                ?: account.updatedAt,
                            currency = account.currency,
                            notes = account.notes
                        )
                    }
                }
                _formState.update { it.copy(isLoading = false) }
            }
        } else if (addContext != null) {
            _formState.update {
                it.copy(accountType = addContext.allowedTypes.first())
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
        _formState.update {
            it.copy(
                accountType = type,
                balanceError = null,
                creditLimitError = null,
                creditLimit = if (type == BankAccountType.CREDIT_CARD) it.creditLimit else ""
            )
        }
    }

    fun onBalanceChange(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            _formState.update { it.copy(balance = value, balanceError = null) }
        }
    }

    fun onCreditLimitChange(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
            _formState.update { it.copy(creditLimit = value, creditLimitError = null) }
        }
    }

    fun onCurrencyChange(currency: Currency) {
        _formState.update { it.copy(currency = currency) }
    }

    fun onAsOfDateChange(millis: Long) {
        _formState.update { it.copy(asOfDateMillis = millis) }
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
        if (balance == null || balance < 0) {
            _formState.update { it.copy(balanceError = "Enter a valid balance (0 or more)") }
            valid = false
        }

        val creditLimit = state.creditLimit.toDoubleOrNull()
        if (state.accountType == BankAccountType.CREDIT_CARD) {
            if (creditLimit == null || creditLimit <= 0) {
                _formState.update { it.copy(creditLimitError = "Enter a valid credit limit") }
                valid = false
            }
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
                    notes = state.notes.trim(),
                    creditLimit = if (state.accountType == BankAccountType.CREDIT_CARD) {
                        creditLimit ?: 0.0
                    } else {
                        0.0
                    },
                    asOfDateMillis = state.asOfDateMillis
                )
            )
            _formState.update { it.copy(isSaved = true) }
        }
        return true
    }
}

class AddEditBankAccountViewModelFactory(
    private val repository: AssetRepository,
    private val bankAccountId: Long?,
    private val addContext: BankAccountAddContext?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEditBankAccountViewModel::class.java)) {
            return AddEditBankAccountViewModel(repository, bankAccountId, addContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
