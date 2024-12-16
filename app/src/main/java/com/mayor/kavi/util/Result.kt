package com.mayor.kavi.util

/**
 * Represents the outcome of an operation that can either succeed or fail.
 *
 * @param T The type of the result data in case of a successful operation.
 */
sealed class Result<out T> {

    /**
     * Represents a successful result.
     *
     * @param data The data of the successful result.
     */
    data class Success<out T>(val data: T) : Result<T>()

    /**
     * Represents a failed result.
     *
     * @param message The error message describing the failure.
     * @param exception The optional exception that caused the failure.
     */
    data class Error<T>(
        val message: String = "An unknown error occurred",
        val exception: Throwable? = null,
        val data: T? = null
    ) : Result<T>()

    data class Loading<out T>(val data: T? = null) : Result<T>()

}

/**
 * Helper function to get the data from a successful result or return a fallback value.
 *
 * @param fallback The fallback value if the result is not successful.
 */
fun <T> Result<T>.successOr(fallback: T): T {
    return (this as? Result.Success<T>)?.data ?: fallback
}

/**
 * Helper function to get the data from a successful result or throw an exception.
 *
 * @throws Exception If the result is not successful.
 */
val <T> Result<T>.dataOrNull: T?
    get() = when (this) {
        is Result.Success -> this.data
        is Result.Error -> this.data
        is Result.Loading -> this.data
    }

