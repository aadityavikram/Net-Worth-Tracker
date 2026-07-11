package com.networth.tracker.ui.navigation

object Routes {
    const val DASHBOARD = "dashboard"
    const val ADD_EDIT = "add_edit/{assetId}"

    fun addEdit(assetId: Long = -1) = "add_edit/$assetId"
}
