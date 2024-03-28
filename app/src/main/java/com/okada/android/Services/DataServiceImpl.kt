package com.okada.rider.android.services

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.okada.rider.android.data.model.DriverInfo

class DataServiceImpl: DataService {
    private val databaseRefUser = FirebaseDatabase.getInstance().getReference("DriverInfo")
    override fun checkIfUserInfoExists(uid: String, listener: ValueEventListener) {
        // Set up Firebase listener
        databaseRefUser.child(uid).addListenerForSingleValueEvent(listener)
    }

    override fun createUserInfo(uid: String, driverInfo: DriverInfo,
                                failureListener: OnFailureListener,
                                successListener: OnSuccessListener<Void>) {
        databaseRefUser.child(uid)
            .setValue(driverInfo)
            .addOnFailureListener(failureListener)
            .addOnSuccessListener(successListener)
    }
}