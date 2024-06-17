package com.okada.android.services

interface DriverRequestService {

    fun declineRouteRequest(
        driverUid: String,
        clientPushToken: String,
        completion: (Result<Unit>) -> Unit)
}