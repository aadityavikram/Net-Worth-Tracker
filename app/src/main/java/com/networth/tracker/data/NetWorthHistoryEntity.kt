package com.networth.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "net_worth_history")
data class NetWorthHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordedAt: Long,
    val totalAssetsInInr: Double,
    val totalLiabilitiesInInr: Double
)
