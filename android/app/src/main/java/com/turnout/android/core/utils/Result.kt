package com.turnout.android.core.utils

/**
 * Wraps every repository response so ViewModels never deal with raw exceptions.
 * Loading is a separate UI state — not represented here.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int? = null) : Result<Nothing>()
}

/** Runs [block] and wraps the outcome in Result — no try-catch boilerplate in callers. */
suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: retrofit2.HttpException) {
        Result.Error(
            message = e.response()?.errorBody()?.string() ?: "Server error",
            code = e.code()
        )
    } catch (e: java.io.IOException) {
        // Network unavailable, DNS failure, socket timeout
        Result.Error(message = "No internet connection")
    } catch (e: Exception) {
        Result.Error(message = e.localizedMessage ?: "Unexpected error")
    }
}
