package com.okada.android.ui.home

import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.okada.android.Common

class HomeViewModel : ViewModel() {
    private val _model = HomeModel()

    // Online database
    private lateinit var onlineRef: DatabaseReference
    private lateinit var currentUserRef: DatabaseReference
    private lateinit var driverLocationRef: DatabaseReference
    private lateinit var geoFire: GeoFire

    private val _text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
    val text: LiveData<String> = _text

    val showSnackbarMessage: MutableLiveData<String?> by lazy {
        MutableLiveData<String?>()
    }
    val updateMap: MutableLiveData<LatLng> by lazy {
        MutableLiveData<LatLng>()
    }


    init {
        _model.uid = FirebaseAuth.getInstance().currentUser?.uid
        setupDataBase()
    }

    fun clearMessage() {
        showSnackbarMessage.postValue(null)
    }
    fun updateLocation(location: Location?) {
        location?.let { location ->
            val newPos = LatLng(location.latitude, location.longitude)
            _model.lastLocation = newPos
            Log.i(
                "App_Info",
                "on:locationCallback Lat: $location.latitude, Lon: $location.longitude"
            )
            updateMap.postValue(newPos)
            //Update location in driver db
            _model.uid?.let {
                geoFire.setLocation(
                    it,
                    GeoLocation(location.latitude, location.longitude)
                ) { _: String?, error: DatabaseError? ->
                    if (error != null) {
                        showSnackbarMessage.postValue(error.message)
                    } else {
                        showSnackbarMessage.postValue("Location updated\nLat: ${location.latitude}, Lon: ${location.longitude}}")
                    }
                }
            }
        }
    }

    fun removeUserLocation() {
        _model.uid?.let {
            geoFire.removeLocation(it)
        }
    }
    private fun setupDataBase() {
        // Setting up Driver location DB
        onlineRef = FirebaseDatabase.getInstance().reference.child(".info/connected")
        driverLocationRef =
            FirebaseDatabase.getInstance().getReference(Common.DRIVER_LOCATION_REFERENCE)
        _model.uid?.let {
            currentUserRef =
                FirebaseDatabase.getInstance().getReference(Common.DRIVER_LOCATION_REFERENCE)
                    .child(it)
        }
        geoFire = GeoFire(driverLocationRef)
    }


    private fun registerOnlineSystem() {
        onlineRef.addValueEventListener(onlineValueEventListener)
    }

    private val onlineValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists()) {
                currentUserRef.onDisconnect().removeValue()
            }
        }

        override fun onCancelled(error: DatabaseError) {
            showSnackbarMessage.postValue(error.message)
        }
    }

}