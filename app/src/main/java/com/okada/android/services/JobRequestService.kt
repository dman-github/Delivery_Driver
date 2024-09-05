package com.okada.rider.android.services


import android.location.Location
import com.google.firebase.database.ValueEventListener
import com.okada.android.data.model.AppLocation
import com.okada.android.data.model.JobInfoModel
import com.okada.android.data.model.enum.JobStatus

interface JobRequestService {


    fun declineJob(
        jobId: String,
        completion: (Result<Unit>) -> Unit
    )

    fun acceptJob(
        jobId: String,
        listener: ValueEventListener,
        currentDriverLocation: AppLocation,
        completion: (Result<Unit>) -> Unit
    )

    fun updateJobStatus(
        jobId: String,
        jobStatus: JobStatus,
        completion: (Result<Unit>) -> Unit
    )

    fun updateJob(
        jobId: String,
        currentDriverLocation: AppLocation,
        completion: (Result<Unit>) -> Unit
    )

    fun fetchCurrentJob(
        jobId: String,
        completion: (Result<JobInfoModel>) -> Unit
    )

    fun removeJobListener()

}