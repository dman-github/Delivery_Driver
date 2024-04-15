package com.okada.android.data

import android.content.Context
import android.location.Location
import com.okada.android.services.LocationService

class LocationUsecase(val locationService: LocationService) {
    fun setupDatabase(uid: String) {
        locationService.setupDatabase(uid)
    }

    fun updateLocation(
        uid:String,
        newLocation: Location,
        context: Context,
        completion: (Result<Unit>) -> Unit
    ) {
        locationService.updateLocation(uid,newLocation,context,completion)
    }

    fun removeLocationFor(uid: String) {
        locationService.removeLocationfor(uid)
    }
}