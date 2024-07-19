package com.okada.rider.android.services


import android.location.Location
import com.okada.android.data.model.AppLocation
import com.okada.android.data.model.JobInfoModel

interface JobRequestService {


    fun declineJob(
        jobId: String,
        completion: (Result<Unit>) -> Unit
    )

    fun acceptJob(
        jobId: String,
        currentDriverLocation: AppLocation,
        completion: (Result<Unit>) -> Unit
    )

    fun updateJob(
        jobId: String,
        currentDriverLocation: AppLocation,
        completion: (Result<Unit>) -> Unit
    )

    fun fetchCurrentJob(jobId: String,
                        completion: (Result<JobInfoModel>) -> Unit)

    fun removeJobListener()

}