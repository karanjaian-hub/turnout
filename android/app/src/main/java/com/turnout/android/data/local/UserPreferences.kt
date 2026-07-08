package com.turnout.android.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension property creates a single DataStore instance per process
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    val username: Flow<String> = dataStore.data.map { it[KEY_USERNAME] ?: "" }
    val fullName: Flow<String> = dataStore.data.map { it[KEY_FULL_NAME] ?: "" }
    val theme: Flow<String>    = dataStore.data.map { it[KEY_THEME] ?: "system" }
    val biometricEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_BIOMETRIC] ?: false }

    suspend fun saveUserProfile(username: String, fullName: String) {
        dataStore.edit {
            it[KEY_USERNAME]  = username
            it[KEY_FULL_NAME] = fullName
        }
    }

    suspend fun setTheme(theme: String) {
        dataStore.edit { it[KEY_THEME] = theme }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_BIOMETRIC] = enabled }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    companion object {
        private val KEY_USERNAME  = stringPreferencesKey("username")
        private val KEY_FULL_NAME = stringPreferencesKey("full_name")
        private val KEY_THEME     = stringPreferencesKey("theme")
        private val KEY_BIOMETRIC = booleanPreferencesKey("biometric_enabled")
    }
}
