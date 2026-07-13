package com.networth.tracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.networth.tracker.data.AssetAddContext
import com.networth.tracker.data.BankAccountAddContext
import com.networth.tracker.ui.navigation.Routes
import com.networth.tracker.ui.screens.AddEditAssetScreen
import com.networth.tracker.ui.screens.AddEditBankAccountScreen
import com.networth.tracker.ui.screens.BackupScreen
import com.networth.tracker.ui.screens.DashboardScreen
import com.networth.tracker.ui.screens.PinLockMode
import com.networth.tracker.ui.screens.PinLockScreen
import com.networth.tracker.ui.theme.NetWorthTrackerTheme
import com.networth.tracker.viewmodel.AddEditAssetViewModel
import com.networth.tracker.viewmodel.AddEditAssetViewModelFactory
import com.networth.tracker.viewmodel.AddEditBankAccountViewModel
import com.networth.tracker.viewmodel.AddEditBankAccountViewModelFactory
import com.networth.tracker.viewmodel.BackupViewModel
import com.networth.tracker.viewmodel.BackupViewModelFactory
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
        val pinPreferences = app.pinPreferences

        setContent {
            NetWorthTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var isUnlocked by remember { mutableStateOf(false) }
                    var sessionStarted by remember { mutableStateOf(false) }

                    DisposableEffect(Unit) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_STOP && pinPreferences.isPinSet) {
                                isUnlocked = false
                            }
                        }
                        val processLifecycle = ProcessLifecycleOwner.get().lifecycle
                        processLifecycle.addObserver(observer)
                        onDispose { processLifecycle.removeObserver(observer) }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (sessionStarted) {
                            val navController = rememberNavController()
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route
                            val showBottomBar = currentRoute == Routes.HOME || currentRoute == Routes.BACKUP

                            Scaffold(
                                bottomBar = {
                                    if (showBottomBar) {
                                        NavigationBar {
                                            NavigationBarItem(
                                                selected = currentRoute == Routes.HOME,
                                                onClick = {
                                                    navController.navigate(Routes.HOME) {
                                                        popUpTo(Routes.HOME) { inclusive = true }
                                                        launchSingleTop = true
                                                    }
                                                },
                                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                                label = { Text("Home") }
                                            )
                                            NavigationBarItem(
                                                selected = currentRoute == Routes.BACKUP,
                                                onClick = {
                                                    navController.navigate(Routes.BACKUP) {
                                                        popUpTo(Routes.HOME)
                                                        launchSingleTop = true
                                                    }
                                                },
                                                icon = { Icon(Icons.Default.Backup, contentDescription = "Backup") },
                                                label = { Text("Backup") }
                                            )
                                        }
                                    }
                                }
                            ) { padding ->
                                NavHost(
                                    navController = navController,
                                    startDestination = Routes.HOME,
                                    modifier = Modifier.padding(padding)
                                ) {
                                    composable(Routes.HOME) {
                                        val viewModel: DashboardViewModel = viewModel(
                                            factory = DashboardViewModelFactory(repository, exchangeRateRepository)
                                        )
                                        DashboardScreen(
                                            viewModel = viewModel,
                                            onAddAsset = { context ->
                                                navController.navigate(
                                                    Routes.addEdit(context = context.name)
                                                )
                                            },
                                            onEditAsset = { id -> navController.navigate(Routes.addEdit(id)) },
                                            onAddBankAccount = { context ->
                                                navController.navigate(
                                                    Routes.addEditBank(context = context.name)
                                                )
                                            },
                                            onEditBankAccount = { id -> navController.navigate(Routes.addEditBank(id)) }
                                        )
                                    }

                                    composable(Routes.BACKUP) {
                                        val viewModel: BackupViewModel = viewModel(
                                            factory = BackupViewModelFactory(repository)
                                        )
                                        BackupScreen(
                                            viewModel = viewModel,
                                            backupStore = app.backupStore,
                                            pinPreferences = pinPreferences
                                        )
                                    }

                                    composable(
                                        route = Routes.ADD_EDIT,
                                        arguments = listOf(
                                            navArgument("assetId") {
                                                type = NavType.LongType
                                                defaultValue = -1L
                                            },
                                            navArgument("context") {
                                                type = NavType.StringType
                                                nullable = true
                                                defaultValue = null
                                            }
                                        )
                                    ) { backStackEntry ->
                                        val assetId = backStackEntry.arguments?.getLong("assetId")?.takeIf { it > 0 }
                                        val addContext = backStackEntry.arguments
                                            ?.getString("context")
                                            ?.let { runCatching { AssetAddContext.valueOf(it) }.getOrNull() }
                                        val viewModel: AddEditAssetViewModel = viewModel(
                                            factory = AddEditAssetViewModelFactory(
                                                repository,
                                                assetId,
                                                addContext
                                            )
                                        )
                                        AddEditAssetScreen(
                                            viewModel = viewModel,
                                            onNavigateBack = { navController.popBackStack() }
                                        )
                                    }

                                    composable(
                                        route = Routes.ADD_EDIT_BANK,
                                        arguments = listOf(
                                            navArgument("bankAccountId") {
                                                type = NavType.LongType
                                                defaultValue = -1L
                                            },
                                            navArgument("context") {
                                                type = NavType.StringType
                                                nullable = true
                                                defaultValue = null
                                            }
                                        )
                                    ) { backStackEntry ->
                                        val bankAccountId = backStackEntry.arguments?.getLong("bankAccountId")?.takeIf { it > 0 }
                                        val addContext = backStackEntry.arguments
                                            ?.getString("context")
                                            ?.let { runCatching { BankAccountAddContext.valueOf(it) }.getOrNull() }
                                        val viewModel: AddEditBankAccountViewModel = viewModel(
                                            factory = AddEditBankAccountViewModelFactory(
                                                repository,
                                                bankAccountId,
                                                addContext
                                            )
                                        )
                                        AddEditBankAccountScreen(
                                            viewModel = viewModel,
                                            onNavigateBack = { navController.popBackStack() }
                                        )
                                    }
                                }
                            }
                        }

                        if (!isUnlocked) {
                            Box(modifier = Modifier
                                .fillMaxSize()
                                .zIndex(1f)) {
                                PinLockScreen(
                                    pinPreferences = pinPreferences,
                                    mode = if (pinPreferences.isPinSet) PinLockMode.Unlock else PinLockMode.Setup,
                                    onUnlocked = {
                                        isUnlocked = true
                                        sessionStarted = true
                                    }
                                )
                            }
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
