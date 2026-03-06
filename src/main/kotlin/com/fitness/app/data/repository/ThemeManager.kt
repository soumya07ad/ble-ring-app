package com.fitness.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fitness.app.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

/**
 * Manages theme persistence using DataStore.
 * Exposes a Flow of AppTheme and provides a suspend function to update the preference.
 */
class ThemeManager(private val context: Context) {

    private val THEME_KEY = stringPreferencesKey("theme_preference")

    val themeFlow: Flow<AppTheme> = context.themeDataStore.data.map { prefs ->
        when (prefs[THEME_KEY]) {
            "LIGHT" -> AppTheme.LIGHT
            "DARK" -> AppTheme.DARK
            else -> AppTheme.SYSTEM
        }
    }

    suspend fun setTheme(theme: AppTheme) {
        context.themeDataStore.edit { prefs ->
            prefs[THEME_KEY] = theme.name
        }
    }
}
