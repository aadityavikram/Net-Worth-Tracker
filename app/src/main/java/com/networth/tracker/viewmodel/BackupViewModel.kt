package com.networth.tracker.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.networth.tracker.data.AssetRepository
import com.networth.tracker.data.BackupActionResult
import com.networth.tracker.data.BackupInfo
import com.networth.tracker.data.DriveAccountInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BackupViewModel(
    private val repository: AssetRepository
) : ViewModel() {

    private val _backupInfo = MutableStateFlow(BackupInfo())
    val backupInfo: StateFlow<BackupInfo> = _backupInfo.asStateFlow()

    private val _driveBackupInfo = MutableStateFlow(
        BackupInfo(folderPath = "Google Drive / My Drive / NetWorthTracker")
    )
    val driveBackupInfo: StateFlow<BackupInfo> = _driveBackupInfo.asStateFlow()

    private val _driveAccount = MutableStateFlow(DriveAccountInfo())
    val driveAccount: StateFlow<DriveAccountInfo> = _driveAccount.asStateFlow()

    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    private val _needsFolderAccess = MutableStateFlow(false)
    val needsFolderAccess: StateFlow<Boolean> = _needsFolderAccess.asStateFlow()

    private val _isBackupBusy = MutableStateFlow(false)
    val isBackupBusy: StateFlow<Boolean> = _isBackupBusy.asStateFlow()

    private val _isDriveBusy = MutableStateFlow(false)
    val isDriveBusy: StateFlow<Boolean> = _isDriveBusy.asStateFlow()

    init {
        viewModelScope.launch {
            refreshBackupInfo()
            refreshDriveAccount()
            _needsFolderAccess.value = repository.needsBackupFolderAccess()
        }
    }

    fun refreshBackupInfo() {
        viewModelScope.launch {
            _backupInfo.value = repository.getBackupInfo()
            _needsFolderAccess.value = repository.needsBackupFolderAccess()
        }
    }

    fun refreshDriveAccount() {
        _driveAccount.value = repository.getDriveAccountInfo()
    }

    fun onDriveConnected(email: String?, accessToken: String?) {
        repository.setDriveAccountEmail(email)
        refreshDriveAccount()
        if (!accessToken.isNullOrBlank()) {
            refreshDriveBackupInfo(accessToken)
        }
    }

    fun disconnectDrive() {
        repository.disconnectDriveAccount()
        _driveBackupInfo.value = BackupInfo(folderPath = "Google Drive / My Drive / NetWorthTracker")
        refreshDriveAccount()
        _backupMessage.value = "Disconnected from Google Drive"
    }

    fun refreshDriveBackupInfo(accessToken: String) {
        viewModelScope.launch {
            try {
                _driveBackupInfo.value = repository.getDriveBackupInfo(accessToken)
            } catch (e: Exception) {
                _backupMessage.value = e.message ?: "Could not load Google Drive backups"
            }
        }
    }

    fun createBackup() {
        viewModelScope.launch {
            _isBackupBusy.value = true
            handleLocalResult(repository.createBackup())
            _isBackupBusy.value = false
        }
    }

    fun restoreLatestBackup() {
        viewModelScope.launch {
            _isBackupBusy.value = true
            handleLocalResult(repository.restoreLatestBackup())
            _isBackupBusy.value = false
        }
    }

    fun createDriveBackup(accessToken: String) {
        viewModelScope.launch {
            _isDriveBusy.value = true
            when (val result = repository.createDriveBackup(accessToken)) {
                is BackupActionResult.Success -> {
                    _backupMessage.value = result.message
                    refreshDriveBackupInfo(accessToken)
                }
                is BackupActionResult.Error -> _backupMessage.value = result.message
                is BackupActionResult.NeedsFolderAccess -> _backupMessage.value = result.message
                is BackupActionResult.NeedsGoogleSignIn -> _backupMessage.value = result.message
            }
            _isDriveBusy.value = false
        }
    }

    fun restoreLatestDriveBackup(accessToken: String) {
        viewModelScope.launch {
            _isDriveBusy.value = true
            when (val result = repository.restoreLatestDriveBackup(accessToken)) {
                is BackupActionResult.Success -> {
                    _backupMessage.value = result.message
                    refreshDriveBackupInfo(accessToken)
                    refreshBackupInfo()
                }
                is BackupActionResult.Error -> _backupMessage.value = result.message
                is BackupActionResult.NeedsFolderAccess -> _backupMessage.value = result.message
                is BackupActionResult.NeedsGoogleSignIn -> _backupMessage.value = result.message
            }
            _isDriveBusy.value = false
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

    private fun handleLocalResult(result: BackupActionResult) {
        when (result) {
            is BackupActionResult.Success -> {
                _backupMessage.value = result.message
                refreshBackupInfo()
            }
            is BackupActionResult.NeedsFolderAccess -> {
                _needsFolderAccess.value = true
                _backupMessage.value = result.message
            }
            is BackupActionResult.Error -> _backupMessage.value = result.message
            is BackupActionResult.NeedsGoogleSignIn -> _backupMessage.value = result.message
        }
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
