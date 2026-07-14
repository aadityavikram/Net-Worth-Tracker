package com.networth.tracker.data

import android.content.Context
import android.net.Uri

class BackupPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var backupTreeUri: Uri?
        get() = prefs.getString(KEY_TREE_URI, null)?.let(Uri::parse)
        set(value) {
            prefs.edit().putString(KEY_TREE_URI, value?.toString()).apply()
        }

    var driveAccountEmail: String?
        get() = prefs.getString(KEY_DRIVE_EMAIL, null)
        set(value) {
            prefs.edit().putString(KEY_DRIVE_EMAIL, value).apply()
        }

    var isDriveConnected: Boolean
        get() = prefs.getBoolean(KEY_DRIVE_CONNECTED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_DRIVE_CONNECTED, value).apply()
        }

    var driveFolderId: String?
        get() = prefs.getString(KEY_DRIVE_FOLDER_ID, null)
        set(value) {
            prefs.edit().putString(KEY_DRIVE_FOLDER_ID, value).apply()
        }

    companion object {
        private const val PREFS_NAME = "backup_prefs"
        private const val KEY_TREE_URI = "backup_tree_uri"
        private const val KEY_DRIVE_EMAIL = "drive_account_email"
        private const val KEY_DRIVE_CONNECTED = "drive_connected"
        private const val KEY_DRIVE_FOLDER_ID = "drive_folder_id"
    }
}
