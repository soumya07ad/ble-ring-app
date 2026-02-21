package com.fitness.app

import android.content.Context
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.fitness.app.network.client.RetrofitClient
import com.fitness.app.network.repository.FitnessRepository
import com.fitness.app.MockData
import com.fitness.app.network.sync.SyncWorker
import com.fitness.app.ui.theme.FitnessAppTheme

class MainActivity : ComponentActivity() {
    
    companion object {
        lateinit var fitnessAPI: FitnessAPI
        lateinit var fitnessRepository: FitnessRepository
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Clear SharedPreferences to always use MockData
        val sharedPreferences = getSharedPreferences("fitness_app", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        
        // Initialize FitnessAPI (local)
        fitnessAPI = FitnessAPI(this)
        
        // Initialize Network layer
        val retrofitClient = RetrofitClient.getInstance(this)
        fitnessRepository = FitnessRepository(
            retrofitClient.getApiService(),
            retrofitClient.getTokenManager()
        )
        
        // Start background sync
        SyncWorker.scheduleSyncWorker(this)
        
        setContent {
            FitnessAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigationFlow()
                }
            }
        }
    }
}

@Composable
fun AppNavigationFlow() {
    val userLoggedIn by AppState.userLoggedIn
    val setupComplete by AppState.setupComplete

    when {
        !userLoggedIn -> {
            // Show WebView with login flow
            FitnessWebView(onLoginSuccess = {
                AppState.setUserLoggedIn(true)
                // Reset setup when user logs in (so they see setup screen)
                AppState.setSetupComplete(false)
            })
        }
        !setupComplete -> {
            // Show Smart Ring Setup after login (Route manages its own ViewModel)
            com.fitness.app.presentation.ring.screens.RingSetupRoute(
                onSetupComplete = {
                    AppState.setSetupComplete(true)
                },
                onSkip = {
                    AppState.setSetupComplete(true)
                }
            )
        }
        else -> {
            // Show full app with dashboard (Route manages its own ViewModel)
            DashboardRoute()
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
                loadUrl("file:///android_asset/index.html?page=login")
            }
        }
    )
}

// Deprecated: Replaced by native DashboardScreenWithHeader
@Composable
fun FitnessAppWithStress() {
    // Kept for reference but unused
}

// Bridge class for communication between WebView and Kotlin
class LoginBridge(private val onLoginSuccess: () -> Unit) {
    @android.webkit.JavascriptInterface
    fun notifyLoginSuccess() {
        onLoginSuccess()
    }
}

// PageBridge removed as no longer needed for native dashboard
class PageBridge {
    // Only kept if other WebViews need it, otherwise can be removed.
    // For now keeping simpler version to avoid breaking if referenced elsewhere (unlikely).
    @android.webkit.JavascriptInterface
    fun getPageState(): String {
        return "dashboard"
    }
}

