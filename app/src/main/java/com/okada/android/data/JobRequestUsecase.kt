package com.okada.android.data

import android.location.Location
import com.okada.android.data.model.AppLocation
import com.okada.android.data.model.JobInfoModel
import com.okada.rider.android.services.JobRequestService

class JobRequestUsecase(
    val jobRequestService: JobRequestService
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

    fun acceptJobRequest(location: Location, completion: (Result<JobInfoModel>) -> Unit) {
        activeJobId?.let { jobId ->
            // Driver is changed to the new driver
            jobRequestService.acceptJob(jobId,
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
}