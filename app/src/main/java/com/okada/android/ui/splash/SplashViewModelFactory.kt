package com.okada.android.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.okada.android.data.AccountUsecase
import com.okada.android.data.ProfileUsecase
import com.okada.android.data.SignupUsecase
import com.okada.android.services.AccountServiceImpl
import com.okada.android.services.DataServiceImpl


/**
 * ViewModel provider factory to instantiate LoginViewModel.
 * Required given LoginViewModel has a non-empty constructor
 */
class SplashViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SplashViewModel::class.java)) {
            return SplashViewModel(
                accUsecase = AccountUsecase(
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