package com.networth.tracker.data

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class CategorySummary(
    val category: AssetCategory,
    val totalInInr: Double,
    val entryCount: Int
)

data class BankAccountTypeSummary(
    val accountType: BankAccountType,
    val totalInInr: Double,
    val entryCount: Int
)

data class NetWorthSummary(
    val totalAssetsInInr: Double,
    val totalLiabilitiesInInr: Double,
    val netWorthInInr: Double,
    val categorySummaries: List<CategorySummary>,
    val portfolioReturn: ReturnMetrics,
    val totalBankAssetsInInr: Double = 0.0,
    val totalBankLiabilitiesInInr: Double = 0.0,
    val outstandingSummaries: List<BankAccountTypeSummary> = emptyList()
)

class AssetRepository(
    private val assetDao: AssetDao,
    private val bankAccountDao: BankAccountDao,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val backupStore: PortfolioBackupStore
) {
    val assets: Flow<List<AssetEntity>> = assetDao.getAllAssets()
    val bankAccounts: Flow<List<BankAccountEntity>> = bankAccountDao.getAllBankAccounts()

    val netWorthSummary: Flow<NetWorthSummary> = combine(
        assets,
        bankAccounts,
        exchangeRateRepository.state
    ) { assetList, bankList, rateState ->
        buildSummary(assetList, bankList, rateState.rate)
    }

    suspend fun getAsset(id: Long): AssetEntity? = assetDao.getAssetById(id)

    suspend fun saveAsset(asset: AssetEntity): Long {
        return if (asset.id == 0L) {
            assetDao.insert(asset)
        } else {
            assetDao.update(asset.copy(updatedAt = System.currentTimeMillis()))
            asset.id
        }
    }

    suspend fun deleteAsset(asset: AssetEntity) = assetDao.delete(asset)

    suspend fun deleteAssets(assets: List<AssetEntity>) {
        if (assets.isNotEmpty()) assetDao.delete(assets)
    }

    suspend fun getBankAccount(id: Long): BankAccountEntity? = bankAccountDao.getBankAccountById(id)

    suspend fun saveBankAccount(bankAccount: BankAccountEntity): Long {
        return if (bankAccount.id == 0L) {
            bankAccountDao.insert(bankAccount)
        } else {
            bankAccountDao.update(bankAccount.copy(updatedAt = System.currentTimeMillis()))
            bankAccount.id
        }
    }

    suspend fun deleteBankAccount(bankAccount: BankAccountEntity) = bankAccountDao.delete(bankAccount)

    suspend fun deleteBankAccounts(bankAccounts: List<BankAccountEntity>) {
        if (bankAccounts.isNotEmpty()) bankAccountDao.delete(bankAccounts)
    }

    suspend fun getBackupInfo(): BackupInfo = backupStore.getBackupInfo()

    suspend fun createBackup(): BackupActionResult {
        val snapshot = getPortfolioSnapshot()
        if (snapshot.assets.isEmpty() && snapshot.bankAccounts.isEmpty()) {
            return BackupActionResult.Error("Add at least one asset or bank account before backing up")
        }
        return backupStore.createBackup(snapshot)
    }

    suspend fun restoreLatestBackup(): BackupActionResult {
        val snapshot = backupStore.loadLatestSnapshot()
            ?: return BackupActionResult.Error("No backup found in Documents/NetWorthTracker")

        if (snapshot.assets.isEmpty() && snapshot.bankAccounts.isEmpty()) {
            return BackupActionResult.Error("Latest backup file is empty")
        }

        assetDao.deleteAll()
        bankAccountDao.deleteAll()
        if (snapshot.assets.isNotEmpty()) assetDao.insertAll(snapshot.assets)
        if (snapshot.bankAccounts.isNotEmpty()) bankAccountDao.insertAll(snapshot.bankAccounts)

        val totalEntries = snapshot.assets.size + snapshot.bankAccounts.size
        val latest = backupStore.getBackupInfo()
        return BackupActionResult.Success(
            message = "Restored $totalEntries entries from ${latest.latestFileName ?: "backup"}",
            fileName = latest.latestFileName
        )
    }

    suspend fun configureBackupFolder(treeUri: Uri) {
        backupStore.setBackupTreeUri(treeUri)
    }

    fun needsBackupFolderAccess(): Boolean = backupStore.needsFolderAccess()

    private suspend fun getPortfolioSnapshot(): PortfolioSnapshot {
        return PortfolioSnapshot(
            assets = assetDao.getAllAssetsOnce(),
            bankAccounts = bankAccountDao.getAllBankAccountsOnce()
        )
    }

    companion object {
        const val DEFAULT_USD_TO_INR = 84.0

        fun toInr(amount: Double, currency: Currency, usdToInrRate: Double): Double {
            return when (currency) {
                Currency.INR -> amount
                Currency.USD -> amount * usdToInrRate
            }
        }

        fun buildSummary(
            assets: List<AssetEntity>,
            bankAccounts: List<BankAccountEntity>,
            usdToInrRate: Double
        ): NetWorthSummary {
            val categoryTotals = AssetCategory.entries.associateWith { category ->
                val entries = assets.filter { it.category == category }
                val totalInInr = entries.sumOf { toInr(it.amount, it.currency, usdToInrRate) }
                CategorySummary(category, totalInInr, entries.size)
            }

            var totalAssets = categoryTotals.values
                .filter { !it.category.isLiability }
                .sumOf { it.totalInInr }

            var totalLiabilities = categoryTotals.values
                .filter { it.category.isLiability }
                .sumOf { it.totalInInr }

            val bankAssets = bankAccounts
                .filter { !it.accountType.isLiability }
                .sumOf { toInr(it.balance, it.currency, usdToInrRate) }

            val bankLiabilities = bankAccounts
                .filter { it.accountType.isLiability }
                .sumOf { toInr(it.balance, it.currency, usdToInrRate) }

            val outstandingSummaries = BankAccountType.liabilities.mapNotNull { type ->
                val entries = bankAccounts.filter { it.accountType == type }
                if (entries.isEmpty()) {
                    null
                } else {
                    BankAccountTypeSummary(
                        accountType = type,
                        totalInInr = entries.sumOf { toInr(it.balance, it.currency, usdToInrRate) },
                        entryCount = entries.size
                    )
                }
            }

            totalAssets += bankAssets
            totalLiabilities += bankLiabilities

            return NetWorthSummary(
                totalAssetsInInr = totalAssets,
                totalLiabilitiesInInr = totalLiabilities,
                netWorthInInr = totalAssets - totalLiabilities,
                categorySummaries = categoryTotals.values
                    .filter { it.entryCount > 0 || !it.category.isLiability }
                    .sortedBy { it.category.ordinal },
                portfolioReturn = ReturnCalculator.forAssets(assets, usdToInrRate),
                totalBankAssetsInInr = bankAssets,
                totalBankLiabilitiesInInr = bankLiabilities,
                outstandingSummaries = outstandingSummaries
            )
        }
    }
}
