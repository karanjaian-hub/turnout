package com.turnout.android.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Access token is in-memory only — never persisted.
    // If the process dies, the user re-authenticates (or TokenRefreshInterceptor handles it).
    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken = _accessToken.asStateFlow()

    // EncryptedSharedPreferences wraps AES-256-GCM encryption transparently.
    // MasterKey is backed by Android Keystore — keys never leave secure hardware.
    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "turnout_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        _accessToken.value = accessToken
        encryptedPrefs.edit().putString(KEY_REFRESH_TOKEN, refreshToken).apply()
    }

    fun updateAccessToken(newToken: String) {
        _accessToken.value = newToken
    }

    // Synchronous getter — most call sites use the accessToken StateFlow, but interceptors
    // running on OkHttp's background thread sometimes need a plain snapshot value instead.
    fun getAccessToken(): String? = _accessToken.value

    // Separate from saveTokens(access, refresh) — used specifically when a refresh response
    // rotates the refresh token itself (returns a new one alongside the new access token),
    // without touching the access token update path that already happens separately.
    fun updateRefreshToken(newRefreshToken: String) {
        encryptedPrefs.edit().putString(KEY_REFRESH_TOKEN, newRefreshToken).apply()
    }

    fun getRefreshToken(): String? = encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)

    fun clearTokens() {
        _accessToken.value = null
        encryptedPrefs.edit().remove(KEY_REFRESH_TOKEN).apply()
    }

    fun isLoggedIn(): Boolean = getRefreshToken() != null

    companion object {
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
