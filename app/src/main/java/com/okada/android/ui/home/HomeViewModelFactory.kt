
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.okada.android.data.LocationUsecase
import com.okada.android.services.LocationServiceImpl
import com.okada.android.ui.home.HomeViewModel
import com.okada.android.data.AccountUsecase
import com.okada.android.data.DirectionsUsecase
import com.okada.android.data.DriverRequestUsecase
import com.okada.android.data.JobRequestUsecase
import com.okada.android.data.ProfileUsecase
import com.okada.android.services.AccountServiceImpl
import com.okada.android.services.DataServiceImpl
import com.okada.android.services.DriverRequestServiceImpl
import com.okada.rider.android.services.JobRequestServiceImpl

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
                directionsUsecase = DirectionsUsecase(),
                profileUsecase = ProfileUsecase(
                    dataService = DataServiceImpl()
                ),
                jobRequestUsecase = JobRequestUsecase(
                    jobRequestService = JobRequestServiceImpl(),
                    driverRequestService =  DriverRequestServiceImpl(),
                    dataService =  DataServiceImpl()
                )
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}