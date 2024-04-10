
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.okada.android.Common
import com.okada.android.R
import com.okada.android.data.AccountUsecase
import com.okada.android.data.ProfileUsecase
import com.okada.android.data.model.DriverInfo
import com.okada.android.ui.register.RegisterFormState
import com.okada.android.ui.register.RegisterResult

class RegisterViewModel(private val accountUsecase: AccountUsecase,
                        private val profileUsecase: ProfileUsecase
) : ViewModel() {

    private val _registerForm = MutableLiveData<RegisterFormState>()
    val registerFormState: LiveData<RegisterFormState> = _registerForm

    private val _registerResult = MutableLiveData<RegisterResult>()
    val registerResult: LiveData<RegisterResult> = _registerResult

    fun fetchUserData() {
        accountUsecase.getLoggedInUser { result ->
            result.fold(onSuccess = { user ->
                _registerForm.value =
                    RegisterFormState(emailAddress = user.email)
                //profileUsecase.

            }, onFailure = {
                _registerResult.value = RegisterResult(errorMsg = it.message)
            })
        }
    }

    fun register(
        firstname: String,
        lastname: String,
        biometricId: String
    ) {
        accountUsecase.loggedInUser?.let {user->
            profileUsecase.createUserInfo(firstname, lastname, biometricId, user) { result ->
                result.fold(onSuccess = {
                    var model = DriverInfo()
                    model.firstname = firstname
                    model.lastname = lastname
                    model.biometricId
                    model.email = user.email
                    Common.currentUser = model
                    _registerResult.value =
                        RegisterResult(navigateToHome = true)

                }, onFailure = {
                    _registerResult.value = RegisterResult(errorMsg = it.message)
                })
            }
        }

    }

    fun dataChanged(firstname: String, surname: String) {
        var formState = RegisterFormState()
        var valid = true
        if (!isStringValid(firstname)) {
            formState.firstNameError = R.string.invalid_string
            valid = false
        }
        if (!isStringValid(surname)) {
            formState.surnameError = R.string.invalid_string
            valid = false
        }
        if (firstname.isNotBlank() &&
            surname.isNotBlank() && valid
        ) {
            formState.isDataValid = true
        }
        _registerForm.value = formState
    }


    // A placeholder string validation check
    private fun isStringValid(str: String): Boolean {
        // has no number characters
        return !str.matches(Regex(".*\\d.*"))
        // contains number characters
    }
}