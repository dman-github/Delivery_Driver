package com.okada.rider.android.services

import android.location.Location
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.okada.android.data.model.AppLocation
import com.okada.android.data.model.JobInfoModel
import com.okada.android.data.model.enum.AppException
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
                addJobStatusListener(jobId, listener)
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

    override fun getActiveJobsforDriver(
        driverUid: String,
        loc: Location,
        listener: ValueEventListener,
        completion: (Result<Pair<String, JobInfoModel>>) -> Unit
    ) {
        Log.i("App_Info", "Going to call api!")
        // Get list of values that match the driverUid
        jobsRef.orderByChild("driverUid").equalTo(FirebaseAuth.getInstance().currentUser?.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val matchingJobs = mutableListOf<Pair<String, JobInfoModel>>()
                    // Search through them to find jobs that are currently Active
                    for (jobSnapshot in snapshot.children) {
                        val jobInfo = jobSnapshot.getValue(JobInfoModel::class.java)
                        if (jobInfo != null && jobInfo.status?.isActiveJob() == true) {
                            val jobKey = jobSnapshot.key ?: continue
                            matchingJobs.add(Pair(jobKey, jobInfo))
                            Log.i("App_Info", "Job found : ${jobKey}")
                        }
                    }
                    if (matchingJobs.size > 0) {
                        // Choose the first, if we have many matches , i.e active jobs, we have a problem
                        // We have to prevent the driver from accepting many jobs somehow
                        val activejob = matchingJobs.first()
                        val jobId = activejob.first
                        // Update the Job with the driver's latest location and add the listener
                        // to the status field
                        addJobStatusListener(jobId, listener)
                        updateJob(
                            jobId,
                            AppLocation(loc.latitude, loc.longitude)
                        ) { result ->
                            result.fold(onSuccess = {
                                completion(Result.success(activejob))
                            }, onFailure = {
                                // Error occurred
                                completion(Result.failure(it))
                            })
                        }
                    } else {
                        completion(Result.failure(AppException.Empty()))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    completion(Result.failure(Exception("Cannot fetch Job: ${error} for  ${FirebaseAuth.getInstance().currentUser?.uid}")))
                }
            })
    }

    private fun addJobStatusListener(jobId: String, listener: ValueEventListener) {
        // remove any listeners already added
        removeJobListener()
        // Add an event listener for the job object
        newJobRef = jobsRef.child(jobId)
        newJobRef.addValueEventListener(listener)
        jobListener = listener
    }

    override fun removeJobListener() {
        jobListener?.let { listener ->
            newJobRef.removeEventListener(listener)
            Log.i("App_Info", "JobReqestService Remove listener")
        }
    }
}