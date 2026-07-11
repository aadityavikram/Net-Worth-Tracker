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

    companion object {
        private const val PREFS_NAME = "backup_prefs"
        private const val KEY_TREE_URI = "backup_tree_uri"
    }
}
