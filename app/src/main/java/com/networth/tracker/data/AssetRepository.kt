package com.networth.tracker.data

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class CategorySummary(
    val category: AssetCategory,
    val totalInInr: Double,
    val entryCount: Int
)

data class NetWorthSummary(
    val totalAssetsInInr: Double,
    val totalLiabilitiesInInr: Double,
    val netWorthInInr: Double,
    val categorySummaries: List<CategorySummary>,
    val portfolioReturn: ReturnMetrics
)

class AssetRepository(
    private val dao: AssetDao,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val backupStore: PortfolioBackupStore
) {
    val assets: Flow<List<AssetEntity>> = dao.getAllAssets()

    val netWorthSummary: Flow<NetWorthSummary> = combine(
        assets,
        exchangeRateRepository.state
    ) { assetList, rateState ->
        buildSummary(assetList, rateState.rate)
    }

    suspend fun getAsset(id: Long): AssetEntity? = dao.getAssetById(id)

    suspend fun saveAsset(asset: AssetEntity): Long {
        return if (asset.id == 0L) {
            dao.insert(asset)
        } else {
            dao.update(asset.copy(updatedAt = System.currentTimeMillis()))
            asset.id
        }
    }

    suspend fun deleteAsset(asset: AssetEntity) = dao.delete(asset)

    suspend fun getBackupInfo(): BackupInfo = backupStore.getBackupInfo()

    suspend fun createBackup(): BackupActionResult {
        val assets = dao.getAllAssetsOnce()
        if (assets.isEmpty()) {
            return BackupActionResult.Error("Add at least one entry before backing up")
        }
        return backupStore.createBackup(assets)
    }

    suspend fun restoreLatestBackup(): BackupActionResult {
        val assets = backupStore.loadLatestAssets()
            ?: return BackupActionResult.Error("No backup found in Documents/NetWorthTracker")

        if (assets.isEmpty()) {
            return BackupActionResult.Error("Latest backup file is empty")
        }

        dao.deleteAll()
        dao.insertAll(assets)

        val latest = backupStore.getBackupInfo()
        return BackupActionResult.Success(
            message = "Restored ${assets.size} entries from ${latest.latestFileName ?: "backup"}",
            fileName = latest.latestFileName
        )
    }

    suspend fun configureBackupFolder(treeUri: Uri) {
        backupStore.setBackupTreeUri(treeUri)
    }

    fun needsBackupFolderAccess(): Boolean = backupStore.needsFolderAccess()

    companion object {
        const val DEFAULT_USD_TO_INR = 84.0

        fun toInr(amount: Double, currency: Currency, usdToInrRate: Double): Double {
            return when (currency) {
                Currency.INR -> amount
                Currency.USD -> amount * usdToInrRate
            }
        }

        fun buildSummary(assets: List<AssetEntity>, usdToInrRate: Double): NetWorthSummary {
            val categoryTotals = AssetCategory.entries.associateWith { category ->
                val entries = assets.filter { it.category == category }
                val totalInInr = entries.sumOf { toInr(it.amount, it.currency, usdToInrRate) }
                CategorySummary(category, totalInInr, entries.size)
            }

            val totalAssets = categoryTotals.values
                .filter { !it.category.isLiability }
                .sumOf { it.totalInInr }

            val totalLiabilities = categoryTotals.values
                .filter { it.category.isLiability }
                .sumOf { it.totalInInr }

            return NetWorthSummary(
                totalAssetsInInr = totalAssets,
                totalLiabilitiesInInr = totalLiabilities,
                netWorthInInr = totalAssets - totalLiabilities,
                categorySummaries = categoryTotals.values
                    .filter { it.entryCount > 0 || !it.category.isLiability }
                    .sortedBy { it.category.ordinal },
                portfolioReturn = ReturnCalculator.forAssets(assets, usdToInrRate)
            )
        }
    }
}
