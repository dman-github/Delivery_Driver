package com.okada.android.ui.home

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.okada.android.Common
import com.okada.android.data.LocationUsecase
import com.okada.android.data.AccountUsecase
import com.okada.android.data.DirectionsUsecase
import com.okada.android.data.JobRequestUsecase
import com.okada.android.data.ProfileUsecase
import com.okada.android.data.model.SelectedPlaceModel

class HomeViewModel(
    private val accountUsecase: AccountUsecase,
    private val locationUsecase: LocationUsecase,
    private val directionsUsecase: DirectionsUsecase,
    private val profileUsecase: ProfileUsecase,
    private val jobRequestUsecase: JobRequestUsecase
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

    private val _activeJobRxd = MutableLiveData<Boolean>()
    val activeJobRxd: LiveData<Boolean> = _activeJobRxd

    private val _acceptedJob = MutableLiveData<Boolean>()
    val acceptedJob: LiveData<Boolean> = _acceptedJob

    private val _declinedJob = MutableLiveData<Boolean>()
    val declinedJob: LiveData<Boolean> = _declinedJob

    private val _arrivedAtPickup = MutableLiveData<Boolean>()
    val arrivedAtPickup: LiveData<Boolean> = _arrivedAtPickup


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

    fun setActiveJob(jobId: String) {
        jobRequestUsecase.setCurrentJobId(jobId)
    }

    fun hasJob(): Boolean {
        return jobRequestUsecase.hasActiveJob
    }


    fun updateLocation(location: Location?, context: Context) {
        location?.let { location ->
            val newPos = LatLng(location.latitude, location.longitude)
            _model.uid?.also { uid ->
                // Here if we have accepted a job then the location is updated only in the Active job
                if (_model.hasAcceptedJob()) {
                    jobRequestUsecase.updateJobRequest(location) { result ->
                        result.fold(onSuccess = { job ->
                            _model.curentJobInfo = job
                            if (!_model.arrivalNotificationSent) {
                                compareDistanceToDest(
                                    newPos,
                                    LatLng(
                                        job.jobDetails!!.pickupLocation!!.latitude!!,
                                        job.jobDetails!!.pickupLocation!!.longitude!!
                                    )
                                )
                            }
                        }, onFailure = {
                            // Error occurred
                            _showSnackbarMessage.value = "Cannot update job: " + it.message
                        })
                    }
                } else {
                    locationUsecase.updateLocation(uid, location, context) { result ->
                        result.onSuccess {
                            _model.lastLocation = location
                            _updateMap.value = newPos
                            _showSnackbarMessage.value =
                                "Location updated\nLat: ${location.latitude}, Lon: ${location.longitude}}"
                        }
                        result.onFailure {
                            _showSnackbarMessage.value = it.message
                        }
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

    fun calculatePath(driverLocation: Location, forPickup: Boolean) {
        _model.curentJobInfo?.jobDetails?.pickupLocation?.let { pickupLocation ->
            _model.curentJobInfo?.jobDetails?.deliveryLocation?.let { deliveryLocation ->
                val pickupAddress = _model.curentJobInfo?.jobDetails?.pickupAddress ?: ""
                val deliveryAddress = _model.curentJobInfo?.jobDetails?.deliverAddress ?: ""
                var addressStr= if (forPickup) pickupAddress else deliveryAddress
                var endLocation = if (forPickup) pickupLocation else deliveryLocation
                val requestedLocationStr =
                    StringBuilder().append(endLocation.latitude).append(",")
                        .append(endLocation.longitude).toString()
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
                            placeModel.forPickup = forPickup
                            placeModel.endAddress = addressStr
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
        }
    }

    fun declineActiveJob() {
        if (jobRequestUsecase.hasActiveJob) {
            jobRequestUsecase.declineActiveJobRequest { result ->
                result.fold(onSuccess = {
                    // Push done
                    _showSnackbarMessage.value = "active request declined"
                    _declinedJob.value = true
                }, onFailure = {
                    // Error occurred
                    _showSnackbarMessage.value = it.message
                })
            }
        }
    }

    fun declineOtherJob(jobId: String) {
        jobRequestUsecase.declineJobRequest(jobId) { result ->
            result.fold(onSuccess = {
                // Push done
                _showSnackbarMessage.value = "request declined"
            }, onFailure = {
                // Error occurred
                _showSnackbarMessage.value = it.message
            })
        }
    }

    fun retrieveActiveJob() {
        jobRequestUsecase.fetchJobRequest() { result ->
            result.fold(onSuccess = { jobRequestModel ->
                profileUsecase.fetchClientInfo(jobRequestModel.clientUid!!) { result ->
                    result.fold(onSuccess = { userInfoModel ->
                        _model.currentJobClient = userInfoModel
                        _model.curentJobInfo = jobRequestModel
                        _activeJobRxd.value = true
                    }, onFailure = {
                        // Error occurred
                        _showSnackbarMessage.value = "Client information not found"
                    })
                }
            }, onFailure = {
                // Error occurred
                _showSnackbarMessage.value = "Job information not found $it.message"
            })
        }
    }

    fun acceptActiveJob() {
        _model.lastLocation?.let { loc ->
            _model.uid?.let { uid ->
                jobRequestUsecase.acceptJobRequest(loc) { result ->
                    result.fold(onSuccess = {
                        _model.acceptJob = true
                        _acceptedJob.value = true
                        locationUsecase.removeLocationFor(uid)
                    }, onFailure = {
                        // Error occurred
                        _showSnackbarMessage.value = "Job information not found $it.message"
                    })
                }
            }
        }
    }

    private fun compareDistanceToDest(newPos: LatLng,
                                      destination: LatLng) {
        var distance = FloatArray(3) //0 is distance, //1 is start bearing , //2 is end bearing
        Location.distanceBetween(newPos.latitude, newPos.longitude,
            destination.latitude, destination.longitude, distance)
        Log.i("App_Info", "HomeViewModel compareDistanceToDest: Dis is ${distance[0]}")
        if (distance[0] <= Common.MIN_DISTANCE_TO_DESIRED_LOCATION) {
            // Send a push notification to the client that the driver has arrived
            _model.curentJobInfo?.clientUid?.let { clientUid ->
                _model.uid?.let { driverUid ->
                    jobRequestUsecase.sendDriverArrivedRequest(driverUid, clientUid) {result->
                        result.fold(onSuccess = {
                            _model.arrivalNotificationSent = true
                            _arrivedAtPickup.value = true
                        }, onFailure = {
                            // Error occurred
                            _showSnackbarMessage.value = "Cannot send arrival notification to client: $it.message"
                        })
                    }
                }
            }
        }
    }

}