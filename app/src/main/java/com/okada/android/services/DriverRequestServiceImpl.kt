package com.okada.android.services

import com.google.firebase.functions.FirebaseFunctions
import com.okada.android.Common

class DriverRequestServiceImpl : DriverRequestService {
    val cloudFuncRequestDriverName = "sendPN"

    override fun declineRouteRequest(
        pushToken: String,
        uid: String,
        completion: (Result<Unit>) -> Unit
    ) {
        val data = hashMapOf(
            "token" to pushToken,
            "title" to Common.DECLINE_REQUEST_MSG_TITLE,
            "body" to "This message is to check the decline request functionality",
            "clientKey" to uid,
            "pickupLoc" to ""
        )
        val functions = FirebaseFunctions.getInstance()
        functions.getHttpsCallable(cloudFuncRequestDriverName)
            .call(data)
            .addOnSuccessListener {
                completion(Result.success(Unit))
            }
            .addOnFailureListener { exp ->
                completion(Result.failure(exp))
            }
    }

    override fun sendDriverArrivalRequest(
        pushToken: String,
        uid: String,
        completion: (Result<Unit>) -> Unit
    ) {
        val data = hashMapOf(
            "token" to pushToken,
            "title" to Common.DRIVER_ARRIVED_REQUEST_MSG_TITLE,
            "body" to "Your driver has arrived",
            "clientKey" to uid,
            "pickupLoc" to ""
        )
        val functions = FirebaseFunctions.getInstance()
        functions.getHttpsCallable(cloudFuncRequestDriverName)
            .call(data)
            .addOnSuccessListener {
                completion(Result.success(Unit))
            }
            .addOnFailureListener { exp ->
                completion(Result.failure(exp))
            }
    }

    override fun sendDriverCompleteRequest(
        pushToken: String,
        uid: String,
        completion: (Result<Unit>) -> Unit
    ) {
        val data = hashMapOf(
            "token" to pushToken,
            "title" to Common.JOB_COMPLETED_REQUEST_MSG_TITLE,
            "body" to "Your driver has completed the Job",
            "clientKey" to uid,
            "pickupLoc" to ""
        )
        val functions = FirebaseFunctions.getInstance()
        functions.getHttpsCallable(cloudFuncRequestDriverName)
            .call(data)
            .addOnSuccessListener {
                completion(Result.success(Unit))
            }
            .addOnFailureListener { exp ->
                completion(Result.failure(exp))
            }
    }
}