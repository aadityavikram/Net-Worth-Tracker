package com.networth.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.networth.tracker.data.AssetEntity
import com.networth.tracker.data.AssetRepository
import com.networth.tracker.data.AssetTransactionEntity
import com.networth.tracker.data.ExchangeRateRepository
import com.networth.tracker.data.ExchangeRateState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AssetTransactionsViewModel(
    private val repository: AssetRepository,
    exchangeRateRepository: ExchangeRateRepository,
    assetId: Long
) : ViewModel() {

    val asset: StateFlow<AssetEntity?> = repository.assets
        .map { list -> list.firstOrNull { it.id == assetId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val transactions: StateFlow<List<AssetTransactionEntity>> =
        repository.transactionsForAsset(assetId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exchangeRateState: StateFlow<ExchangeRateState> = exchangeRateRepository.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExchangeRateState())

    fun deleteTransaction(transaction: AssetTransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
}

class AssetTransactionsViewModelFactory(
    private val repository: AssetRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val assetId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AssetTransactionsViewModel::class.java)) {
            return AssetTransactionsViewModel(repository, exchangeRateRepository, assetId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
