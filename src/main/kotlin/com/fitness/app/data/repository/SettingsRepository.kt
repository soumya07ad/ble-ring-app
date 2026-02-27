package com.fitness.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.fitness.app.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) : ISettingsRepository {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("fitness_settings", Context.MODE_PRIVATE)

    companion object {
        const val PREF_NOTIFICATIONS    = "pref_notifications"
        const val PREF_METRIC_UNITS     = "pref_metric_units"
        const val PREF_BEDTIME_REMINDER = "pref_bedtime_reminder"
        const val PREF_DATA_SYNC        = "pref_data_sync"
        const val PREF_USER_NAME        = "pref_user_name"
        const val PREF_USER_DOB         = "pref_user_dob"
        const val PREF_USER_GENDER      = "pref_user_gender"
    }

    // ── Toggle preferences ─────────────────────────────────────────

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean(PREF_NOTIFICATIONS, true))
    override val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _metricUnitsEnabled = MutableStateFlow(prefs.getBoolean(PREF_METRIC_UNITS, true))
    override val metricUnitsEnabled: StateFlow<Boolean> = _metricUnitsEnabled.asStateFlow()

    private val _bedtimeReminderEnabled = MutableStateFlow(prefs.getBoolean(PREF_BEDTIME_REMINDER, false))
    override val bedtimeReminderEnabled: StateFlow<Boolean> = _bedtimeReminderEnabled.asStateFlow()

    private val _dataSyncEnabled = MutableStateFlow(prefs.getBoolean(PREF_DATA_SYNC, true))
    override val dataSyncEnabled: StateFlow<Boolean> = _dataSyncEnabled.asStateFlow()

    override fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_NOTIFICATIONS, enabled).apply()
        _notificationsEnabled.value = enabled
    }
    override fun setMetricUnitsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_METRIC_UNITS, enabled).apply()
        _metricUnitsEnabled.value = enabled
    }
    override fun setBedtimeReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_BEDTIME_REMINDER, enabled).apply()
        _bedtimeReminderEnabled.value = enabled
    }
    override fun setDataSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_DATA_SYNC, enabled).apply()
        _dataSyncEnabled.value = enabled
    }

    // ── Profile preferences ────────────────────────────────────────

    private val _userName = MutableStateFlow(prefs.getString(PREF_USER_NAME, "") ?: "")
    override val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userDob = MutableStateFlow(prefs.getString(PREF_USER_DOB, "") ?: "")
    override val userDob: StateFlow<String> = _userDob.asStateFlow()

    private val _userGender = MutableStateFlow(prefs.getString(PREF_USER_GENDER, "") ?: "")
    override val userGender: StateFlow<String> = _userGender.asStateFlow()

    override fun saveProfile(name: String, dob: String, gender: String) {
        prefs.edit()
            .putString(PREF_USER_NAME, name.trim())
            .putString(PREF_USER_DOB, dob.trim())
            .putString(PREF_USER_GENDER, gender)
            .apply()
        _userName.value   = name.trim()
        _userDob.value    = dob.trim()
        _userGender.value = gender
    }
}
