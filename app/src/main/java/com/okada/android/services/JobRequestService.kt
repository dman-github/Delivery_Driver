package com.okada.rider.android.services


import com.okada.android.data.model.JobInfoModel

interface JobRequestService {


    fun declineJob(
        jobId: String,
        completion: (Result<Unit>) -> Unit
    )

    fun acceptJob(
        jobId: String,
        completion: (Result<Unit>) -> Unit
    )

    fun fetchCurrentJob(jobId: String,
                        completion: (Result<JobInfoModel>) -> Unit)

    fun removeJobListener()

}