package com.sunitha.fittrack.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sunitha.fittrack.FitTrackApp
import com.sunitha.fittrack.ui.ai.AiInsightsScreen
import com.sunitha.fittrack.ui.ai.AiInsightsViewModel
import com.sunitha.fittrack.ui.ai.AiInsightsViewModelFactory
import com.sunitha.fittrack.ui.home.HomeScreen
import com.sunitha.fittrack.ui.home.HomeViewModel
import com.sunitha.fittrack.ui.home.HomeViewModelFactory
import com.sunitha.fittrack.ui.home.MacroHistoryScreen
import com.sunitha.fittrack.ui.home.MacroHistoryViewModel
import com.sunitha.fittrack.ui.home.MacroHistoryViewModelFactory
import com.sunitha.fittrack.ui.onboarding.OnboardingScreen
import com.sunitha.fittrack.ui.onboarding.OnboardingViewModel
import com.sunitha.fittrack.ui.onboarding.OnboardingViewModelFactory
import com.sunitha.fittrack.ui.settings.ImportScreen
import com.sunitha.fittrack.ui.settings.ImportViewModel
import com.sunitha.fittrack.ui.settings.ImportViewModelFactory
import com.sunitha.fittrack.ui.settings.SettingsScreen
import com.sunitha.fittrack.ui.settings.SettingsViewModel
import com.sunitha.fittrack.ui.settings.SettingsViewModelFactory
import com.sunitha.fittrack.ui.nutrition.NutritionScreen
import com.sunitha.fittrack.ui.nutrition.NutritionViewModel
import com.sunitha.fittrack.ui.nutrition.NutritionViewModelFactory
import com.sunitha.fittrack.ui.progress.ProgressScreen
import com.sunitha.fittrack.ui.progress.ProgressViewModel
import com.sunitha.fittrack.ui.progress.ProgressViewModelFactory
import com.sunitha.fittrack.ui.theme.GreenPrimary
import com.sunitha.fittrack.ui.workout.WorkoutHistoryScreen
import com.sunitha.fittrack.ui.workout.WorkoutHistoryViewModel
import com.sunitha.fittrack.ui.workout.WorkoutHistoryViewModelFactory
import com.sunitha.fittrack.ui.workout.WorkoutScreen
import com.sunitha.fittrack.ui.workout.WorkoutViewModel
import com.sunitha.fittrack.ui.workout.WorkoutViewModelFactory

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

private val navItems = listOf(
    NavItem("home",      "Home",      Icons.Filled.Home),
    NavItem("workouts",  "Workouts",  Icons.Filled.FitnessCenter),
    NavItem("nutrition", "Nutrition", Icons.Filled.Restaurant),
    NavItem("progress",  "Progress",  Icons.AutoMirrored.Filled.ShowChart),
    NavItem("ai",        "AI",        Icons.Filled.AutoAwesome)
)

// Routes that should hide the bottom navigation bar
private val fullScreenRoutes = setOf("onboarding", "settings", "import", "macro_history", "workout_history")

@Composable
fun AppNavigation() {
    val navController  = rememberNavController()
    val backStack      by navController.currentBackStackEntryAsState()
    val currentRoute   = backStack?.destination?.route

    val context      = LocalContext.current
    val app          = context.applicationContext as FitTrackApp
    val repo         = app.repository
    val profileStore = app.profileStore

    // Watch profile setup state; navigate to onboarding on first launch
    val isProfileSetUp by profileStore.profile
        .collectAsState(initial = null)

    // Navigate to onboarding only once when we first know the profile is not set up.
    // We leave "home" in the back stack so popBackStack() from onboarding lands on home.
    LaunchedEffect(isProfileSetUp?.isSetUp) {
        if (isProfileSetUp?.isSetUp == false) {
            navController.navigate("onboarding")
        }
    }

    val showBottomBar = currentRoute !in fullScreenRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    navItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick  = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon  = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor   = GreenPrimary,
                                selectedTextColor   = GreenPrimary,
                                indicatorColor      = GreenPrimary.copy(alpha = 0.12f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = "home",
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                val vm: HomeViewModel = viewModel(factory = HomeViewModelFactory(repo, profileStore))
                HomeScreen(vm, onNavigate = { route ->
                    if (route == "macro_history" || route == "workout_history") {
                        navController.navigate(route)
                    } else {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                }, onOpenSettings = { navController.navigate("settings") })
            }
            composable("workouts") {
                val vm: WorkoutViewModel = viewModel(factory = WorkoutViewModelFactory(repo))
                WorkoutScreen(vm)
            }
            composable("nutrition") {
                val vm: NutritionViewModel = viewModel(factory = NutritionViewModelFactory(repo, profileStore))
                NutritionScreen(vm)
            }
            composable("progress") {
                val vm: ProgressViewModel = viewModel(factory = ProgressViewModelFactory(repo))
                ProgressScreen(vm)
            }
            composable("ai") {
                val vm: AiInsightsViewModel = viewModel(factory = AiInsightsViewModelFactory(repo, profileStore))
                AiInsightsScreen(vm)
            }
            composable("settings") {
                val vm: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(repo))
                SettingsScreen(
                    vm                 = vm,
                    onBack             = { navController.popBackStack() },
                    onNavigateToImport = { navController.navigate("import") },
                    onEditProfile      = { navController.navigate("onboarding") }
                )
            }
            composable("import") {
                val vm: ImportViewModel = viewModel(factory = ImportViewModelFactory(repo))
                ImportScreen(vm, onBack = { navController.popBackStack() })
            }
            composable("macro_history") {
                val vm: MacroHistoryViewModel = viewModel(factory = MacroHistoryViewModelFactory(repo, profileStore))
                val nutritionVm: NutritionViewModel = viewModel(factory = NutritionViewModelFactory(repo, profileStore))
                MacroHistoryScreen(vm, nutritionVm, onBack = { navController.popBackStack() })
            }
            composable("workout_history") {
                val vm: WorkoutHistoryViewModel = viewModel(factory = WorkoutHistoryViewModelFactory(repo))
                WorkoutHistoryScreen(vm, onBack = { navController.popBackStack() })
            }
            composable("onboarding") {
                val vm: OnboardingViewModel = viewModel(factory = OnboardingViewModelFactory(profileStore))
                // popBackStack works for both first-launch (home ← onboarding → home)
                // and edit-from-settings (settings ← onboarding → settings)
                OnboardingScreen(vm, onDone = { navController.popBackStack() })
            }
        }
    }
}
