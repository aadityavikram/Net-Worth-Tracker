package com.networth.tracker.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PortfolioBackupStore(
    private val context: Context,
    private val backupPreferences: BackupPreferences
) {

    fun setBackupTreeUri(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        backupPreferences.backupTreeUri = uri
    }

    fun hasBackupFolderConfigured(): Boolean = backupPreferences.backupTreeUri != null

    fun needsFolderAccess(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasBackupFolderConfigured()

    fun createBackup(snapshot: PortfolioSnapshot): BackupActionResult {
        if (needsFolderAccess()) {
            return BackupActionResult.NeedsFolderAccess(
                "Select the Documents folder once to save backups that survive uninstall."
            )
        }

        val fileName = buildBackupFileName()
        val json = serialize(snapshot, fileName)

        if (saveToSafTree(fileName, json)) {
            return BackupActionResult.Success("Backup saved to $fileName", fileName)
        }

        if (saveLegacyFile(fileName, json)) {
            return BackupActionResult.Success("Backup saved to $fileName", fileName)
        }

        return BackupActionResult.Error("Could not write backup. Grant Documents folder access and try again.")
    }

    fun loadLatestSnapshot(): PortfolioSnapshot? {
        val backupFile = findLatestBackupFile() ?: return null
        return readBackupFile(backupFile)
    }

    @Deprecated("Use loadLatestSnapshot")
    fun loadLatestAssets(): List<AssetEntity>? = loadLatestSnapshot()?.assets

    fun getBackupInfo(): BackupInfo {
        val files = listAllBackupFiles()
        val latest = files.firstOrNull()
        return BackupInfo(
            latestFileName = latest?.displayName,
            latestSavedAt = latest?.savedAt,
            backupCount = files.size
        )
    }

    fun suggestedBackupTreeInitialUri(): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        return DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents",
            "primary:${Environment.DIRECTORY_DOCUMENTS}"
        )
    }

    private data class BackupFileRef(
        val displayName: String,
        val uri: Uri?,
        val legacyFile: File?,
        val savedAt: Long,
        val sortKey: String
    )

    private fun buildBackupFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "${BACKUP_FILE_PREFIX}$timestamp$BACKUP_FILE_SUFFIX"
    }

    private fun serialize(snapshot: PortfolioSnapshot, fileName: String): String {
        val savedAt = System.currentTimeMillis()
        val root = JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("fileName", fileName)
            put("savedAt", savedAt)
            put("assets", JSONArray().apply {
                snapshot.assets.forEach { asset ->
                    put(JSONObject().apply {
                        put("id", asset.id)
                        put("name", asset.name)
                        put("category", asset.category.name)
                        put("amount", asset.amount)
                        put("investedAmount", asset.investedAmount)
                        put("currency", asset.currency.name)
                        put("notes", asset.notes)
                        put("interestRate", asset.interestRate)
                        put("dateTakenMillis", asset.dateTakenMillis)
                        put("updatedAt", asset.updatedAt)
                    })
                }
            })
            put("bankAccounts", JSONArray().apply {
                snapshot.bankAccounts.forEach { account ->
                    put(JSONObject().apply {
                        put("id", account.id)
                        put("bankName", account.bankName)
                        put("accountName", account.accountName)
                        put("accountNumber", account.accountNumber)
                        put("accountType", account.accountType.name)
                        put("balance", account.balance)
                        put("currency", account.currency.name)
                        put("notes", account.notes)
                        put("creditLimit", account.creditLimit)
                        put("updatedAt", account.updatedAt)
                    })
                }
            })
            put("history", JSONArray().apply {
                snapshot.history.forEach { entry ->
                    put(JSONObject().apply {
                        put("id", entry.id)
                        put("recordedAt", entry.recordedAt)
                        put("totalAssetsInInr", entry.totalAssetsInInr)
                        put("totalLiabilitiesInInr", entry.totalLiabilitiesInInr)
                    })
                }
            })
        }
        return root.toString(2)
    }

    private fun deserialize(json: String): PortfolioSnapshot? {
        return try {
            val root = JSONObject(json)
            val assetArray = root.getJSONArray("assets")
            val assets = buildList {
                for (index in 0 until assetArray.length()) {
                    val item = assetArray.getJSONObject(index)
                    add(
                        AssetEntity(
                            id = item.getLong("id"),
                            name = item.getString("name"),
                            category = AssetCategory.valueOf(item.getString("category")),
                            amount = item.getDouble("amount"),
                            investedAmount = item.optDouble("investedAmount", item.getDouble("amount")),
                            currency = Currency.valueOf(item.getString("currency")),
                            notes = item.optString("notes", ""),
                            interestRate = item.optDouble("interestRate", 0.0),
                            dateTakenMillis = item.optLong("dateTakenMillis", 0L),
                            updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val bankAccounts = if (root.has("bankAccounts")) {
                val bankArray = root.getJSONArray("bankAccounts")
                buildList {
                    for (index in 0 until bankArray.length()) {
                        val item = bankArray.getJSONObject(index)
                        add(
                            BankAccountEntity(
                                id = item.getLong("id"),
                                bankName = item.getString("bankName"),
                                accountName = item.getString("accountName"),
                                accountNumber = item.optString("accountNumber", ""),
                                accountType = BankAccountType.valueOf(item.getString("accountType")),
                                balance = item.getDouble("balance"),
                                currency = Currency.valueOf(item.optString("currency", Currency.INR.name)),
                                notes = item.optString("notes", ""),
                                creditLimit = item.optDouble("creditLimit", 0.0),
                                updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                            )
                        )
                    }
                }
            } else {
                emptyList()
            }

            val history = if (root.has("history")) {
                val historyArray = root.getJSONArray("history")
                buildList {
                    for (index in 0 until historyArray.length()) {
                        val item = historyArray.getJSONObject(index)
                        add(
                            NetWorthHistoryEntity(
                                id = item.optLong("id", 0L),
                                recordedAt = item.getLong("recordedAt"),
                                totalAssetsInInr = item.getDouble("totalAssetsInInr"),
                                totalLiabilitiesInInr = item.getDouble("totalLiabilitiesInInr")
                            )
                        )
                    }
                }
            } else {
                emptyList()
            }

            PortfolioSnapshot(assets = assets, bankAccounts = bankAccounts, history = history)
        } catch (_: Exception) {
            null
        }
    }

    private fun readBackupFile(ref: BackupFileRef): PortfolioSnapshot? {
        val json = when {
            ref.uri != null -> readJsonFromUri(ref.uri)
            ref.legacyFile != null -> ref.legacyFile.takeIf { it.exists() }?.readText(Charsets.UTF_8)
            else -> null
        } ?: return null
        return deserialize(json)
    }

    private fun readJsonFromUri(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun writeToUri(uri: Uri, json: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(json.toByteArray(Charsets.UTF_8))
            } != null
        } catch (_: Exception) {
            false
        }
    }

    private fun saveToSafTree(fileName: String, json: String): Boolean {
        val treeUri = backupPreferences.backupTreeUri ?: return false
        return try {
            val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return false
            val folder = tree.findFile(BACKUP_FOLDER)
                ?: tree.createDirectory(BACKUP_FOLDER)
                ?: return false

            val target = folder.createFile("application/json", fileName) ?: return false
            writeToUri(target.uri, json)
        } catch (_: Exception) {
            false
        }
    }

    private fun saveLegacyFile(fileName: String, json: String): Boolean {
        return try {
            val dir = legacyBackupDir()
            dir.mkdirs()
            File(dir, fileName).writeText(json, Charsets.UTF_8)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun legacyBackupDir(): File {
        @Suppress("DEPRECATION")
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        return File(documentsDir, BACKUP_FOLDER)
    }

    private fun findLatestBackupFile(): BackupFileRef? =
        listAllBackupFiles().firstOrNull()

    private fun listAllBackupFiles(): List<BackupFileRef> {
        val files = mutableListOf<BackupFileRef>()
        files.addAll(listLegacyBackupFiles())
        files.addAll(listSafBackupFiles())
        files.addAll(listMediaStoreBackupFiles())

        return files
            .distinctBy { it.displayName }
            .sortedByDescending { it.sortKey }
    }

    private fun listLegacyBackupFiles(): List<BackupFileRef> {
        return try {
            val dir = legacyBackupDir()
            if (!dir.exists()) return emptyList()
            dir.listFiles { file ->
                file.isFile && file.name.startsWith(BACKUP_FILE_PREFIX) && file.name.endsWith(BACKUP_FILE_SUFFIX)
            }?.map { file ->
                BackupFileRef(
                    displayName = file.name,
                    uri = null,
                    legacyFile = file,
                    savedAt = file.lastModified(),
                    sortKey = file.name.removePrefix(BACKUP_FILE_PREFIX).removeSuffix(BACKUP_FILE_SUFFIX)
                )
            }.orEmpty()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun listSafBackupFiles(): List<BackupFileRef> {
        val treeUri = backupPreferences.backupTreeUri ?: return emptyList()
        return try {
            val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
            val folder = tree.findFile(BACKUP_FOLDER) ?: return emptyList()
            folder.listFiles()
                .filter { it.isFile && it.name?.startsWith(BACKUP_FILE_PREFIX) == true }
                .mapNotNull { file ->
                    val name = file.name ?: return@mapNotNull null
                    BackupFileRef(
                        displayName = name,
                        uri = file.uri,
                        legacyFile = null,
                        savedAt = file.lastModified(),
                        sortKey = name.removePrefix(BACKUP_FILE_PREFIX).removeSuffix(BACKUP_FILE_SUFFIX)
                    )
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun listMediaStoreBackupFiles(): List<BackupFileRef> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()

        val results = mutableListOf<BackupFileRef>()
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? AND ${MediaStore.Files.FileColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("${BACKUP_FILE_PREFIX}%$BACKUP_FILE_SUFFIX", "%$BACKUP_FOLDER%")

        context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val modified = cursor.getLong(modifiedColumn) * 1000
                val uri = ContentUris.withAppendedId(collection, id)
                results.add(
                    BackupFileRef(
                        displayName = name,
                        uri = uri,
                        legacyFile = null,
                        savedAt = modified,
                        sortKey = name.removePrefix(BACKUP_FILE_PREFIX).removeSuffix(BACKUP_FILE_SUFFIX)
                    )
                )
            }
        }
        return results
    }

    companion object {
        const val BACKUP_FOLDER = "NetWorthTracker"
        private const val BACKUP_FILE_PREFIX = "net_worth_backup_"
        private const val BACKUP_FILE_SUFFIX = ".json"
        private const val BACKUP_VERSION = 4
    }
}
