package com.okada.rider.android.services

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.okada.android.data.model.JobInfoModel
import com.okada.android.data.model.enum.JobStatus


class JobRequestServiceImpl : JobRequestService {
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val jobsRef: DatabaseReference = database.getReference("Jobs")
    private val jobListener: ChildEventListener? = null
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
        completion: (Result<Unit>) -> Unit
    ) {
        jobsRef.child(jobId).child("status").setValue(JobStatus.ACCEPTED).addOnCompleteListener {
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
        }
    }
}