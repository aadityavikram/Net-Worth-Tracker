package com.networth.tracker.data

data class PortfolioSnapshot(
    val assets: List<AssetEntity>,
    val bankAccounts: List<BankAccountEntity>
)
