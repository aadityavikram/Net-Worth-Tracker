package com.networth.tracker.ui.navigation

object Routes {
    const val HOME = "home"
    const val BACKUP = "backup"
    const val ADD_EDIT = "add_edit/{assetId}"
    const val ADD_EDIT_BANK = "add_edit_bank/{bankAccountId}"

    fun addEdit(assetId: Long = -1) = "add_edit/$assetId"
    fun addEditBank(bankAccountId: Long = -1) = "add_edit_bank/$bankAccountId"
}
