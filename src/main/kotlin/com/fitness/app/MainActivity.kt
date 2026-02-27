package com.fitness.app

import android.content.Context
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fitness.app.core.di.AppContainer
import com.fitness.app.network.sync.SyncWorker
import com.fitness.app.presentation.navigation.AppNavigationViewModel
import com.fitness.app.presentation.dashboard.screens.DashboardRoute
import com.fitness.app.presentation.dashboard.screens.SleepTrackerScreen
import com.fitness.app.presentation.coach.screens.CoachScreen
import com.fitness.app.presentation.wellness.screens.WellnessScreen
import com.fitness.app.presentation.navigation.Screen
import com.fitness.app.presentation.dashboard.DashboardViewModel
import com.fitness.app.presentation.dashboard.SleepTrackerViewModel
import com.fitness.app.presentation.coach.CoachViewModel
import com.fitness.app.presentation.wellness.WellnessViewModel
import com.fitness.app.presentation.streaks.StreakViewModel
import com.fitness.app.presentation.settings.SettingsViewModel
import com.fitness.app.ui.theme.FitnessAppTheme
import com.fitness.app.ui.theme.TextSecondary
import androidx.compose.runtime.remember

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen BEFORE super.onCreate
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Initialize DI container (creates FitnessAPI, Retrofit, etc.)
        AppContainer.initialize(this)

        // Start background sync
        SyncWorker.scheduleSyncWorker(this)

        setContent {
            FitnessAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF050508) // Match dark auth background
                ) {
                    AppNavigationFlow()
                }
            }
        }
    }
}

@Composable
fun AppNavigationFlow(
    navViewModel: AppNavigationViewModel = viewModel()
) {
    val navState by navViewModel.uiState.collectAsState()
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val factory = remember { AppContainer.getInstance(context).viewModelFactory }

    when {
        !navState.userLoggedIn -> {
            FitnessWebView(onLoginSuccess = {
                navViewModel.onLoginSuccess()
            })
        }
        !navState.setupComplete -> {
            com.fitness.app.presentation.ring.screens.RingSetupRoute(
                onSetupComplete = { navViewModel.onSetupComplete() },
                onSkip = { navViewModel.onSkip() }
            )
        }
        else -> {
            Scaffold(
                containerColor = Color(0xFF050508),
                bottomBar = { AppBottomNav(navController = navController) }
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Dashboard.route,
                    modifier = Modifier.padding(paddingValues)
                ) {
                    composable(Screen.Dashboard.route) {
                        DashboardRoute(viewModel = viewModel(factory = factory))
                    }

                    composable(Screen.Sleep.route) {
                        SleepTrackerScreen(
                            viewModel = viewModel(factory = factory),
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Coach.route) {
                        CoachScreen(
                            viewModel = viewModel(factory = factory),
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Wellness.route) {
                        WellnessScreen(
                            viewModel = viewModel(factory = factory),
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Streaks.route) {
                        com.fitness.app.presentation.streaks.screens.StreaksScreen(
                            viewModel = viewModel(factory = factory),
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.Settings.route) {
                        com.fitness.app.presentation.settings.screens.SettingsScreen(
                            viewModel = viewModel(factory = factory),
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}




@Composable
fun PlaceholderScreen(screen: Screen) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(screen.emoji, fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text("${screen.label} coming soon...", color = TextSecondary, fontSize = 18.sp)
        }
    }
}

@Composable
fun AppBottomNav(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = Screen.bottomNavItems

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF080810))
            .border(
                width = 0.5.dp,
                color = Color(0xFF1A1A2E),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
            )
            .navigationBarsPadding()
            .height(65.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { screen ->
            val isSelected = currentRoute == screen.route
            val accentColor = when (screen) {
                Screen.Sleep -> com.fitness.app.ui.theme.PrimaryPurple
                Screen.Coach -> com.fitness.app.ui.theme.NeonPurple
                else -> com.fitness.app.ui.theme.NeonCyan
            }
            
            Column(
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .clickable {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = screen.emoji,
                    fontSize = if (isSelected) 22.sp else 18.sp
                )
                Text(
                    text = screen.label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) accentColor else Color(0xFF5A5A7A)
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(1.dp))
                            .background(accentColor)
                    )
                }
            }
        }
    }
}

@Composable
fun FitnessWebView(onLoginSuccess: () -> Unit) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    domStorageEnabled = true
                    databaseEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                // Add JavaScript bridge for login callback
                addJavascriptInterface(LoginBridge(onLoginSuccess), "LoginBridge")
                setBackgroundColor(0xFF050508.toInt())
                loadUrl("file:///android_asset/index.html?page=login")
            }
        }
    )
}

// Bridge class for communication between WebView and Kotlin
class LoginBridge(private val onLoginSuccess: () -> Unit) {
    @android.webkit.JavascriptInterface
    fun notifyLoginSuccess() {
        onLoginSuccess()
    }
}
