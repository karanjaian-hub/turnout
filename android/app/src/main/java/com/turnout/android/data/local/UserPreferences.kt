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
    val onboardingCompleted: Flow<Boolean> = dataStore.data.map { it[KEY_ONBOARDING_COMPLETED] ?: false }

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
    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    // Draft event storage, scoped per user — key is built dynamically since it embeds
    // userId rather than being a single fixed constant like the other keys above.
    fun getDraftEvent(userId: Long): Flow<String?> =
        dataStore.data.map { it[stringPreferencesKey("draft_event_$userId")] }

    suspend fun saveDraftEvent(userId: Long, json: String) {
        dataStore.edit { it[stringPreferencesKey("draft_event_$userId")] = json }
    }

    suspend fun clearDraftEvent(userId: Long) {
        dataStore.edit { it.remove(stringPreferencesKey("draft_event_$userId")) }
    }

    // AI result cache, scoped per feature (e.g. "description", "rsvp_insights") — value
    // is a JSON blob of { result, timestamp }, deserialized by AiViewModel. Each feature
    // caches independently so switching between them doesn't lose earlier results.
    fun getAiResult(featureKey: String): Flow<String?> =
        dataStore.data.map { it[stringPreferencesKey("ai_result_$featureKey")] }

    suspend fun saveAiResult(featureKey: String, json: String) {
        dataStore.edit { it[stringPreferencesKey("ai_result_$featureKey")] = json }
    }

    val mpesaPhoneNumber: Flow<String?> = dataStore.data.map { it[KEY_MPESA_PHONE] }

    suspend fun saveMpesaPhoneNumber(phoneNumber: String) {
        dataStore.edit { it[KEY_MPESA_PHONE] = phoneNumber }
    }

    val fcmToken: Flow<String?> = dataStore.data.map { it[KEY_FCM_TOKEN] }

    suspend fun saveFcmToken(token: String) {
        dataStore.edit { it[KEY_FCM_TOKEN] = token }
    }

    companion object {
        private val KEY_USERNAME  = stringPreferencesKey("username")
        private val KEY_FULL_NAME = stringPreferencesKey("full_name")
        private val KEY_THEME     = stringPreferencesKey("theme")
        private val KEY_BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_MPESA_PHONE = stringPreferencesKey("mpesa_phone_number")
        private val KEY_FCM_TOKEN = stringPreferencesKey("fcm_token")
    }
}
