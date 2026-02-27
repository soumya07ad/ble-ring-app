package com.fitness.app.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface ISettingsRepository {
    val notificationsEnabled: StateFlow<Boolean>
    val metricUnitsEnabled: StateFlow<Boolean>
    val bedtimeReminderEnabled: StateFlow<Boolean>
    val dataSyncEnabled: StateFlow<Boolean>
    val userName: StateFlow<String>
    val userDob: StateFlow<String>
    val userGender: StateFlow<String>

    fun setNotificationsEnabled(enabled: Boolean)
    fun setMetricUnitsEnabled(enabled: Boolean)
    fun setBedtimeReminderEnabled(enabled: Boolean)
    fun setDataSyncEnabled(enabled: Boolean)
    fun saveProfile(name: String, dob: String, gender: String)
}
