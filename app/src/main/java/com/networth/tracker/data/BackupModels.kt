package com.networth.tracker.data

data class BackupInfo(
    val latestFileName: String? = null,
    val latestSavedAt: Long? = null,
    val backupCount: Int = 0,
    val folderPath: String = "Documents/${PortfolioBackupStore.BACKUP_FOLDER}"
)

data class DriveAccountInfo(
    val email: String? = null,
    val isConnected: Boolean = false
)

sealed class BackupActionResult {
    data class Success(val message: String, val fileName: String? = null) : BackupActionResult()
    data class Error(val message: String) : BackupActionResult()
    data class NeedsFolderAccess(val message: String) : BackupActionResult()
    data class NeedsGoogleSignIn(val message: String) : BackupActionResult()
}
