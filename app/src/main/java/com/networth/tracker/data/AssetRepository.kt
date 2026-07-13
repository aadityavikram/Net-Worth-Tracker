package com.networth.tracker.data

import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar

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
    private val database: AppDatabase,
    private val assetDao: AssetDao,
    private val bankAccountDao: BankAccountDao,
    private val historyDao: NetWorthHistoryDao,
    private val transactionDao: AssetTransactionDao,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val backupStore: PortfolioBackupStore,
    private val driveBackupStore: GoogleDriveBackupStore
) {
    val assets: Flow<List<AssetEntity>> = assetDao.getAllAssets()
    val bankAccounts: Flow<List<BankAccountEntity>> = bankAccountDao.getAllBankAccounts()
    val transactions: Flow<List<AssetTransactionEntity>> = transactionDao.getAll()

    val netWorthSummary: Flow<NetWorthSummary> = combine(
        assets,
        bankAccounts,
        exchangeRateRepository.state
    ) { assetList, bankList, rateState ->
        buildSummary(assetList, bankList, rateState.rate)
    }

    val netWorthHistory: Flow<List<NetWorthHistoryEntity>> = combine(
        assets,
        bankAccounts,
        transactions,
        exchangeRateRepository.state
    ) { assetList, bankList, txList, rateState ->
        buildHistoryFromTransactions(assetList, bankList, txList, rateState.rate)
    }

    fun transactionsForAsset(assetId: Long): Flow<List<AssetTransactionEntity>> =
        transactionDao.getForAsset(assetId)

    suspend fun getAsset(id: Long): AssetEntity? = assetDao.getAssetById(id)

    suspend fun getTransaction(id: Long): AssetTransactionEntity? = transactionDao.getById(id)

    suspend fun saveAsset(asset: AssetEntity, seedOpeningTransaction: Boolean = true): Long {
        return database.withTransaction {
            val isNew = asset.id == 0L
            val id = if (isNew) {
                assetDao.insert(asset)
            } else {
                assetDao.update(asset.copy(updatedAt = System.currentTimeMillis()))
                asset.id
            }
            if (isNew && seedOpeningTransaction) {
                seedTransactionsForNewAsset(id, asset)
                syncAssetTotalsFromTransactions(id)
            }
            id
        }
    }

    suspend fun saveTransaction(transaction: AssetTransactionEntity): Long {
        return database.withTransaction {
            val id = if (transaction.id == 0L) {
                transactionDao.insert(transaction)
            } else {
                transactionDao.update(transaction.copy(updatedAt = System.currentTimeMillis()))
                transaction.id
            }
            syncAssetTotalsFromTransactions(transaction.assetId)
            id
        }
    }

    suspend fun deleteTransaction(transaction: AssetTransactionEntity) {
        database.withTransaction {
            transactionDao.delete(transaction)
            syncAssetTotalsFromTransactions(transaction.assetId)
        }
    }

    suspend fun deleteAsset(asset: AssetEntity) {
        database.withTransaction {
            transactionDao.deleteForAsset(asset.id)
            assetDao.delete(asset)
        }
    }

    suspend fun deleteAssets(assets: List<AssetEntity>) {
        if (assets.isEmpty()) return
        database.withTransaction {
            transactionDao.deleteForAssets(assets.map { it.id })
            assetDao.delete(assets)
        }
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

        return restoreSnapshot(snapshot, backupStore.getBackupInfo().latestFileName ?: "backup")
    }

    fun getDriveAccountInfo(): DriveAccountInfo {
        return DriveAccountInfo(
            email = driveBackupStore.connectedAccountEmail,
            isConnected = driveBackupStore.isConnected
        )
    }

    fun setDriveAccountEmail(email: String?) {
        driveBackupStore.setConnectedAccount(email)
    }

    fun disconnectDriveAccount() {
        driveBackupStore.disconnect()
    }

    suspend fun getDriveBackupInfo(accessToken: String): BackupInfo =
        driveBackupStore.getBackupInfo(accessToken)

    suspend fun createDriveBackup(accessToken: String): BackupActionResult {
        val snapshot = getPortfolioSnapshot()
        if (snapshot.assets.isEmpty() && snapshot.bankAccounts.isEmpty()) {
            return BackupActionResult.Error("Add at least one asset or bank account before backing up")
        }
        return driveBackupStore.createBackup(accessToken, snapshot)
    }

    suspend fun restoreLatestDriveBackup(accessToken: String): BackupActionResult {
        val snapshot = driveBackupStore.loadLatestSnapshot(accessToken)
            ?: return BackupActionResult.Error("No backup found in Google Drive")

        val latestName = driveBackupStore.getBackupInfo(accessToken).latestFileName ?: "Google Drive backup"
        return restoreSnapshot(snapshot, latestName)
    }

    suspend fun configureBackupFolder(treeUri: Uri) {
        backupStore.setBackupTreeUri(treeUri)
    }

    fun needsBackupFolderAccess(): Boolean = backupStore.needsFolderAccess()

    private suspend fun restoreSnapshot(snapshot: PortfolioSnapshot, sourceLabel: String): BackupActionResult {
        if (snapshot.assets.isEmpty() && snapshot.bankAccounts.isEmpty()) {
            return BackupActionResult.Error("Backup file is empty")
        }

        database.withTransaction {
            transactionDao.deleteAll()
            assetDao.deleteAll()
            bankAccountDao.deleteAll()
            historyDao.deleteAll()
            if (snapshot.assets.isNotEmpty()) assetDao.insertAll(snapshot.assets)
            if (snapshot.bankAccounts.isNotEmpty()) bankAccountDao.insertAll(snapshot.bankAccounts)
            val txs = snapshot.transactions.ifEmpty {
                snapshot.assets.map { asset ->
                    AssetTransactionEntity(
                        assetId = asset.id,
                        title = asset.name,
                        amount = asset.amount,
                        investedAmount = asset.investedAmount,
                        dateMillis = asset.dateTakenMillis.takeIf { it > 0 } ?: asset.updatedAt,
                        notes = asset.notes,
                        updatedAt = asset.updatedAt
                    )
                }
            }
            if (txs.isNotEmpty()) transactionDao.insertAll(txs.map { it.copy(id = 0) })
            val history = snapshot.history.ifEmpty {
                buildHistoryFromTransactions(
                    snapshot.assets,
                    snapshot.bankAccounts,
                    txs,
                    exchangeRateRepository.state.value.rate
                )
            }
            if (history.isNotEmpty()) {
                historyDao.insertAll(history.map { it.copy(id = 0) })
            }
        }

        val totalEntries = snapshot.assets.size + snapshot.bankAccounts.size
        return BackupActionResult.Success(
            message = "Restored $totalEntries entries from $sourceLabel",
            fileName = sourceLabel
        )
    }

    private suspend fun seedTransactionsForNewAsset(assetId: Long, asset: AssetEntity) {
        val now = System.currentTimeMillis()
        val purchaseDate = asset.dateTakenMillis.takeIf { it > 0 } ?: now
        val valuationDate = asset.valuationDateMillis.takeIf { it > 0 } ?: now
        val seeds = buildList {
            if (asset.category == AssetCategory.REAL_ESTATE) {
                if (asset.investedAmount > 0) {
                    add(
                        AssetTransactionEntity(
                            assetId = assetId,
                            title = "Purchase",
                            amount = asset.investedAmount,
                            investedAmount = asset.investedAmount,
                            dateMillis = purchaseDate,
                            notes = asset.notes
                        )
                    )
                }
                add(
                    AssetTransactionEntity(
                        assetId = assetId,
                        title = "Current value",
                        amount = asset.amount,
                        investedAmount = 0.0,
                        dateMillis = valuationDate
                    )
                )
            } else {
                add(
                    AssetTransactionEntity(
                        assetId = assetId,
                        title = if (asset.category.isLiability) "Opening balance" else "Opening",
                        amount = asset.amount,
                        investedAmount = asset.investedAmount,
                        dateMillis = purchaseDate,
                        notes = asset.notes
                    )
                )
            }
        }
        transactionDao.insertAll(seeds)
    }

    private suspend fun syncAssetTotalsFromTransactions(assetId: Long) {
        val asset = assetDao.getAssetById(assetId) ?: return
        val txs = transactionDao.getForAssetOnce(assetId)
        if (txs.isEmpty()) return
        val sorted = txs.sortedBy { it.dateMillis }
        val currentAmount = if (asset.usesValueCheckpoints()) {
            sorted.last().amount
        } else {
            sorted.sumOf { it.amount }
        }
        val invested = sorted.sumOf { it.investedAmount }.takeIf { it > 0 }
            ?: asset.investedAmount
        val latestDate = sorted.maxOf { it.dateMillis }
        assetDao.update(
            asset.copy(
                amount = currentAmount,
                investedAmount = invested,
                valuationDateMillis = latestDate,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun getPortfolioSnapshot(): PortfolioSnapshot {
        val assets = assetDao.getAllAssetsOnce()
        val bankAccounts = bankAccountDao.getAllBankAccountsOnce()
        val transactions = transactionDao.getAllOnce()
        val rate = exchangeRateRepository.state.value.rate
        return PortfolioSnapshot(
            assets = assets,
            bankAccounts = bankAccounts,
            transactions = transactions,
            history = buildHistoryFromTransactions(assets, bankAccounts, transactions, rate)
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

        fun startOfDayMillis(millis: Long): Long {
            return Calendar.getInstance().apply {
                timeInMillis = millis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        fun buildHistoryFromTransactions(
            assets: List<AssetEntity>,
            bankAccounts: List<BankAccountEntity>,
            transactions: List<AssetTransactionEntity>,
            usdToInrRate: Double
        ): List<NetWorthHistoryEntity> {
            data class Addition(val dayMillis: Long, val amountInInr: Double)

            val txsByAsset = transactions.groupBy { it.assetId }
            val additiveAssetPoints = mutableListOf<Addition>()
            val allDays = sortedSetOf<Long>()

            fun assetSeries(asset: AssetEntity): List<Pair<Long, Double>> {
                val txs = txsByAsset[asset.id].orEmpty().sortedBy { it.dateMillis }
                if (txs.isNotEmpty()) {
                    return txs.map {
                        startOfDayMillis(it.dateMillis) to
                            toInr(it.amount, asset.currency, usdToInrRate)
                    }
                }
                val valueDate = asset.valuationDateMillis.takeIf { it > 0 }
                    ?: System.currentTimeMillis()
                return listOf(
                    startOfDayMillis(valueDate) to
                        toInr(asset.amount, asset.currency, usdToInrRate)
                )
            }

            assets.forEach { asset ->
                val series = assetSeries(asset)
                if (asset.usesValueCheckpoints() || asset.category.isLiability) {
                    series.forEach { allDays += it.first }
                } else {
                    series.forEach { (day, amount) ->
                        additiveAssetPoints += Addition(day, amount)
                        allDays += day
                    }
                }
            }

            bankAccounts.forEach { account ->
                val day = startOfDayMillis(
                    account.asOfDateMillis.takeIf { it > 0 } ?: account.updatedAt
                )
                allDays += day
            }

            if (allDays.isEmpty()) return emptyList()

            var runningAdditiveAssets = 0.0
            val additiveByDay = additiveAssetPoints.groupBy { it.dayMillis }

            return allDays.map { day ->
                additiveByDay[day]?.forEach { runningAdditiveAssets += it.amountInInr }

                var checkpointAssets = 0.0
                var liabilities = 0.0

                assets.forEach { asset ->
                    val series = assetSeries(asset)
                    val valueAsOf = series.lastOrNull { it.first <= day }?.second ?: return@forEach
                    when {
                        asset.category.isLiability -> liabilities += valueAsOf
                        asset.usesValueCheckpoints() -> checkpointAssets += valueAsOf
                    }
                }

                bankAccounts.forEach { account ->
                    val dayMs = startOfDayMillis(
                        account.asOfDateMillis.takeIf { it > 0 } ?: account.updatedAt
                    )
                    if (dayMs > day) return@forEach
                    val amount = toInr(account.balance, account.currency, usdToInrRate)
                    if (account.accountType.isLiability) {
                        liabilities += amount
                    } else {
                        // Bank assets only appear from their as-of day onward (not cumulative adds).
                        // Include as checkpoint-style from that day.
                        checkpointAssets += amount
                    }
                }

                // Bank assets were double-counted if also in additive — keep banks as checkpoint from as-of date only.
                // Remove bank assets from additive path entirely (already done above).

                NetWorthHistoryEntity(
                    recordedAt = day,
                    totalAssetsInInr = runningAdditiveAssets + checkpointAssets,
                    totalLiabilitiesInInr = liabilities
                )
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
