package com.networth.tracker.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class GoogleDriveBackupStore(
    private val backupPreferences: BackupPreferences,
    private val localStore: PortfolioBackupStore
) {

    val connectedAccountEmail: String?
        get() = backupPreferences.driveAccountEmail

    val isConnected: Boolean
        get() = backupPreferences.isDriveConnected

    fun setConnectedAccount(email: String?) {
        backupPreferences.driveAccountEmail = email
        backupPreferences.isDriveConnected = true
    }

    fun disconnect() {
        backupPreferences.driveAccountEmail = null
        backupPreferences.isDriveConnected = false
        backupPreferences.driveFolderId = null
    }

    suspend fun getBackupInfo(accessToken: String): BackupInfo = withContext(Dispatchers.IO) {
        val files = listBackupFiles(accessToken)
        val latest = files.firstOrNull()
        BackupInfo(
            latestFileName = latest?.name,
            latestSavedAt = latest?.modifiedAtMillis,
            backupCount = files.size,
            folderPath = "Google Drive / My Drive / ${PortfolioBackupStore.BACKUP_FOLDER}"
        )
    }

    suspend fun createBackup(accessToken: String, snapshot: PortfolioSnapshot): BackupActionResult =
        withContext(Dispatchers.IO) {
            try {
                val fileName = localStore.buildBackupFileName()
                val json = localStore.serialize(snapshot, fileName)
                uploadBackup(accessToken, fileName, json)
                BackupActionResult.Success(
                    "Backup uploaded to My Drive/${PortfolioBackupStore.BACKUP_FOLDER}/$fileName",
                    fileName
                )
            } catch (e: Exception) {
                BackupActionResult.Error(e.message ?: "Could not upload backup to Google Drive")
            }
        }

    suspend fun loadLatestSnapshot(accessToken: String): PortfolioSnapshot? = withContext(Dispatchers.IO) {
        val latest = listBackupFiles(accessToken).firstOrNull() ?: return@withContext null
        val json = downloadFile(accessToken, latest.id) ?: return@withContext null
        localStore.deserialize(json)
    }

    private data class DriveFileRef(
        val id: String,
        val name: String,
        val modifiedAtMillis: Long,
        val sortKey: String
    )

    private fun ensureBackupFolderId(accessToken: String): String {
        backupPreferences.driveFolderId?.let { cached ->
            if (folderExists(accessToken, cached)) return cached
        }

        findBackupFolderId(accessToken)?.let { existing ->
            backupPreferences.driveFolderId = existing
            return existing
        }

        val created = createBackupFolder(accessToken)
        backupPreferences.driveFolderId = created
        return created
    }

    private fun folderExists(accessToken: String, folderId: String): Boolean {
        return try {
            val fields = URLEncoder.encode("id,trashed", Charsets.UTF_8.name())
            val response = httpGet("$DRIVE_FILES/$folderId?fields=$fields", accessToken)
            val json = JSONObject(response)
            !json.optBoolean("trashed", false)
        } catch (_: Exception) {
            false
        }
    }

    private fun findBackupFolderId(accessToken: String): String? {
        val query = URLEncoder.encode(
            "name = '${PortfolioBackupStore.BACKUP_FOLDER}' and " +
                "mimeType = 'application/vnd.google-apps.folder' and trashed = false",
            Charsets.UTF_8.name()
        )
        val fields = URLEncoder.encode("files(id,name)", Charsets.UTF_8.name())
        val url = "$DRIVE_FILES?q=$query&spaces=drive&pageSize=10&fields=$fields"
        val response = httpGet(url, accessToken)
        val files = JSONObject(response).optJSONArray("files") ?: return null
        if (files.length() == 0) return null
        return files.getJSONObject(0).getString("id")
    }

    private fun createBackupFolder(accessToken: String): String {
        val metadata = JSONObject()
            .put("name", PortfolioBackupStore.BACKUP_FOLDER)
            .put("mimeType", FOLDER_MIME)
            .toString()

        val connection = (URL(DRIVE_FILES).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }

        try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(metadata)
            }
            val code = connection.responseCode
            val response = readBody(connection)
            if (code !in 200..299) {
                throw IllegalStateException(parseDriveError(response) ?: "Could not create Drive folder ($code)")
            }
            return JSONObject(response).getString("id")
        } finally {
            connection.disconnect()
        }
    }

    private fun listBackupFiles(accessToken: String): List<DriveFileRef> {
        val folderId = ensureBackupFolderId(accessToken)
        val query = URLEncoder.encode(
            "'$folderId' in parents and " +
                "name contains '${PortfolioBackupStore.BACKUP_FILE_PREFIX}' and trashed = false",
            Charsets.UTF_8.name()
        )
        val fields = URLEncoder.encode("files(id,name,modifiedTime)", Charsets.UTF_8.name())
        val url =
            "$DRIVE_FILES?q=$query&spaces=drive&orderBy=modifiedTime desc&pageSize=50&fields=$fields"
        val response = httpGet(url, accessToken)
        val filesArray = JSONObject(response).optJSONArray("files") ?: return emptyList()
        val results = mutableListOf<DriveFileRef>()
        for (index in 0 until filesArray.length()) {
            val item = filesArray.getJSONObject(index)
            val name = item.getString("name")
            if (!name.startsWith(PortfolioBackupStore.BACKUP_FILE_PREFIX) ||
                !name.endsWith(PortfolioBackupStore.BACKUP_FILE_SUFFIX)
            ) {
                continue
            }
            results.add(
                DriveFileRef(
                    id = item.getString("id"),
                    name = name,
                    modifiedAtMillis = parseDriveTime(item.optString("modifiedTime")),
                    sortKey = name
                        .removePrefix(PortfolioBackupStore.BACKUP_FILE_PREFIX)
                        .removeSuffix(PortfolioBackupStore.BACKUP_FILE_SUFFIX)
                )
            )
        }
        return results.sortedByDescending { it.sortKey }
    }

    private fun uploadBackup(accessToken: String, fileName: String, json: String) {
        val folderId = ensureBackupFolderId(accessToken)
        val boundary = "networth_${System.currentTimeMillis()}"
        val metadata = JSONObject()
            .put("name", fileName)
            .put("parents", JSONArray().put(folderId))
            .put("mimeType", "application/json")
            .toString()

        val body = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadata)
            append("\r\n--$boundary\r\n")
            append("Content-Type: application/json\r\n\r\n")
            append(json)
            append("\r\n--$boundary--\r\n")
        }

        val connection = (URL(DRIVE_UPLOAD).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        }

        try {
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body)
            }
            val code = connection.responseCode
            val response = readBody(connection)
            if (code !in 200..299) {
                throw IllegalStateException(parseDriveError(response) ?: "Drive upload failed ($code)")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadFile(accessToken: String, fileId: String): String? {
        val url = "$DRIVE_FILES/$fileId?alt=media"
        return try {
            httpGet(url, accessToken)
        } catch (_: Exception) {
            null
        }
    }

    private fun httpGet(url: String, accessToken: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $accessToken")
        }
        try {
            val code = connection.responseCode
            val body = readBody(connection)
            if (code !in 200..299) {
                throw IllegalStateException(parseDriveError(body) ?: "Drive request failed ($code)")
            }
            return body
        } finally {
            connection.disconnect()
        }
    }

    private fun readBody(connection: HttpURLConnection): String {
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: connection.inputStream
        }
        return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    }

    private fun parseDriveError(body: String): String? {
        return try {
            JSONObject(body).optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseDriveTime(value: String): Long {
        if (value.isBlank()) return 0L
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            format.parse(value)?.time ?: 0L
        } catch (_: Exception) {
            try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                format.parse(value)?.time ?: 0L
            } catch (_: Exception) {
                0L
            }
        }
    }

    companion object {
        private const val DRIVE_FILES = "https://www.googleapis.com/drive/v3/files"
        private const val DRIVE_UPLOAD =
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
        private const val FOLDER_MIME = "application/vnd.google-apps.folder"
    }
}
