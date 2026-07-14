package com.networth.tracker.ui.navigation

object Routes {
    const val HOME = "home"
    const val BACKUP = "backup"
    const val ASSET_TRANSACTIONS = "asset_transactions/{assetId}"
    const val ADD_EDIT = "add_edit/{assetId}?context={context}"
    const val ADD_EDIT_TRANSACTION = "add_edit_tx/{assetId}/{transactionId}"
    const val ADD_EDIT_BANK = "add_edit_bank/{bankAccountId}?context={context}"

    fun assetTransactions(assetId: Long): String = "asset_transactions/$assetId"

    fun addEdit(
        assetId: Long = -1,
        context: String? = null
    ): String {
        val route = "add_edit/$assetId"
        return if (context != null) "$route?context=$context" else route
    }

    fun addEditTransaction(assetId: Long, transactionId: Long = -1): String =
        "add_edit_tx/$assetId/$transactionId"

    fun addEditBank(
        bankAccountId: Long = -1,
        context: String? = null
    ): String {
        val route = "add_edit_bank/$bankAccountId"
        return if (context != null) "$route?context=$context" else route
    }
}
