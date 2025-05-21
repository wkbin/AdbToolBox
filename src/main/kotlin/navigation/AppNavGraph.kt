package navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import ui.devices.DevicesScreen
import ui.settings.SettingsScreen
import ui.apk.ApkScreen
import ui.file.FileScreen
import ui.home.HomeScreen
import viewmodel.DevicesViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
) {
    val devicesViewModel = viewModel { DevicesViewModel() }
    NavHost(
        navController = navController,
        startDestination = NavScreen.Devices.id
    ) {
        composable(NavScreen.Devices.id) {
            DevicesScreen(devicesViewModel)
        }
//        composable(NavScreen.Home.id) {
//            HomeScreen(
//                onNavigateToBattery = {},
//                onNavigateToDevice = {},
//                onNavigateToNetwork = {},
//                onNavigateToStorage = {})
//        }
        composable(NavScreen.File.id) {
            FileScreen()
        }
        composable(NavScreen.Apk.id) {
            ApkScreen()
        }
        composable(NavScreen.Settings.id) {
            SettingsScreen()
        }
    }
}