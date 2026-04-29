package com.raithabharosahub.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.raithabharosahub.R
import com.raithabharosahub.presentation.calendar.KrishiCalendarScreen
import com.raithabharosahub.presentation.calendar.KrishiCalendarViewModel
import com.raithabharosahub.presentation.dashboard.DashboardScreen
import com.raithabharosahub.presentation.dashboard.DashboardViewModel
import com.raithabharosahub.presentation.history.SeasonHistoryScreen
import com.raithabharosahub.presentation.history.SeasonHistoryViewModel
import com.raithabharosahub.presentation.npk.NpkScreen
import com.raithabharosahub.presentation.npk.NpkViewModel
import com.raithabharosahub.presentation.onboarding.OnboardingViewModel
import com.raithabharosahub.presentation.onboarding.screens.FarmerProfileScreen
import com.raithabharosahub.presentation.onboarding.screens.LanguagePickerScreen
import com.raithabharosahub.presentation.onboarding.screens.PermissionsScreen
import com.raithabharosahub.presentation.onboarding.screens.PlotGpsScreen
import com.raithabharosahub.presentation.settings.SettingsScreen
import com.raithabharosahub.util.findActivity

object AppRoutes {
    const val OnboardingLanguage = "onboarding_language"
    const val OnboardingProfile = "onboarding_profile"
    const val OnboardingPlot = "onboarding_plot"
    const val OnboardingPermissions = "onboarding_permissions"
    const val Dashboard = "dashboard"
    const val NpkCentre = "npk_centre"
    const val KrishiCalendar = "krishi_calendar"
    const val SeasonHistory = "season_history"
    const val Settings = "settings"
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    onboardingViewModel: OnboardingViewModel = hiltViewModel(LocalContext.current.findActivity())
) {
    val state by onboardingViewModel.uiState.collectAsState()

    if (state.isLoading) {
        return
    }

    val startDestination = if (state.isComplete) {
        AppRoutes.Dashboard
    } else {
        AppRoutes.OnboardingLanguage
    }

    // Bottom navigation items
    val bottomNavItems = listOf(
        BottomNavItem(
            route = AppRoutes.Dashboard,
            label = stringResource(R.string.dashboard_title),
            icon = Icons.Default.Home
        ),
        BottomNavItem(
            route = AppRoutes.NpkCentre,
            label = stringResource(R.string.npk_title),
            icon = Icons.Default.Science
        ),
        BottomNavItem(
            route = AppRoutes.KrishiCalendar,
            label = stringResource(R.string.calendar_title),
            icon = Icons.Default.CalendarMonth
        ),
        BottomNavItem(
            route = AppRoutes.SeasonHistory,
            label = stringResource(R.string.season_history_title),
            icon = Icons.Default.History
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Show bottom nav only for main screens (not onboarding)
    val showBottomNav = currentDestination?.route in listOf(
        AppRoutes.Dashboard,
        AppRoutes.NpkCentre,
        AppRoutes.KrishiCalendar,
        AppRoutes.SeasonHistory
    )

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(text = item.label) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(AppRoutes.OnboardingLanguage) {
                LanguagePickerScreen(
                    onLanguageSelected = {
                        navController.navigate(AppRoutes.OnboardingProfile)
                    },
                    viewModel = onboardingViewModel
                )
            }

            composable(AppRoutes.OnboardingProfile) {
                FarmerProfileScreen(
                    onNext = {
                        navController.navigate(AppRoutes.OnboardingPlot)
                    },
                    viewModel = onboardingViewModel
                )
            }

            composable(AppRoutes.OnboardingPlot) {
                PlotGpsScreen(
                    onNext = {
                        navController.navigate(AppRoutes.OnboardingPermissions)
                    },
                    viewModel = onboardingViewModel
                )
            }

            composable(AppRoutes.OnboardingPermissions) {
                PermissionsScreen(
                    onFinish = {
                        navController.navigate(AppRoutes.Dashboard) {
                            popUpTo(AppRoutes.OnboardingLanguage) { inclusive = true }
                        }
                    },
                    viewModel = onboardingViewModel
                )
            }

            composable(AppRoutes.Dashboard) {
                val dashboardViewModel: DashboardViewModel = hiltViewModel(LocalContext.current.findActivity())
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onSettingsClick = {
                        navController.navigate(AppRoutes.Settings)
                    }
                )
            }

            composable(AppRoutes.NpkCentre) {
                val npkViewModel: NpkViewModel = hiltViewModel(LocalContext.current.findActivity())
                NpkScreen(
                    viewModel = npkViewModel
                )
            }

            composable(AppRoutes.KrishiCalendar) {
                val calendarViewModel: KrishiCalendarViewModel = hiltViewModel(LocalContext.current.findActivity())
                KrishiCalendarScreen(
                    viewModel = calendarViewModel
                )
            }

            composable(AppRoutes.SeasonHistory) {
                val historyViewModel: SeasonHistoryViewModel = hiltViewModel(LocalContext.current.findActivity())
                SeasonHistoryScreen(
                    onNavigateBack = { navController.navigateUp() },
                    viewModel = historyViewModel
                )
            }

            composable(AppRoutes.Settings) {
                SettingsScreen(
                    navController = navController
                )
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

