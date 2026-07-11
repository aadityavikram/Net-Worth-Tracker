package com.networth.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bank_accounts")
data class BankAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bankName: String,
    val accountName: String,
    val accountNumber: String = "",
    val accountType: BankAccountType,
    val balance: Double,
    val currency: Currency = Currency.INR,
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
