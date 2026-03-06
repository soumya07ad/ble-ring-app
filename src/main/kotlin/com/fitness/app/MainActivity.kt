package com.fitness.app

import android.content.Context
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
import com.fitness.app.presentation.dashboard.SmartRingViewModel
import com.fitness.app.presentation.coach.CoachViewModel
import com.fitness.app.presentation.wellness.WellnessViewModel
import com.fitness.app.presentation.streaks.StreakViewModel
import com.fitness.app.presentation.settings.SettingsViewModel
import com.fitness.app.presentation.theme.ThemeViewModel
import com.fitness.app.domain.model.AppTheme
import com.fitness.app.ui.theme.FitnessAppTheme
import com.fitness.app.ui.theme.AppColors
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
            val factory = remember { AppContainer.getInstance(this).viewModelFactory }
            val themeViewModel: ThemeViewModel = viewModel(factory = factory)
            val appTheme by themeViewModel.themeState.collectAsState()

            val isDark = when (appTheme) {
                AppTheme.DARK -> true
                AppTheme.LIGHT -> false
                AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            FitnessAppTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigationFlow(themeViewModel = themeViewModel)
                }
            }
        }
    }
}

@Composable
fun AppNavigationFlow(
    navViewModel: AppNavigationViewModel = viewModel(),
    themeViewModel: ThemeViewModel = viewModel()
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
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = { AppBottomNav(navController = navController) }
            ) { paddingValues ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Dashboard.route,
                    modifier = Modifier.padding(paddingValues)
                ) {
                    composable(Screen.Dashboard.route) {
                        DashboardRoute(
                            viewModel = viewModel(factory = factory),
                            smartRingViewModel = viewModel(factory = factory),
                            navController = navController,
                            themeViewModel = themeViewModel
                        )
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

                    composable("ringSetup") {
                        com.fitness.app.presentation.ring.screens.RingSetupRoute(
                            onSetupComplete = { navController.popBackStack() },
                            onSkip = { navController.popBackStack() }
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
            Text("${screen.label} coming soon...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp)
        }
    }
}

@Composable
fun AppBottomNav(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isCoachSelected = currentRoute == Screen.Coach.route

    // Items split: left pair (Dashboard, Sleep)  |  FAB  |  right pair (Wellness, Streaks)
    val leftItems = listOf(Screen.Dashboard, Screen.Sleep)
    val rightItems = listOf(Screen.Wellness, Screen.Streaks)

    // Pulse animation for the Coach FAB
    val infiniteTransition = rememberInfiniteTransition(label = "coachPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = androidx.compose.animation.core.EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coachScale"
    )
    val fabScale = if (isCoachSelected) 1.15f else pulseScale

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Bottom bar background
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(AppColors.navBarBackground)
                .border(
                    width = 0.5.dp,
                    color = AppColors.navBarBorder,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
                )
                .navigationBarsPadding()
                .height(65.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left items
            leftItems.forEach { screen ->
                BottomNavItem(
                    screen = screen,
                    isSelected = currentRoute == screen.route,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            // Spacer for the center FAB
            Spacer(modifier = Modifier.width(64.dp))

            // Right items
            rightItems.forEach { screen ->
                BottomNavItem(
                    screen = screen,
                    isSelected = currentRoute == screen.route,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }

        // Center Coach FAB — elevated above the bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .offset(y = (-24).dp)
                .size(60.dp)
                .scale(fabScale)
                .clip(CircleShape)
                .background(
                    AppColors.accentGradient
                )
                .border(
                    width = if (isCoachSelected) 2.dp else 1.dp,
                    brush = if (AppColors.isDark) {
                        Brush.linearGradient(
                            listOf(
                                com.fitness.app.ui.theme.NeonPurple.copy(alpha = 0.8f),
                                com.fitness.app.ui.theme.NeonCyan.copy(alpha = 0.4f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                com.fitness.app.ui.theme.SkyBlueDark.copy(alpha = 0.5f),
                                com.fitness.app.ui.theme.HighlighterGreen.copy(alpha = 0.3f)
                            )
                        )
                    },
                    shape = CircleShape
                )
                .clickable {
                    navController.navigate(Screen.Coach.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = Screen.Coach.emoji,
                    fontSize = 24.sp
                )
                Text(
                    text = "Coach",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    screen: Screen,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val accentColor = if (AppColors.isDark) {
        when (screen) {
            Screen.Sleep -> com.fitness.app.ui.theme.PrimaryPurple
            else -> com.fitness.app.ui.theme.NeonCyan
        }
    } else {
        com.fitness.app.ui.theme.SkyBlue
    }

    Column(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
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
            color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
