package com.networth.tracker.data

data class PortfolioSnapshot(
    val assets: List<AssetEntity>,
    val bankAccounts: List<BankAccountEntity>,
    val history: List<NetWorthHistoryEntity> = emptyList()
)
