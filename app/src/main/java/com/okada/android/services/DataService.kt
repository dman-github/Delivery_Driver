package com.okada.android.services

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.ValueEventListener
import com.okada.android.data.model.DriverInfo
import com.okada.android.data.model.TokenModel

interface DataService {
    fun checkIfUserInfoExists(uid: String, listener: ValueEventListener)
    fun createUserInfo(uid: String, driverInfo: DriverInfo,
                       failureListener: OnFailureListener,
                       successListener: OnSuccessListener<Void>)

    fun updatePushMessagingToken(uid: String, tokenModel: TokenModel, completion: (Result<Unit>) -> Unit)
}