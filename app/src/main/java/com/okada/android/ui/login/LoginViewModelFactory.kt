package com.okada.android.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.okada.android.data.AccountUsecase
import com.okada.android.data.ProfileUsecase
import com.okada.android.services.AccountServiceImpl
import com.okada.android.services.DataServiceImpl

/**
 * ViewModel provider factory to instantiate LoginViewModel.
 * Required given LoginViewModel has a non-empty constructor
 */
class LoginViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(
                loginRepository = AccountUsecase(
                    accountService = AccountServiceImpl()
                ),
                profileUsecase = ProfileUsecase(
                    dataService = DataServiceImpl()
                )
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}