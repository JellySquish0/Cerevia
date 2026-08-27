package com.example.cerevia.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cerevia.ui.screens.analysis.stage1.Stage1Screen
import com.example.cerevia.ui.screens.analysis.stage2.Stage2Screen
import com.example.cerevia.ui.screens.analysis.stage3.Stage3Screen
import com.example.cerevia.ui.screens.consultation.ConsultationScreen
import com.example.cerevia.ui.screens.directory.DirectoryScreen
import com.example.cerevia.ui.screens.directory.DoctorDetailScreen
import com.example.cerevia.ui.screens.directory.HospitalDetailScreen
import com.example.cerevia.ui.screens.directory.BookingConfirmationScreen
import com.example.cerevia.ui.screens.education.EducationScreen
import com.example.cerevia.ui.screens.education.BEFastDetailScreen
import com.example.cerevia.ui.screens.history.HistoryDetailScreen
import com.example.cerevia.ui.screens.history.HistoryScreen
import com.example.cerevia.ui.screens.home.HomeScreen
import com.example.cerevia.ui.screens.result.ResultScreen
import com.example.cerevia.ui.screens.trends.TrendsScreen
import com.example.cerevia.ui.screens.directory.PaymentScreen
import com.example.cerevia.ui.screens.bluetooth.DeviceScanScreen

import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.outlined.Analytics
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.unit.dp

object Routes {
    const val HOME = "home"
    const val ANALYSIS_STAGE1 = "analysis/stage1"
    const val ANALYSIS_STAGE2 = "analysis/stage2"
    const val ANALYSIS_STAGE3 = "analysis/stage3"
    const val RESULT = "result/{analysisId}"
    const val HISTORY = "history"
    const val HISTORY_DETAIL = "history/{analysisId}"
    const val TRENDS = "trends"
    const val EDUCATION = "education"
    const val DIRECTORY = "directory"
    const val DOCTOR_DETAIL = "directory/doctor/{doctorId}"
    const val HOSPITAL_DETAIL = "directory/hospital/{hospitalId}"
    const val BOOKING_CONFIRMATION = "konfirmasi_booking"
    const val PAYMENT = "payment"
    const val CONSULTATION = "consultation"
    const val BE_FAST_DETAIL = "education/befast"
    const val DEVICE_SCAN = "bluetooth/scan"
}

sealed class BottomNavItem(val route: String, val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Analysis : BottomNavItem(Routes.HOME, "Analisis", Icons.Filled.Analytics, Icons.Outlined.Analytics)
    object History : BottomNavItem(Routes.HISTORY, "Histori", Icons.Filled.History, Icons.Outlined.History)
    object Education : BottomNavItem(Routes.EDUCATION, "Edukasi", Icons.Filled.School, Icons.Outlined.School)
    object Directory : BottomNavItem(Routes.DIRECTORY, "Dokter", Icons.Filled.LocalHospital, Icons.Outlined.LocalHospital)
}

@Composable
fun CereviaApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val items = listOf(BottomNavItem.Analysis, BottomNavItem.History, BottomNavItem.Education, BottomNavItem.Directory)
    
    // Only show bottom bar on top-level routes
    val showBottomBar = items.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    tonalElevation = 8.dp
                ) {
                    items.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = { Icon(if (selected) item.selectedIcon else item.unselectedIcon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = selected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        CereviaNavHost(navController = navController, modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()))
    }
}

@Composable
fun CereviaNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(navController = navController, startDestination = Routes.HOME, modifier = modifier) {
        composable(Routes.HOME) { HomeScreen(navController = navController) }
        composable(Routes.ANALYSIS_STAGE1) { Stage1Screen(navController = navController) }
        composable(Routes.ANALYSIS_STAGE2) { Stage2Screen(navController = navController) }
        composable(Routes.ANALYSIS_STAGE3) { Stage3Screen(navController = navController) }
        composable(
            route = Routes.RESULT,
            arguments = listOf(navArgument("analysisId") { type = NavType.LongType })
        ) { back -> ResultScreen(analysisId = back.arguments?.getLong("analysisId") ?: 0L, navController = navController) }
        composable(Routes.HISTORY) { HistoryScreen(navController = navController) }
        composable(
            route = Routes.HISTORY_DETAIL,
            arguments = listOf(navArgument("analysisId") { type = NavType.LongType })
        ) { back -> HistoryDetailScreen(analysisId = back.arguments?.getLong("analysisId") ?: 0L, navController = navController) }
        composable(Routes.TRENDS) { TrendsScreen(navController = navController) }
        composable(Routes.EDUCATION) { EducationScreen(navController = navController) }
        composable(Routes.BE_FAST_DETAIL) { BEFastDetailScreen(navController = navController) }
        composable(Routes.DIRECTORY) { DirectoryScreen(navController = navController) }
        composable(
            route = Routes.DOCTOR_DETAIL,
            arguments = listOf(navArgument("doctorId") { type = NavType.IntType })
        ) { back -> DoctorDetailScreen(doctorId = back.arguments?.getInt("doctorId") ?: 0, navController = navController) }
        composable(Routes.HOSPITAL_DETAIL,
            arguments = listOf(navArgument("hospitalId") { type = NavType.IntType })
        ) { back -> HospitalDetailScreen(hospitalId = back.arguments?.getInt("hospitalId") ?: 0, navController = navController) }
        composable(Routes.BOOKING_CONFIRMATION) { BookingConfirmationScreen(navController = navController) }
        composable(Routes.PAYMENT) { PaymentScreen(navController = navController) }
        composable(Routes.CONSULTATION) { ConsultationScreen(navController = navController) }
        composable(Routes.DEVICE_SCAN) { DeviceScanScreen(navController = navController) }
    }
}
