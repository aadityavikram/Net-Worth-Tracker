package com.networth.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: AssetCategory,
    val amount: Double,
    val investedAmount: Double = 0.0,
    val currency: Currency,
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
