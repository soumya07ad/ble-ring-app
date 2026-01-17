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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fitness_auth")

class TokenManager(private val context: Context) {
    
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("auth_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val PHONE_KEY = stringPreferencesKey("phone_number")
    }
    
    // Get token as Flow for reactive updates
    val tokenFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }
    
    // Get token synchronously - not recommended, use tokenFlow instead
    suspend fun getToken(): String? {
        var result: String? = null
        context.dataStore.data.collect { preferences ->
            result = preferences[TOKEN_KEY]
        }
        return result
    }
    
    // Save token
    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
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
        }
    }
    
    // Check if user is logged in
    fun isLoggedInFlow(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        !preferences[TOKEN_KEY].isNullOrEmpty()
    }
}
