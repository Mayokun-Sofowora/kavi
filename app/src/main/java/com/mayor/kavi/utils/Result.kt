package com.mayor.kavi.utils

/**
 * Represents the outcome of an operation that can either succeed or fail.
 *
 * @param R The type of the result data in case of a successful operation.
 */
sealed class Result<out R> {

    /**
     * Represents a successful result.
     *
     * @param T The type of the result data.
     * @property data The data of the successful result.
     */
    data class Success<out T>(val data: T) : Result<T>()

    /**
     * Represents a failed result.
     *
     * @property exception The exception that caused the failure.
     */
    data class Error(val exception: Exception) : Result<Nothing>()
}
