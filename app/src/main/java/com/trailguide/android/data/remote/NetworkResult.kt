package com.trailguide.android.data.remote

/**
 * Sealed class representing the result of a network operation.
 * Provides type-safe handling of success, error, and loading states.
 */
sealed class NetworkResult<out T> {
    /**
     * Successful network response with data.
     */
    data class Success<T>(val data: T) : NetworkResult<T>()
    
    /**
     * Network error with message.
     */
    data class Error(val message: String, val exception: Exception? = null) : NetworkResult<Nothing>()
    
    /**
     * Loading state for in-progress requests.
     */
    object Loading : NetworkResult<Nothing>()
}

/**
 * Extension function to handle network responses safely.
 */
suspend fun <T> safeApiCall(apiCall: suspend () -> retrofit2.Response<T>): NetworkResult<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                NetworkResult.Success(body)
            } else {
                NetworkResult.Error("Empty response body")
            }
        } else {
            val errorMessage = response.errorBody()?.string() ?: "Unknown error"
            NetworkResult.Error("HTTP ${response.code()}: $errorMessage")
        }
    } catch (e: Exception) {
        NetworkResult.Error(
            message = e.message ?: "Network request failed",
            exception = e
        )
    }
}

