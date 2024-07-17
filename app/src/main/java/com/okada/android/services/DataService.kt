package com.okada.android.services

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.ValueEventListener
import com.okada.android.data.model.DriverInfo
import com.okada.android.data.model.TokenModel

interface DataService {
    fun checkIfDriverInfoExists(uid: String, listener: ValueEventListener)

    fun fetchUserInfo(uid: String, listener: ValueEventListener)
    fun createDriverInfo(uid: String, driverInfo: DriverInfo,
                       failureListener: OnFailureListener,
                       successListener: OnSuccessListener<Void>)

    fun updatePushMessagingToken(uid: String, tokenModel: TokenModel, completion: (Result<Unit>) -> Unit)

    fun retrievePushMessagingToken(uid: String, listener: ValueEventListener)
}