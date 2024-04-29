package com.okada.android.services

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresApi
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.IOException
import java.util.Locale

class LocationServiceImpl : LocationService {
    // Online database
    private lateinit var databaseRefUserLocations: DatabaseReference
    private lateinit var onlineRef: DatabaseReference
    private lateinit var currentUserLocationRef: DatabaseReference
    private lateinit var geoFireRef: GeoFire

    override fun setupDatabase(uid: String) {
        // Setting up Driver location DB
        onlineRef = FirebaseDatabase.getInstance().reference.child(".info/connected")
        databaseRefUserLocations = FirebaseDatabase.getInstance().getReference("DriverLocations")
        currentUserLocationRef = databaseRefUserLocations.child(uid)
        geoFireRef = GeoFire(databaseRefUserLocations)
    }

    @Suppress("DEPRECATION")
    override fun updateLocation(
        uid: String,
        newLocation: Location,
        context: Context,
        completion: (Result<Unit>) -> Unit
    ) {
        // In Android Tiramisu onwards the getFromLocation has been depracated in favour of an async trigger function
        // instead of the in-line function
        val geoCoder = Geocoder(context, Locale.getDefault())
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            try {
                val addressList =
                    geoCoder.getFromLocation(newLocation.latitude, newLocation.longitude, 1)
                sendLocation(uid, newLocation, addressList?.firstOrNull(), completion)
            } catch (e: IOException) {
                completion(Result.failure(e))
            }
        } else {
            geoCoder.getFromLocation(
                newLocation.latitude,
                newLocation.longitude,
                1
            ) { addresses ->
                sendLocation(uid, newLocation, addresses.firstOrNull(), completion)
            }
        }

    }

    private fun sendLocation(
        uid: String,
        newLocation: Location,
        address: Address?,
        completion: (Result<Unit>) -> Unit
    ) {
        address?.let {
            val geocodeLocation = getCountryCodeComponent(address) + "_" + getGeocodeComponent(address)
            currentUserLocationRef = databaseRefUserLocations.child(geocodeLocation).child(uid)
            geoFireRef = GeoFire(databaseRefUserLocations.child(geocodeLocation))
            geoFireRef.setLocation(
                uid, GeoLocation(newLocation.latitude, newLocation.longitude)
            ) { _: String?, error: DatabaseError? ->
                if (error != null) {
                    completion(Result.failure(Exception(error.message)))
                } else {
                    completion(Result.success(Unit))
                }
            }
        }?:run {
            completion(Result.failure(Exception("Cannot find address from Geocode")))
        }
    }

    private fun getGeocodeComponent(a: Address): String {
        //locality -> adminArea is going from narrow window to a larger address window
        // if the locality is null then select sublocality e.t.`false`
        // Normally locality is the city
        return a.subAdminArea ?: a.adminArea
    }

    private fun getCountryCodeComponent(a: Address): String {
        //locality -> adminArea is going from narrow window to a larger address window
        // if the locality is null then select sublocality e.t.`false`
        // Normally locality is the city
        return a.countryCode ?: ""
    }

    override fun removeLocationfor(uid: String) {
        geoFireRef.removeLocation(uid)
    }


    private fun registerOnlineSystem() {
        onlineRef.addValueEventListener(onlineValueEventListener)
    }

    private val onlineValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists() && currentUserLocationRef != null) {
                currentUserLocationRef.onDisconnect().removeValue()
            }
        }

        override fun onCancelled(error: DatabaseError) {
        }
    }
}