package com.networth.tracker

import android.app.Application
import com.networth.tracker.data.AppDatabase
import com.networth.tracker.data.AssetRepository
import com.networth.tracker.data.BackupPreferences
import com.networth.tracker.data.ExchangeRateRepository
import com.networth.tracker.data.PortfolioBackupStore

class NetWorthApp : Application() {
    val database by lazy { AppDatabase.getInstance(this) }
    val backupPreferences by lazy { BackupPreferences(this) }
    val backupStore by lazy { PortfolioBackupStore(this, backupPreferences) }
    val exchangeRateRepository by lazy { ExchangeRateRepository(this) }
    val repository by lazy {
        AssetRepository(
            database.assetDao(),
            database.bankAccountDao(),
            exchangeRateRepository,
            backupStore
        )
    }
}
