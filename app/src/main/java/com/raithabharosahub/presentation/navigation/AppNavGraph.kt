package com.raithabharosahub.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.raithabharosahub.R
import com.raithabharosahub.presentation.auth.screens.LanguageScreen
import com.raithabharosahub.presentation.auth.screens.LoginScreen
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
import com.raithabharosahub.presentation.onboarding.screens.PlotGpsScreen
import com.raithabharosahub.presentation.settings.SettingsScreen
import com.raithabharosahub.util.findActivity

object AppRoutes {
    // ── Auth flow ─────────────────────────────────────────────────────────────
    /** Pre-login language selection (only shown when no language is persisted). */
    const val PreLoginLanguage   = "pre_login_language"
    /** Email/password + Google login / registration. */
    const val Login              = "login"

    // ── Onboarding flow ───────────────────────────────────────────────────────
    const val OnboardingLanguage  = "onboarding_language"
    const val OnboardingProfile   = "onboarding_profile"
    const val OnboardingPlot      = "onboarding_plot"

    // ── Main app ──────────────────────────────────────────────────────────────
    const val Dashboard    = "dashboard"
    const val NpkCentre    = "npk_centre"
    const val KrishiCalendar = "krishi_calendar"
    const val SeasonHistory = "season_history"
    const val Settings     = "settings"
}

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    onboardingViewModel: OnboardingViewModel = hiltViewModel(LocalContext.current.findActivity())
) {
    val onboardingState by onboardingViewModel.uiState.collectAsState()

    // Wait until DataStore preferences are loaded before deciding the start destination.
    if (onboardingState.isLoading) return

    // ── Start-destination logic ───────────────────────────────────────────────
    //
    //  CASE 1: User is already logged in with Firebase.
    //          → Go straight to Dashboard (skip auth + language gate entirely).
    //
    //  CASE 2: Not logged in AND language already set.
    //          → Start at Login (language gate is already done).
    //
    //  CASE 3: Not logged in AND no language preference yet.
    //          → Start at PreLoginLanguage so user picks locale first.
    //
    val isLoggedIn     = FirebaseAuth.getInstance().currentUser != null
    val hasLanguage    = onboardingState.language.isNotBlank()
    val isOnboardingComplete = onboardingState.isComplete

    val startDestination = when {
        !hasLanguage -> AppRoutes.PreLoginLanguage
        isLoggedIn && isOnboardingComplete -> AppRoutes.Dashboard
        isLoggedIn && !isOnboardingComplete -> AppRoutes.OnboardingProfile
        else -> AppRoutes.Login
    }

    // ── Bottom-nav items ──────────────────────────────────────────────────────
    val bottomNavItems = listOf(
        BottomNavItem(AppRoutes.Dashboard,     stringResource(R.string.dashboard_title),     Icons.Default.Home),
        BottomNavItem(AppRoutes.NpkCentre,     stringResource(R.string.npk_title),           Icons.Default.Science),
        BottomNavItem(AppRoutes.KrishiCalendar, stringResource(R.string.calendar_title),     Icons.Default.CalendarMonth),
        BottomNavItem(AppRoutes.SeasonHistory, stringResource(R.string.season_history_title), Icons.Default.History)
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val mainRoutes = setOf(
        AppRoutes.Dashboard,
        AppRoutes.NpkCentre,
        AppRoutes.KrishiCalendar,
        AppRoutes.SeasonHistory
    )
    val showBottomNav = currentDestination?.route in mainRoutes

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
                            icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
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

            // ── Auth screens ──────────────────────────────────────────────────

            composable(AppRoutes.PreLoginLanguage) {
                LanguageScreen(
                    onLanguageSelected = {
                        navController.navigate(AppRoutes.Login) {
                            popUpTo(AppRoutes.PreLoginLanguage) { inclusive = true }
                        }
                    },
                    onboardingViewModel = onboardingViewModel
                )
            }

            composable(AppRoutes.Login) {
                LoginScreen(
                    onNavigateToOnboarding = {
                        navController.navigate(AppRoutes.OnboardingProfile) {
                            popUpTo(AppRoutes.Login) { inclusive = true }
                        }
                    },
                    onNavigateToDashboard = {
                        navController.navigate(AppRoutes.Dashboard) {
                            popUpTo(AppRoutes.Login) { inclusive = true }
                        }
                    }
                )
            }

            // ── Onboarding screens ────────────────────────────────────────────

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
                    onFinish = {
                        navController.navigate(AppRoutes.Dashboard) {
                            popUpTo(AppRoutes.OnboardingProfile) { inclusive = true }
                        }
                    },
                    viewModel = onboardingViewModel
                )
            }

            // ── Main app screens ──────────────────────────────────────────────

            composable(AppRoutes.Dashboard) {
                val dashboardViewModel: DashboardViewModel =
                    hiltViewModel(LocalContext.current.findActivity())
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onSettingsClick = { navController.navigate(AppRoutes.Settings) }
                )
            }

            composable(AppRoutes.NpkCentre) {
                val npkViewModel: NpkViewModel =
                    hiltViewModel(LocalContext.current.findActivity())
                NpkScreen(viewModel = npkViewModel)
            }

            composable(AppRoutes.KrishiCalendar) {
                val calendarViewModel: KrishiCalendarViewModel =
                    hiltViewModel(LocalContext.current.findActivity())
                KrishiCalendarScreen(viewModel = calendarViewModel)
            }

            composable(AppRoutes.SeasonHistory) {
                val historyViewModel: SeasonHistoryViewModel =
                    hiltViewModel(LocalContext.current.findActivity())
                SeasonHistoryScreen(
                    onNavigateBack = { navController.navigateUp() },
                    viewModel = historyViewModel
                )
            }

            composable(AppRoutes.Settings) {
                SettingsScreen(navController = navController)
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
