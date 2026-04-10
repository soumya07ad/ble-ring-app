package com.fitness.app.network.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fitness_auth")

class TokenManager(private val context: Context) {
    
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val PHONE_KEY = stringPreferencesKey("phone_number")
        private val SETUP_COMPLETE_KEY = stringPreferencesKey("setup_complete")
    }
    
    // Get token as Flow for reactive updates
    val tokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }
    
    // Get setup complete as Flow
    val setupCompleteFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SETUP_COMPLETE_KEY] == "true"
    }
    
    // Get token synchronously - not recommended, use tokenFlow instead
    suspend fun getToken(): String? {
        return context.dataStore.data.first()[TOKEN_KEY]
    }
    
    // Save token
    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }

    // Save setup status
    suspend fun setSetupComplete(complete: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SETUP_COMPLETE_KEY] = if (complete) "true" else "false"
        }
    }
    
    // Save user info
    suspend fun saveUserInfo(userId: String, phoneNumber: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[PHONE_KEY] = phoneNumber
        }
    }
    
    // Get user ID
    fun getUserIdFlow(): Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]
    }
    
    // Get phone number
    fun getPhoneFlow(): Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PHONE_KEY]
    }
    
    // Clear token (logout)
    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(PHONE_KEY)
            preferences.remove(SETUP_COMPLETE_KEY)
        }
    }
    
    // Check if user is logged in
    fun isLoggedInFlow(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        !preferences[TOKEN_KEY].isNullOrEmpty()
    }
}
