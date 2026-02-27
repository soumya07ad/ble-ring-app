package com.fitness.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fitness.app.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SettingsUiState(
    val notificationsEnabled: Boolean = true,
    val metricUnitsEnabled: Boolean = true,
    val bedtimeReminderEnabled: Boolean = false,
    val dataSyncEnabled: Boolean = true,
    val userName: String = "",
    val userDob: String = "",
    val userGender: String = ""
)

class SettingsViewModel(private val settingsRepository: ISettingsRepository) : ViewModel() {

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

    fun saveProfile(name: String, dob: String, gender: String) {
        settingsRepository.saveProfile(name, dob, gender)
    }
}
