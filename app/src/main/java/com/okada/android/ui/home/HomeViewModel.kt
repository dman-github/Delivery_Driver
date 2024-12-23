package com.okada.android.ui.home

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.okada.android.Common
import com.okada.android.data.LocationUsecase
import com.okada.android.data.AccountUsecase
import com.okada.android.data.DirectionsUsecase
import com.okada.android.data.JobRequestUsecase
import com.okada.android.data.ProfileUsecase
import com.okada.android.data.model.JobInfoModel
import com.okada.android.data.model.SelectedPlaceModel
import com.okada.android.data.model.enum.AppException
import com.okada.android.data.model.enum.JobStatus

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

    private val _jobCancelled = MutableLiveData<Boolean>()
    val jobCancelled: LiveData<Boolean> = _jobCancelled

    private val _arrivedAtPickup = MutableLiveData<Boolean>()
    val arrivedAtPickup: LiveData<Boolean> = _arrivedAtPickup

    private val _arrivedAtDropOff = MutableLiveData<Boolean>()
    val arrivedAtDropOff: LiveData<Boolean> = _arrivedAtDropOff

    private val _updateMapWithPlace = MutableLiveData<SelectedPlaceModel>()
    val updateMapWithPlace: LiveData<SelectedPlaceModel> = _updateMapWithPlace

    //private val _fetchLastLocation = MutableLiveData<Boolean>()
   // val fetchLastLocation: LiveData<Boolean> = _fetchLastLocation


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

    fun hasJob(): Boolean {
        return jobRequestUsecase.hasActiveJob
    }


    fun updateLocation(location: Location?, context: Context) {
        location?.let { location ->
            val newPos = LatLng(location.latitude, location.longitude)
            _model.uid?.also { uid ->
                // Here if we have accepted a job then the location is updated only in the Active job
                if (_model.hasAcceptedJob()) {
                    _model.lastLocation = location
                    jobRequestUsecase.updateJobRequest(location) { result ->
                        result.fold(onSuccess = { job ->
                            _model.curentJobInfo = job
                            if (!_model.arrivalNotificationSent) {
                                // Driver is travelling to the pickup location
                                // Pickup location is the destination
                                compareDistanceToDest(
                                    newPos,
                                    LatLng(
                                        job.jobDetails!!.pickupLocation!!.latitude!!,
                                        job.jobDetails!!.pickupLocation!!.longitude!!
                                    )
                                )
                            } else {
                                if (_model.jobStarted) {
                                    // Driver is travelling to the delivery location
                                    // Pickup location is the delivery location
                                    compareDistanceToDest(
                                        newPos,
                                        LatLng(
                                            job.jobDetails!!.deliveryLocation!!.latitude!!,
                                            job.jobDetails!!.deliveryLocation!!.longitude!!
                                        )
                                    )
                                }
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
                            Log.i("App_Info", "Update job with location")
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
                _model.curentJobInfo?.status?.let { status ->
                    val pickupAddress = _model.curentJobInfo?.jobDetails?.pickupAddress ?: ""
                    val deliveryAddress = _model.curentJobInfo?.jobDetails?.deliverAddress ?: ""
                    var addressStr = if (forPickup) pickupAddress else deliveryAddress
                    var endLocation = if (forPickup) pickupLocation else deliveryLocation
                    val requestedLocationStr =
                        StringBuilder().append(endLocation.latitude).append(",")
                            .append(endLocation.longitude).toString()
                    val driverLocationStr =
                        StringBuilder().append(driverLocation.latitude).append(",")
                            .append(driverLocation.longitude).toString()
                    //fetch directions between the 2 points from the Google directions api
                    Log.i("App_Info", "HomeViewModel calculate path getting directions")
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
                                placeModel.isAccepted = status.isActiveJob()
                                Log.i(
                                    "App_Info",
                                    "HomeViewModel calculate path getting directions SUCCESS"
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
            }
        }
    }

    fun declineActiveJob() {
            jobRequestUsecase.declineActiveJobRequest { result ->
                result.fold(onSuccess = {
                    // Push done
                    _showSnackbarMessage.value = "active request declined"
                    _declinedJob.value = true
                }, onFailure = {
                    _showSnackbarMessage.value = "Active job cleared: $it.message"
                })
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

    fun retrieveActiveJob(jobId: String) {
        jobRequestUsecase.setCurrentJobId(jobId)
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
                        Log.i("App_Info", "Client information not found")
                        jobRequestUsecase.clearJob()
                    })
                }
            }, onFailure = {
                // Error occurred
                _showSnackbarMessage.value = "Job information not found $it.message"
                Log.i("App_Info", "Job information not found $it.message")
                jobRequestUsecase.clearJob()
            })
        }
    }

    fun retrieveCurrentJobInProgress(currentLoc: Location,
                                     context: Context) {
        _model.uid?.also { uid ->
            jobRequestUsecase.fetchActiveJobsforDriverRequest(uid,
                currentLoc,
                jobStatusListener) { result ->
                result.fold(onSuccess = { currentJob ->
                    _model.curentJobInfo = currentJob.second
                    jobRequestUsecase.setCurrentJobId(currentJob.first)
                    _model.curentJobInfo?.status?.also { status ->
                        checkJobStatus(status)
                    }
                }, onFailure = {
                    if (it is AppException.Empty) {
                        // Driver does not have an active job so just update the location as per normal
                        updateLocation(currentLoc, context)
                    } else {
                        // Error occurred
                        _showSnackbarMessage.value = "No active jobs found found $it.message"
                        Log.i("App_Info", "No active jobs found found $it.message")
                    }
                })
            }
        } ?: run {
            _showSnackbarMessage.value = "No logged in user"
        }
    }

    fun completeActiveJob() {
        jobRequestUsecase.updateJobStatusRequest(JobStatus.COMPLETED) { result ->
            result.fold(onSuccess = { jobRequestModel ->
                _model.curentJobInfo = jobRequestModel
                _model.curentJobInfo?.clientUid?.let { clientUid ->
                    _model.uid?.let { driverUid ->
                        Log.i("App_Info", "HomeViewModel sending driver Complete push notification")
                        jobRequestUsecase.sendDriverJobCompletedRequest(driverUid, clientUid) { result ->
                            result.fold(onSuccess = {
                                // Do nothing
                            }, onFailure = {
                                // Error occurred
                                _showSnackbarMessage.value =
                                    "Cannot send complete job notification to client: $it.message"
                            })
                        }

                    }
                }
            }, onFailure = {
                // Error occurred
                _showSnackbarMessage.value = "Job information cannot be updated $it.message"
            })
        }
    }

    fun acceptActiveJob() {
        _model.lastLocation?.let { loc ->
            _model.uid?.let { uid ->
                jobRequestUsecase.acceptJobRequest(loc, jobStatusListener) { result ->
                    result.fold(onSuccess = { job ->
                        Log.i("App_Info", "Job plan accepted -> Remove location")
                        _model.curentJobInfo = job
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

    val jobStatusListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists()) {
                snapshot.getValue(JobInfoModel::class.java)?.also { job ->
                    checkJobStatusForCancellation(job.status!!)
                }
            }
        }

        override fun onCancelled(error: DatabaseError) {
            _showSnackbarMessage.value = "Error creating listener: $error"
        }
    }


    fun startActiveJob() {
        _model.jobStarted = true
        jobRequestUsecase.fetchJobRequest { result ->
            result.fold(onSuccess = { job ->
                _model.curentJobInfo = job
                // Check if job is not cancelled
                if (job.status != JobStatus.CANCELLED) {
                    jobRequestUsecase.updateJobStatusRequest(JobStatus.IN_PROGRESS) { result ->
                        result.fold(onSuccess = { job ->
                            Log.i("App_Info", "Job plan in-progress ")
                            _model.curentJobInfo = job
                        }, onFailure = {
                            // Error occurred
                            _showSnackbarMessage.value = "Job information not found $it.message"
                        })
                    }
                } else {
                    jobHasBeenCancelled()
                }
            }, onFailure = {
                // Error occurred
                _showSnackbarMessage.value = "Job information not found $it.message"
            })
        }
    }


    fun jobHasBeenCancelled() {
        _model.curentJobInfo?.let {
            _showSnackbarMessage.value =
                "Job has been cancelled by the user"
            _jobCancelled.value = true
            jobRequestUsecase.removeJobListeners()
        }
        _model.clearJobState()
    }

    private fun checkJobStatusForCancellation(jobStatus: JobStatus) {
        Log.i("App_Info", "Check job status for cancellation: $jobStatus")
        when (jobStatus) {
            JobStatus.CANCELLED -> {
                jobHasBeenCancelled()
                jobRequestUsecase.clearJob()
            }
            else -> {
                //Do nothing
            }
        }
    }

    private fun checkJobStatus(jobStatus: JobStatus) {
        Log.i("App_Info", "Check job status: $jobStatus")
        when (jobStatus) {
            JobStatus.ACCEPTED -> {
                // Driver is still getting to the pickup point
                _acceptedJob.value = true
                _activeJobRxd.value = true
            }
            JobStatus.IN_PROGRESS -> {
                // Driver has reached pickup point and started the journey
                // to the destination
            }
            else -> {
                //Do nothing
            }
        }
    }

    private fun compareDistanceToDest(
        newPos: LatLng,
        destination: LatLng
    ) {
        var distance = FloatArray(3) //0 is distance, //1 is start bearing , //2 is end bearing
        Location.distanceBetween(
            newPos.latitude, newPos.longitude,
            destination.latitude, destination.longitude, distance
        )
        Log.i("App_Info", "HomeViewModel compareDistanceToDest: Dis is ${distance[0]}")
        if (distance[0] <= Common.MIN_DISTANCE_TO_DESIRED_LOCATION) {
            // Send a push notification to the client that the driver has arrived
            _model.curentJobInfo?.clientUid?.let { clientUid ->
                _model.uid?.let { driverUid ->
                    if (!_model.jobStarted) {
                        Log.i("App_Info", "HomeViewModel sending driver arrived push notification")
                        jobRequestUsecase.sendDriverArrivedRequest(driverUid, clientUid) { result ->
                            result.fold(onSuccess = {
                                Log.i(
                                    "App_Info",
                                    "HomeViewModel sending driver arrived push notification SUCCESS"
                                )
                                _model.arrivalNotificationSent = true
                                _arrivedAtPickup.value = true
                            }, onFailure = {
                                // Error occurred
                                _showSnackbarMessage.value =
                                    "Cannot send arrival notification to client: $it.message"
                            })
                        }
                    } else {
                        Log.i(
                            "App_Info",
                            "HomeViewModel driver arrived at destination"
                        )
                        _arrivedAtDropOff.value = true
                    }
                }
            }
        }
    }


}