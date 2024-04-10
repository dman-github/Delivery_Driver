package com.okada.android.services

import com.okada.android.data.model.LoggedInUser

interface AccountService {
    fun authenticate(email: String, password: String, completion: (Result<LoggedInUser>) -> Unit)
    fun createUser(email: String, password: String, completion: (Result<LoggedInUser>) -> Unit)
    fun isUserLoggedIn(completion: (Result<Boolean>) -> Unit)
    fun getLoggedInUser(completion: (Result<LoggedInUser>) -> Unit)
    fun logout(completion: (Result<Unit>) -> Unit )

    fun getPushNotificationToken(completion: (Result<String>) -> Unit)


}