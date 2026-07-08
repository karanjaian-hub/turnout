package com.turnout.android.data.remote.interceptor

import com.turnout.android.core.utils.AppEvent
import com.turnout.android.core.utils.AppEventBus
import com.turnout.android.data.local.TokenManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TokenRefreshInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    // A separate client with NO auth interceptor — prevents infinite loop:
    // if we used the main client here, a failed refresh would trigger another refresh, forever.
    @Named("refreshClient") private val refreshClient: OkHttpClient,
    @Named("baseUrl") private val baseUrl: String
) : Interceptor {

    // Guards against concurrent refresh races: if two requests 401 at nearly the same time,
    // only one actually calls /refresh — the second waits, then reuses whatever token the
    // first one obtained, instead of both racing the backend and possibly invalidating
    // each other's refresh token.
    private val refreshMutex = Mutex()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)

        if (response.code != 401) return response

        val refreshToken = tokenManager.getRefreshToken()
            ?: return forceLogoutAndReturn(response)

        // Close the 401 response body before making the refresh call —
        // OkHttp enforces this to avoid connection leaks
        response.close()

        val newAccessToken = runBlocking {
            refreshMutex.withLock {
                // Re-check inside the lock: another thread may have already refreshed
                // while we were waiting, in which case tokenManager already has a fresh
                // token and we can skip a redundant network call entirely.
                val currentToken = tokenManager.getAccessToken()
                if (currentToken != null && currentToken != extractTokenFromRequest(originalRequest)) {
                    currentToken
                } else {
                    tryRefresh(refreshToken)
                }
            }
        }

        return if (newAccessToken != null) {
            val retryRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $newAccessToken")
                .build()
            chain.proceed(retryRequest)
        } else {
            forceLogoutAndReturn(response)
        }
    }

    private fun extractTokenFromRequest(request: Request): String? =
        request.header("Authorization")?.removePrefix("Bearer ")

    private fun tryRefresh(refreshToken: String): String? {
        return try {
            val refreshRequest = Request.Builder()
                .url("${baseUrl}api/auth/refresh")
                .post(ByteArray(0).toRequestBody(null))
                .header("Authorization", "Bearer $refreshToken")
                .build()

            val refreshResponse = refreshClient.newCall(refreshRequest).execute()
            if (!refreshResponse.isSuccessful) return null

            val body = refreshResponse.body?.string() ?: return null
            val json = JSONObject(body)
            val newAccessToken = json.getString("accessToken")
            tokenManager.updateAccessToken(newAccessToken)

            // Some backends rotate the refresh token on every use, invalidating the old one.
            // If the response includes a new one, save it — otherwise the next refresh
            // attempt would send an already-invalidated token and fail unexpectedly.
            if (json.has("refreshToken")) {
                tokenManager.updateRefreshToken(json.getString("refreshToken"))
            }

            newAccessToken
        } catch (e: Exception) {
            null
        }
    }

    private fun forceLogoutAndReturn(response: Response): Response {
        tokenManager.clearTokens()
        // runBlocking is acceptable here — we're on OkHttp's IO thread, not the main thread
        runBlocking { AppEventBus.emit(AppEvent.Logout) }
        return response
    }
}
