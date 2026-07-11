package com.networth.tracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.networth.tracker.ui.navigation.Routes
import com.networth.tracker.ui.screens.AddEditAssetScreen
import com.networth.tracker.ui.screens.DashboardScreen
import com.networth.tracker.ui.theme.NetWorthTrackerTheme
import com.networth.tracker.viewmodel.AddEditAssetViewModel
import com.networth.tracker.viewmodel.AddEditAssetViewModelFactory
import com.networth.tracker.viewmodel.DashboardViewModel
import com.networth.tracker.viewmodel.DashboardViewModelFactory

class MainActivity : ComponentActivity() {

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestLegacyStoragePermissionIfNeeded()

        val app = application as NetWorthApp
        val repository = app.repository
        val exchangeRateRepository = app.exchangeRateRepository

        setContent {
            NetWorthTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = Routes.DASHBOARD
                    ) {
                        composable(Routes.DASHBOARD) {
                            val viewModel: DashboardViewModel = viewModel(
                                factory = DashboardViewModelFactory(repository, exchangeRateRepository)
                            )
                            DashboardScreen(
                                viewModel = viewModel,
                                backupStore = app.backupStore,
                                onAddAsset = {
                                    navController.navigate(Routes.addEdit())
                                },
                                onEditAsset = { id ->
                                    navController.navigate(Routes.addEdit(id))
                                }
                            )
                        }

                        composable(
                            route = Routes.ADD_EDIT,
                            arguments = listOf(
                                navArgument("assetId") {
                                    type = NavType.LongType
                                    defaultValue = -1L
                                }
                            )
                        ) { backStackEntry ->
                            val assetId = backStackEntry.arguments?.getLong("assetId")?.takeIf { it > 0 }
                            val viewModel: AddEditAssetViewModel = viewModel(
                                factory = AddEditAssetViewModelFactory(repository, assetId)
                            )
                            AddEditAssetScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestLegacyStoragePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) return

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            val writePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, writePermission) != PackageManager.PERMISSION_GRANTED) {
                storagePermissionLauncher.launch(writePermission)
            }
        }

        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.Q..Build.VERSION_CODES.S_V2) {
            val readPermission = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, readPermission) != PackageManager.PERMISSION_GRANTED) {
                storagePermissionLauncher.launch(readPermission)
            }
        }
    }
}
