package com.okada.rider.android.services

import android.location.Location
import android.util.Log
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.okada.android.data.model.AppLocation
import com.okada.android.data.model.JobInfoModel
import com.okada.android.data.model.enum.JobStatus


class JobRequestServiceImpl : JobRequestService {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val jobsRef: DatabaseReference = database.getReference("Jobs")
    private var jobListener: ValueEventListener? = null
    private lateinit var newJobRef: DatabaseReference

    override fun declineJob(
        jobId: String,
        completion: (Result<Unit>) -> Unit
    ) {
        jobsRef.child(jobId).child("status").setValue(JobStatus.DECLINED).addOnCompleteListener {
            if (it.isSuccessful) {
                completion(Result.success(Unit))
            } else {
                it.exception?.also { exception ->
                    completion(Result.failure(exception))
                } ?: run {
                    completion(Result.failure(Exception("Cannot update Job")))
                }
            }
        }
    }


    override fun acceptJob(
        jobId: String,
        listener: ValueEventListener,
        currentDriverLocation: AppLocation,
        completion: (Result<Unit>) -> Unit
    ) {
        val values: MutableMap<String, Any> = HashMap()
        values["jobDetails/driverLocation"] = currentDriverLocation
        values["status"] = JobStatus.ACCEPTED.toString()
        jobsRef.child(jobId).updateChildren(values).addOnCompleteListener {
            if (it.isSuccessful) {
                // remove any listeners already added
                removeJobListener()
                // Add an event listener for the job object
                newJobRef = jobsRef.child(jobId)
                newJobRef.addValueEventListener(listener)
                jobListener = listener
                completion(Result.success(Unit))
                Log.i("App_Info", "JobReqestService Impl acceptJob, ${newJobRef}")
            } else {
                it.exception?.also { exception ->
                    completion(Result.failure(exception))
                } ?: run {
                    completion(Result.failure(Exception("Cannot accept Job")))
                }
            }
        }
    }

    override fun updateJobStatus(
        jobId: String,
        jobStatus: JobStatus,
        completion: (Result<Unit>) -> Unit
    ) {
        val values: MutableMap<String, Any> = HashMap()
        values["status"] = jobStatus.toString()
        jobsRef.child(jobId).updateChildren(values).addOnCompleteListener {
            if (it.isSuccessful) {
                completion(Result.success(Unit))
            } else {
                it.exception?.also { exception ->
                    completion(Result.failure(exception))
                } ?: run {
                    completion(Result.failure(Exception("Cannot change job status")))
                }
            }
        }
    }

    override fun updateJob(
        jobId: String,
        currentDriverLocation: AppLocation,
        completion: (Result<Unit>) -> Unit
    ) {
        val values: MutableMap<String, Any> = HashMap()
        values["jobDetails/driverLocation"] = currentDriverLocation
        jobsRef.child(jobId).updateChildren(values).addOnCompleteListener {
            if (it.isSuccessful) {
                completion(Result.success(Unit))
            } else {
                it.exception?.also { exception ->
                    completion(Result.failure(exception))
                } ?: run {
                    completion(Result.failure(Exception("Cannot update Job")))
                }
            }
        }
    }


    override fun fetchCurrentJob(
        jobId: String,
        completion: (Result<JobInfoModel>) -> Unit
    ) {
        jobsRef.child(jobId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    snapshot.getValue(JobInfoModel::class.java)?.also { job ->
                        completion(Result.success(job))
                    } ?: run {
                        completion(Result.failure(Exception("Cannot fetch Job")))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                completion(Result.failure(Exception("Cannot fetch Job")))
            }
        })
    }

    override fun removeJobListener() {
        jobListener?.let { listener ->
            newJobRef.removeEventListener(listener)
            Log.i("App_Info", "JobReqestService Remove listener")
        }
    }
}