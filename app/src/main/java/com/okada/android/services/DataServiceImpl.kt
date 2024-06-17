package com.okada.android.services

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.okada.android.data.model.DriverInfo
import com.okada.android.data.model.TokenModel

class DataServiceImpl: DataService {
    private val databaseRefUser = FirebaseDatabase.getInstance().getReference("DriverInfo")
    private val pushTokenRef = FirebaseDatabase.getInstance().getReference("PushTokens")
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

    override fun updatePushMessagingToken(uid: String, tokenModel: TokenModel, completion: (Result<Unit>) -> Unit) {
        pushTokenRef.child(uid)
            .setValue(tokenModel)
            .addOnFailureListener {e ->
                completion(Result.failure(e))
            }.addOnSuccessListener {
                completion(Result.success(Unit))
            }
    }

    override fun retrievePushMessagingToken(uid: String, listener: ValueEventListener) {
        pushTokenRef.child(uid).addListenerForSingleValueEvent(listener)
    }
}