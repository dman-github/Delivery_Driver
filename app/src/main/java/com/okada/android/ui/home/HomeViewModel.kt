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
import com.okada.android.data.DirectionsUsecase
import com.okada.android.data.DriverRequestUsecase
import com.okada.android.data.model.DriverRequestModel
import com.okada.android.data.model.SelectedPlaceModel

class HomeViewModel(
    private val accountUsecase: AccountUsecase,
    private val locationUsecase: LocationUsecase,
    private val directionsUsecase: DirectionsUsecase,
    private val driverRequestUsecase: DriverRequestUsecase
) : ViewModel() {
    private val _model = HomeModel()

    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text

    private val _showSnackbarMessage = MutableLiveData<String?>()
    val showSnackbarMessage: LiveData<String?> = _showSnackbarMessage

    private val _updateMap = MutableLiveData<LatLng>()
    val updateMap: LiveData<LatLng> = _updateMap


    private val _updateMapWithPlace = MutableLiveData<SelectedPlaceModel>()
    val updateMapWithPlace: LiveData<SelectedPlaceModel> = _updateMapWithPlace


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
    fun setGoogleApiKey(key: String) {
        _model.apiKey = key
    }

    fun clearMessage() {
        _showSnackbarMessage.value = null
    }

    fun setDriverRequest(req: DriverRequestModel) {
        _model.driverRequestModel = req
    }

    fun getDriverRequest(): DriverRequestModel? {
        return _model.driverRequestModel
    }

    fun updateLocation(location: Location?, context: Context) {
        location?.let { location ->
            val newPos = LatLng(location.latitude, location.longitude)
            _model.uid?.also { uid ->
                locationUsecase.updateLocation(uid, location, context) { result ->
                    result.onSuccess {
                        _model.lastLocation = newPos
                        _updateMap.value = newPos
                        _showSnackbarMessage.value =
                            "Location updated\nLat: ${location.latitude}, Lon: ${location.longitude}}"
                    }
                    result.onFailure {
                        _showSnackbarMessage.value = it.message
                    }
                }
            } ?: run {
                _showSnackbarMessage.value = "No logged in user"
            }
        }
    }

    fun removeUserLocation() {
        _model.uid?.also { uid ->
            locationUsecase.removeLocationFor(uid)
        } ?: run {
            _showSnackbarMessage.value = "No logged in user"
        }
    }

    fun calculatePath(requestedLocationStr: String, driverLocation: Location) {
        val driverLocationStr = StringBuilder().append(driverLocation.latitude).append(",")
            .append(driverLocation.longitude).toString()
        //fetch directions between the 2 points from the Google directions api
        directionsUsecase.getDirections(
            driverLocationStr,
            requestedLocationStr,
            _model.apiKey
        ) { result ->
            result.onSuccess { placeModel ->
                try {
                    placeModel.eventOrigin =
                        LatLng(driverLocation.latitude, driverLocation.longitude)
                    placeModel.eventDest = LatLng(
                        requestedLocationStr.split(",")[0].toDouble(),
                        requestedLocationStr.split(",")[1].toDouble()
                    )
                    _updateMapWithPlace.value = placeModel
                } catch (e: Exception) {
                    _showSnackbarMessage.value = e.message
                }
            }
            result.onFailure {
                _showSnackbarMessage.value = it.message
            }
        }
    }

    fun declineRequest() {
        _model.uid?.let{driverUid->
            _model.driverRequestModel?.clientKey?.let {clientUid->
                driverRequestUsecase.declineRouteRequest(driverUid, clientUid) { result ->
                    result.fold(onSuccess = {
                        // Push done
                        _showSnackbarMessage.value = "request declined"
                    }, onFailure = {
                        // Error occurred
                        _showSnackbarMessage.value = it.message
                    })
                }
            }
        }

    }

}