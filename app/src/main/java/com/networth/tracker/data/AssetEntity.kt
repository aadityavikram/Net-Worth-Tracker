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
    val interestRate: Double = 0.0,
    val dateTakenMillis: Long = 0L,
    /** When current market/outstanding value was assessed (esp. real estate). */
    val valuationDateMillis: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** Categories where each transaction sets value/outstanding as of that date. */
    fun usesValueCheckpoints(): Boolean =
        category.isLiability || category == AssetCategory.REAL_ESTATE || category == AssetCategory.GOLD
}
