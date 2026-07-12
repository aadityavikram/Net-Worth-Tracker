package com.networth.tracker.ui.navigation

object Routes {
    const val HOME = "home"
    const val BACKUP = "backup"
    const val ADD_EDIT = "add_edit/{assetId}?context={context}"
    const val ADD_EDIT_BANK = "add_edit_bank/{bankAccountId}?context={context}"

    fun addEdit(
        assetId: Long = -1,
        context: String? = null
    ): String {
        val route = "add_edit/$assetId"
        return if (context != null) "$route?context=$context" else route
    }
    fun addEditBank(
        bankAccountId: Long = -1,
        context: String? = null
    ): String {
        val route = "add_edit_bank/$bankAccountId"
        return if (context != null) "$route?context=$context" else route
    }
}
