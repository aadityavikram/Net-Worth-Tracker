package com.networth.tracker.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE assets ADD COLUMN investedAmount REAL NOT NULL DEFAULT 0")
        db.execSQL("UPDATE assets SET investedAmount = amount")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS bank_accounts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                bankName TEXT NOT NULL,
                accountName TEXT NOT NULL,
                accountNumber TEXT NOT NULL,
                accountType TEXT NOT NULL,
                balance REAL NOT NULL,
                currency TEXT NOT NULL,
                notes TEXT NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE assets ADD COLUMN interestRate REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE assets ADD COLUMN dateTakenMillis INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE bank_accounts ADD COLUMN creditLimit REAL NOT NULL DEFAULT 0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS net_worth_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                recordedAt INTEGER NOT NULL,
                totalAssetsInInr REAL NOT NULL,
                totalLiabilitiesInInr REAL NOT NULL
            )
            """.trimIndent()
        )
    }
}
