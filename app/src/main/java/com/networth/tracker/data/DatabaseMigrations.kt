package com.networth.tracker.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE assets ADD COLUMN investedAmount REAL NOT NULL DEFAULT 0")
        db.execSQL("UPDATE assets SET investedAmount = amount")
    }
}
