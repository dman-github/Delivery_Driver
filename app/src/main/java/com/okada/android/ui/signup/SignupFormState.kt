package com.okada.android.ui.signup


data class SignupFormState(
    var usernameError: Int? = null,
    var passwordError: Int? = null,
    var isDataValid: Boolean = false
)