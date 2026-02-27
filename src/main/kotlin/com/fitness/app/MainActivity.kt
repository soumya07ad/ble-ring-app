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
import com.fitness.app.core.di.AppContainer
import com.fitness.app.network.sync.SyncWorker
import com.fitness.app.presentation.navigation.AppNavigationViewModel
import com.fitness.app.presentation.dashboard.screens.DashboardRoute
import com.fitness.app.ui.theme.FitnessAppTheme

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
    var selectedTab by remember { mutableStateOf(0) }  // 0 = Dashboard, 1 = Sleep

    when {
        !navState.userLoggedIn -> {
            // Show WebView with login flow
            FitnessWebView(onLoginSuccess = {
                navViewModel.onLoginSuccess()
            })
        }
        !navState.setupComplete -> {
            // Show Smart Ring Setup
            com.fitness.app.presentation.ring.screens.RingSetupRoute(
                onSetupComplete = { navViewModel.onSetupComplete() },
                onSkip = { navViewModel.onSkip() }
            )
        }
        else -> {
            // Main app with bottom navigation
            Scaffold(
                containerColor = Color(0xFF050508),
                bottomBar = {
                    AppBottomNav(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    when (selectedTab) {
                        0 -> DashboardRoute()
                        1 -> {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val factory = com.fitness.app.presentation.dashboard.SleepTrackerViewModel.provideFactory(
                                com.fitness.app.core.di.AppContainer.getInstance(context).sleepRepository
                            )
                            val sleepViewModel: com.fitness.app.presentation.dashboard.SleepTrackerViewModel = 
                                androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)

                            com.fitness.app.presentation.dashboard.screens.SleepTrackerScreen(
                                viewModel = sleepViewModel,
                                onBack = { selectedTab = 0 }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppBottomNav(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val items = listOf(
        Triple(0, "Dashboard", "🏠"),
        Triple(1, "Sleep",     "😴")
    )

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
            .height(60.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (idx, label, emoji) ->
            val isSelected = selectedTab == idx
            val accentColor = if (idx == 1) com.fitness.app.ui.theme.PrimaryPurple
                              else com.fitness.app.ui.theme.NeonCyan
            Column(
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .clickable { onTabSelected(idx) }
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = emoji,
                    fontSize = if (isSelected) 22.sp else 18.sp
                )
                Text(
                    text = label,
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
