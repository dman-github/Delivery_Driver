package com.okada.android.services

interface DriverRequestService {

    fun declineRouteRequest(
        pushToken: String,
        uid: String,
        completion: (Result<Unit>) -> Unit
    )
    fun sendDriverArrivalRequest(
        pushToken: String,
        uid: String,
        completion: (Result<Unit>) -> Unit
    )

    fun sendDriverCompleteRequest(
        pushToken: String,
        uid: String,
        completion: (Result<Unit>) -> Unit
    )
}