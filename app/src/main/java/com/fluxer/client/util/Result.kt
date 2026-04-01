package com.fluxer.client.util

/**
 * Sealed class representing a result that can be either Success or Error.
 * Similar to Kotlin's Result but with custom error types.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()

    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error
    fun isLoading(): Boolean = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data
    fun errorOrNull(): String? = (this as? Error)?.message

    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (String) -> Unit): Result<T> {
        if (this is Error) action(message)
        return this
    }

    inline fun <R> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
            is Loading -> this
        }
    }

    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> {
        return when (this) {
            is Success -> transform(data)
            is Error -> this
            is Loading -> this
        }
    }
}

/**
 * Wrap a suspend function call in a Result
 */
suspend inline fun <T> safeCall(call: suspend () -> T): Result<T> {
    return try {
        Result.Success(call())
    } catch (e: Exception) {
        Result.Error(e.message ?: "Unknown error")
    }
}
