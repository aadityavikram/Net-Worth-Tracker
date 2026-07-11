package com.networth.tracker.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.networth.tracker.data.AssetRepository
import com.networth.tracker.data.BackupActionResult
import com.networth.tracker.data.BackupInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BackupViewModel(
    private val repository: AssetRepository
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
            refreshBackupInfo()
            _needsFolderAccess.value = repository.needsBackupFolderAccess()
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
}

class BackupViewModelFactory(
    private val repository: AssetRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BackupViewModel::class.java)) {
            return BackupViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
