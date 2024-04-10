package com.okada.android.ui.signup

import com.okada.android.ui.login.LoggedInUserView


data class SignupResult(
    val success: LoggedInUserView? = null,
    val navigateToRegister: Boolean? = false,
    val errorMsg: String? = null,
    val stringResource: Int? = null
)