package com.okada.android.ui.home

import android.content.Context
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.firebase.geofire.GeoFire
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DatabaseReference
import com.okada.android.data.LocationUsecase
import com.okada.android.data.AccountUsecase

class HomeViewModel(private val accountUsecase: AccountUsecase,
                    private val locationUsecase: LocationUsecase
) : ViewModel() {
    private val _model = HomeModel()

    // Online database
    private lateinit var onlineRef: DatabaseReference
    private lateinit var currentUserRef: DatabaseReference
    private lateinit var driverLocationRef: DatabaseReference
    private lateinit var geoFire: GeoFire

    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text

    private val _showSnackbarMessage = MutableLiveData<String?>()
    val showSnackbarMessage: LiveData<String?> = _showSnackbarMessage

    private val _updateMap = MutableLiveData<LatLng>()
    val updateMap: LiveData<LatLng> = _updateMap


    init {
        accountUsecase.getLoggedInUser { result ->
            result.fold(onSuccess = { user ->
                _model.uid = user.userId
                locationUsecase.setupDatabase(user.userId)
            }, onFailure = {
                _showSnackbarMessage.value = it.message
            })
        }
    }

    fun clearMessage() {
        _showSnackbarMessage.value = null
    }
    fun updateLocation(location: Location?, context: Context) {
        location?.let {location ->
            val newPos = LatLng(location.latitude, location.longitude)
            _model.uid?.also {uid->
                locationUsecase.updateLocation(uid, location, context) {result->
                    result.onSuccess {
                        _model.lastLocation = newPos
                        _updateMap.value = newPos
                        _showSnackbarMessage.value = "Location updated\nLat: ${location.latitude}, Lon: ${location.longitude}}"
                    }
                    result.onFailure {
                        _showSnackbarMessage.value = it.message
                    }
                }
            }?:run {
                _showSnackbarMessage.value = "No logged in user"
            }
        }
    }

    fun removeUserLocation() {
        _model.uid?.also {uid->
            locationUsecase.removeLocationFor(uid)
        }?:run {
            _showSnackbarMessage.value = "No logged in user"
        }
    }

}