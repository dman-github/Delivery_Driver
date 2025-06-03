package com.okada.android.data.model.enum

sealed class AppException(message: String) : Exception(message) {
    class NetworkException(message: String = "A network error occurred.") : AppException(message)
    class DatabaseException(message: String = "A database error occurred.") : AppException(message)
    class AuthenticationException(message: String = "Authentication failed.") : AppException(message)
    class Empty (message: String = "There is no matching data in the databse"): AppException(message)
    class UnknownException(message: String = "An unknown error occurred.") : AppException(message)
}