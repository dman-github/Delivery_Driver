package com.okada.android.services

import android.content.Context
import android.location.Location

interface LocationService {

    fun setupDatabase(uid: String)

    fun updateLocation(
        uid:String,
        newLocation: Location,
        context: Context,
        completion: (Result<Unit>) -> Unit)

    fun removeLocationfor(uid: String)
}