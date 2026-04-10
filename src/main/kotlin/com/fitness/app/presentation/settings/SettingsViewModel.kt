package com.fitness.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

import com.fitness.app.domain.repository.IAuthRepository
import com.fitness.app.network.auth.TokenManager
import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.launch

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val metricUnitsEnabled: Boolean = true,
    val bedtimeReminderEnabled: Boolean = false,
    val dataSyncEnabled: Boolean = true,
    val userName: String = "",
    val userDob: String = "",
    val userGender: String = ""
)

class SettingsViewModel(
    private val settingsRepository: ISettingsRepository,
    private val authRepository: IAuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.notificationsEnabled,
        settingsRepository.metricUnitsEnabled,
        settingsRepository.bedtimeReminderEnabled,
        settingsRepository.dataSyncEnabled,
        settingsRepository.userName,
        settingsRepository.userDob,
        settingsRepository.userGender
    ) { values ->
        SettingsUiState(
            notificationsEnabled    = values[0] as Boolean,
            metricUnitsEnabled      = values[1] as Boolean,
            bedtimeReminderEnabled  = values[2] as Boolean,
            dataSyncEnabled         = values[3] as Boolean,
            userName                = values[4] as String,
            userDob                 = values[5] as String,
            userGender              = values[6] as String
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun toggleNotifications(enabled: Boolean)  = settingsRepository.setNotificationsEnabled(enabled)
    fun toggleMetricUnits(enabled: Boolean)     = settingsRepository.setMetricUnitsEnabled(enabled)
    fun toggleBedtimeReminder(enabled: Boolean) = settingsRepository.setBedtimeReminderEnabled(enabled)
    fun toggleDataSync(enabled: Boolean)        = settingsRepository.setDataSyncEnabled(enabled)

    fun logout(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            
            // Sign out from Google to clear the cached account
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            val googleSignInClient = GoogleSignIn.getClient(context, gso)
            googleSignInClient.signOut()
            
            // Clear local token and user info
            tokenManager.clearToken()
            
            // Invoke the callback to trigger navigation
            onComplete()
        }
    }

    fun saveProfile(name: String, dob: String, gender: String) {
        settingsRepository.saveProfile(name, dob, gender)
    }

    fun triggerManualSync(context: Context) {
        val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.fitness.app.network.sync.BackendSyncWorker>().build()
        androidx.work.WorkManager.getInstance(context).enqueue(syncRequest)
    }
}
