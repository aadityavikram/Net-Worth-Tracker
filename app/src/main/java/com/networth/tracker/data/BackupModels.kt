package com.networth.tracker.data

data class BackupInfo(
    val latestFileName: String? = null,
    val latestSavedAt: Long? = null,
    val backupCount: Int = 0,
    val folderPath: String = "Documents/${PortfolioBackupStore.BACKUP_FOLDER}"
)

sealed class BackupActionResult {
    data class Success(val message: String, val fileName: String? = null) : BackupActionResult()
    data class Error(val message: String) : BackupActionResult()
    data class NeedsFolderAccess(val message: String) : BackupActionResult()
}
