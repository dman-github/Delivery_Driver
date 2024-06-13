
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.okada.android.data.LocationUsecase
import com.okada.android.services.LocationServiceImpl
import com.okada.android.ui.home.HomeViewModel
import com.okada.android.data.AccountUsecase
import com.okada.android.data.DirectionsUsecase
import com.okada.android.services.AccountServiceImpl

/**
 * ViewModel provider factory to instantiate LoginViewModel.
 * Required given LoginViewModel has a non-empty constructor
 */
class HomeViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                accountUsecase = AccountUsecase(
                    accountService = AccountServiceImpl()
                ),
                locationUsecase = LocationUsecase(
                    locationService = LocationServiceImpl()
                ),
                directionsUsecase = DirectionsUsecase()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}