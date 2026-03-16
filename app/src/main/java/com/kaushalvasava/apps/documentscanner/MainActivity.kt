package com.kaushalvasava.apps.documentscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kaushalvasava.apps.documentscanner.data.SettingsDataStore
import com.kaushalvasava.apps.documentscanner.ui.screen.HomeScreen
import com.kaushalvasava.apps.documentscanner.ui.screen.LoginScreen
import com.kaushalvasava.apps.documentscanner.ui.screen.SettingsScreen
import com.kaushalvasava.apps.documentscanner.ui.theme.AppTheme
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
            // Read persisted theme from DataStore
            val dataStore = remember { SettingsDataStore(applicationContext) }
            val savedThemeKey by dataStore.appTheme.collectAsState(initial = null)
            // Live theme state — updated immediately when user picks in Settings
            var currentTheme by remember(savedThemeKey) {
                mutableStateOf(AppTheme.fromWebKey(savedThemeKey))
            }

            DocumentScannerTheme(appTheme = currentTheme) {
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

                    val savedToken by authViewModel.isLoggedIn.collectAsState(initial = null)
                    val userName by authViewModel.userName.collectAsState(initial = null)
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
                                userName = userName,
                                onNavigateToSettings = {
                                    navController.navigate(Routes.SETTINGS)
                                },
                                onSessionExpired = {
                                    authViewModel.logout()
                                    navController.navigate(Routes.LOGIN) {
                                        popUpTo(0) { inclusive = true }
                                    }
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
                                },
                                // Live theme swap — no app restart needed
                                onThemeChange = { newTheme -> currentTheme = newTheme }
                            )
                        }
                    }
                }
            }
        }
    }
}