package com.networth.tracker.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.networth.tracker.data.AssetEntity
import com.networth.tracker.data.AssetRepository
import com.networth.tracker.data.BackupActionResult
import com.networth.tracker.data.BackupInfo
import com.networth.tracker.data.ExchangeRateRepository
import com.networth.tracker.data.ExchangeRateState
import com.networth.tracker.data.NetWorthSummary
import com.networth.tracker.data.ReturnMetrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: AssetRepository,
    private val exchangeRateRepository: ExchangeRateRepository
) : ViewModel() {

    private val _backupInfo = MutableStateFlow(BackupInfo())
    val backupInfo: StateFlow<BackupInfo> = _backupInfo.asStateFlow()

    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    private val _needsFolderAccess = MutableStateFlow(false)
    val needsFolderAccess: StateFlow<Boolean> = _needsFolderAccess.asStateFlow()

    private val _isBackupBusy = MutableStateFlow(false)
    val isBackupBusy: StateFlow<Boolean> = _isBackupBusy.asStateFlow()

    init {
        viewModelScope.launch {
            exchangeRateRepository.refresh(force = false)
            refreshBackupInfo()
            _needsFolderAccess.value = repository.needsBackupFolderAccess()
        }
    }

    val assets: StateFlow<List<AssetEntity>> = repository.assets
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

    val exchangeRateState: StateFlow<ExchangeRateState> = exchangeRateRepository.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExchangeRateState())

    fun refreshExchangeRate() {
        viewModelScope.launch {
            exchangeRateRepository.refresh(force = true)
        }
    }

    fun refreshBackupInfo() {
        viewModelScope.launch {
            _backupInfo.value = repository.getBackupInfo()
            _needsFolderAccess.value = repository.needsBackupFolderAccess()
        }
    }

    fun createBackup() {
        viewModelScope.launch {
            _isBackupBusy.value = true
            when (val result = repository.createBackup()) {
                is BackupActionResult.Success -> {
                    _backupMessage.value = result.message
                    refreshBackupInfo()
                }
                is BackupActionResult.NeedsFolderAccess -> {
                    _needsFolderAccess.value = true
                    _backupMessage.value = result.message
                }
                is BackupActionResult.Error -> {
                    _backupMessage.value = result.message
                }
            }
            _isBackupBusy.value = false
        }
    }

    fun restoreLatestBackup() {
        viewModelScope.launch {
            _isBackupBusy.value = true
            when (val result = repository.restoreLatestBackup()) {
                is BackupActionResult.Success -> {
                    _backupMessage.value = result.message
                    refreshBackupInfo()
                }
                is BackupActionResult.NeedsFolderAccess -> {
                    _needsFolderAccess.value = true
                    _backupMessage.value = result.message
                }
                is BackupActionResult.Error -> {
                    _backupMessage.value = result.message
                }
            }
            _isBackupBusy.value = false
        }
    }

    fun configureBackupFolder(treeUri: Uri) {
        viewModelScope.launch {
            repository.configureBackupFolder(treeUri)
            _needsFolderAccess.value = false
            refreshBackupInfo()
            createBackup()
        }
    }

    fun clearBackupMessage() {
        _backupMessage.value = null
    }

    fun deleteAsset(asset: AssetEntity) {
        viewModelScope.launch {
            repository.deleteAsset(asset)
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
