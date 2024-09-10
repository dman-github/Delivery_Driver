package com.okada.android.data

import android.location.Location
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.okada.android.data.model.AppLocation
import com.okada.android.data.model.JobInfoModel
import com.okada.android.data.model.TokenModel
import com.okada.android.data.model.enum.JobStatus
import com.okada.android.services.DataService
import com.okada.android.services.DriverRequestService
import com.okada.rider.android.services.JobRequestService

class JobRequestUsecase(
    val jobRequestService: JobRequestService,
    val driverRequestService: DriverRequestService,
    val dataService: DataService
) {
    // in-memory cache of the currentJobId  object
    private var activeJobId: String? = null

    val hasActiveJob: Boolean
        get() = activeJobId != null


    fun setCurrentJobId(jobId: String) {
        activeJobId = jobId
    }


    fun declineJobRequest(jobId: String, completion: (Result<Unit>) -> Unit) {
        // The job request for this jobId is declined
        // This can be the current active job , or any other jobs that are received while the driver is having an active Job
        jobRequestService.declineJob(jobId) { result ->
            result.fold(onSuccess = {
                if (activeJobId == jobId) {
                    // if the active job is declined, set it to null
                    activeJobId = null
                }
                completion(Result.success(Unit))
            }, onFailure = {
                // Error occurred
                completion(Result.failure(it))
            })
        }
    }

    fun declineActiveJobRequest(completion: (Result<Unit>) -> Unit) {
        activeJobId?.let { jobId ->
            // The job request for this jobId is declined
            // This can be the current active job , or any other jobs that are received while the driver is having an active Job
            jobRequestService.declineJob(jobId) { result ->
                result.fold(onSuccess = {
                    // if the active job is declined, set it to null
                    activeJobId = null
                    completion(Result.success(Unit))
                }, onFailure = {
                    // Error occurred
                    completion(Result.failure(it))
                })
            }
        }
    }

    fun acceptJobRequest(location: Location,listener: ValueEventListener,
                         completion: (Result<JobInfoModel>) -> Unit) {
        activeJobId?.let { jobId ->
            // Driver is changed to the new driver
            jobRequestService.acceptJob(jobId,listener,
                AppLocation(location.latitude, location.longitude)) { result ->
                result.fold(onSuccess = {
                    jobRequestService.fetchCurrentJob(jobId) { result ->
                        result.fold(onSuccess = {
                            completion(Result.success(it))
                        }, onFailure = {
                            // Error occurred
                            completion(Result.failure(it))
                        })
                    }
                }, onFailure = {
                    // Error occurred
                    completion(Result.failure(it))
                })
            }
        }
    }

    fun updateJobStatusRequest(jobStatus: JobStatus, completion: (Result<JobInfoModel>) -> Unit) {
        activeJobId?.let { jobId ->
            // Driver is changed to the new driver
            jobRequestService.updateJobStatus(jobId,jobStatus) { result ->
                result.fold(onSuccess = {
                    jobRequestService.fetchCurrentJob(jobId) { result ->
                        result.fold(onSuccess = {
                            completion(Result.success(it))
                        }, onFailure = {
                            // Error occurred
                            completion(Result.failure(it))
                        })
                    }
                }, onFailure = {
                    // Error occurred
                    completion(Result.failure(it))
                })
            }
        }
    }


    fun fetchJobRequest(completion: (Result<JobInfoModel>) -> Unit) {
        activeJobId?.let { jobId ->
            // Fetch the details of th job
            jobRequestService.fetchCurrentJob(jobId) { result ->
                result.fold(onSuccess = {
                    completion(Result.success(it))
                }, onFailure = {
                    // Error occurred
                    completion(Result.failure(it))
                })
            }
        }
    }

    fun updateJobRequest(location: Location, completion: (Result<JobInfoModel>) -> Unit) {
        activeJobId?.let { jobId ->
            // Driver is changed to the new driver
            jobRequestService.updateJob(jobId,
                AppLocation(location.latitude, location.longitude)) { result ->
                result.fold(onSuccess = {
                    jobRequestService.fetchCurrentJob(jobId) { result ->
                        result.fold(onSuccess = {
                            completion(Result.success(it))
                        }, onFailure = {
                            // Error occurred
                            completion(Result.failure(it))
                        })
                    }
                }, onFailure = {
                    // Error occurred
                    completion(Result.failure(it))
                })
            }
        }
    }

    fun sendDriverArrivedRequest(
        driverUid: String,
        clientUid: String,
        completion: (Result<Unit>) -> Unit
    ) {
        dataService.retrievePushMessagingToken(clientUid, object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.hasChildren()) {
                    completion(Result.failure(Exception("No Push token")))
                } else {
                    snapshot.getValue(TokenModel::class.java)?.let { model ->
                        driverRequestService.sendDriverArrivalRequest(
                            model.token,
                            driverUid
                        ) { result ->
                            result.fold(onSuccess = {
                                // Notification sent
                                completion(Result.success(Unit))
                            }, onFailure = {
                                // Error occurred
                                completion(Result.failure(it))
                            })
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                completion(Result.failure(error.toException()))
            }
        })
    }

    fun sendDriverJobCompletedRequest(
        driverUid: String,
        clientUid: String,
        completion: (Result<Unit>) -> Unit
    ) {
        dataService.retrievePushMessagingToken(clientUid, object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.hasChildren()) {
                    completion(Result.failure(Exception("No Push token")))
                } else {
                    snapshot.getValue(TokenModel::class.java)?.let { model ->
                        driverRequestService.sendDriverCompleteRequest(
                            model.token,
                            driverUid
                        ) { result ->
                            result.fold(onSuccess = {
                                // Notification sent
                                completion(Result.success(Unit))
                            }, onFailure = {
                                // Error occurred
                                completion(Result.failure(it))
                            })
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                completion(Result.failure(error.toException()))
            }
        })
    }

    fun removeJobListeners() {
        jobRequestService.removeJobListener()
    }


}