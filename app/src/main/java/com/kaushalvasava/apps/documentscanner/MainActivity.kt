package com.kaushalvasava.apps.documentscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kaushalvasava.apps.documentscanner.ui.screen.HomeScreen
import com.kaushalvasava.apps.documentscanner.ui.screen.LoginScreen
import com.kaushalvasava.apps.documentscanner.ui.screen.SettingsScreen
import com.kaushalvasava.apps.documentscanner.ui.theme.DocumentScannerTheme
import com.kaushalvasava.apps.documentscanner.ui.viewmodel.AuthViewModel
import com.kaushalvasava.apps.documentscanner.ui.viewmodel.ScannerViewModel

private object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DocumentScannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    val authViewModel: AuthViewModel = viewModel(
                        factory = AuthViewModel.Factory(applicationContext)
                    )
                    val scannerViewModel: ScannerViewModel = viewModel(
                        factory = ScannerViewModel.Factory(applicationContext)
                    )

                    // Determine start destination: logged in → home, else → login
                    val savedToken by authViewModel.isLoggedIn.collectAsState(initial = null)
                    // null means still loading; use login as safe default
                    val startDestination = if (savedToken != null && savedToken!!.isNotBlank())
                        Routes.HOME else Routes.LOGIN

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable(Routes.LOGIN) {
                            LoginScreen(
                                viewModel = authViewModel,
                                onLoginSuccess = {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.LOGIN) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(Routes.HOME) {
                            HomeScreen(
                                viewModel = scannerViewModel,
                                onNavigateToSettings = {
                                    navController.navigate(Routes.SETTINGS)
                                }
                            )
                        }
                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                authViewModel = authViewModel,
                                scannerViewModel = scannerViewModel,
                                onBack = { navController.popBackStack() },
                                onLogout = {
                                    navController.navigate(Routes.LOGIN) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}