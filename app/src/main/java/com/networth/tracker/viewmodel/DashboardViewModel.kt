package com.networth.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.networth.tracker.data.AssetEntity
import com.networth.tracker.data.AssetRepository
import com.networth.tracker.data.BankAccountEntity
import com.networth.tracker.data.ExchangeRateRepository
import com.networth.tracker.data.ExchangeRateState
import com.networth.tracker.data.NetWorthHistoryEntity
import com.networth.tracker.data.NetWorthSummary
import com.networth.tracker.data.ReturnMetrics
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: AssetRepository,
    private val exchangeRateRepository: ExchangeRateRepository
) : ViewModel() {

    init {
        viewModelScope.launch {
            exchangeRateRepository.refresh(force = false)
        }
    }

    val assets: StateFlow<List<AssetEntity>> = repository.assets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bankAccounts: StateFlow<List<BankAccountEntity>> = repository.bankAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<NetWorthSummary> = repository.netWorthSummary
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            NetWorthSummary(
                0.0, 0.0, 0.0, emptyList(),
                ReturnMetrics(0.0, 0.0, 0.0, 0.0)
            )
        )

    val netWorthHistory: StateFlow<List<NetWorthHistoryEntity>> = repository.netWorthHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exchangeRateState: StateFlow<ExchangeRateState> = exchangeRateRepository.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExchangeRateState())

    fun refreshExchangeRate() {
        viewModelScope.launch {
            exchangeRateRepository.refresh(force = true)
        }
    }

    fun deleteAsset(asset: AssetEntity) {
        viewModelScope.launch {
            repository.deleteAsset(asset)
        }
    }

    fun deleteAssets(assets: List<AssetEntity>) {
        viewModelScope.launch {
            repository.deleteAssets(assets)
        }
    }

    fun deleteBankAccount(bankAccount: BankAccountEntity) {
        viewModelScope.launch {
            repository.deleteBankAccount(bankAccount)
        }
    }

    fun deleteBankAccounts(bankAccounts: List<BankAccountEntity>) {
        viewModelScope.launch {
            repository.deleteBankAccounts(bankAccounts)
        }
    }
}

class DashboardViewModelFactory(
    private val repository: AssetRepository,
    private val exchangeRateRepository: ExchangeRateRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(repository, exchangeRateRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
