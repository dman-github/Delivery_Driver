package com.okada.android.data

import com.okada.android.data.model.JobInfoModel
import com.okada.rider.android.services.JobRequestService

class JobRequestUsecase(
    val jobRequestService: JobRequestService
) {
    // in-memory cache of the currentJobId  object
    private var currentJobId: String? = null

    val hasActiveJob: Boolean
        get() = currentJobId != null


    fun setCurrentJobId(jobId: String) {
        currentJobId = jobId
    }

    fun declineJobRequest(completion: (Result<Unit>) -> Unit) {
        currentJobId?.let { jobId ->
            // Driver is changed to the new driver
            jobRequestService.declineJob(jobId) { result ->
                result.fold(onSuccess = {
                    completion(Result.success(Unit))
                }, onFailure = {
                    // Error occurred
                    completion(Result.failure(it))
                })
            }
        }
    }

    fun acceptJobRequest(completion: (Result<JobInfoModel>) -> Unit) {
        currentJobId?.let { jobId ->
            // Driver is changed to the new driver
            jobRequestService.acceptJob(jobId) { result ->
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