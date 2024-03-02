package com.okada.android.ui.home

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.okada.android.Common
import com.okada.android.R
import com.okada.android.databinding.FragmentHomeBinding
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.annotations.AfterPermissionGranted
import com.vmadalin.easypermissions.dialogs.SettingsDialog

class HomeFragment : Fragment(), OnMapReadyCallback, EasyPermissions.PermissionCallbacks {
    private lateinit var mMap: GoogleMap
    private var _binding: FragmentHomeBinding? = null
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // Online database
    private lateinit var onlineRef: DatabaseReference
    private lateinit var currentUserRef: DatabaseReference
    private lateinit var driverLocationRef: DatabaseReference
    private lateinit var geoFire: GeoFire
    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 0x2233
    }
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        init()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return root
    }

    private fun init() {

        // Setting up Driver location DB
        onlineRef = FirebaseDatabase.getInstance().reference.child(".info/connected")
        driverLocationRef = FirebaseDatabase.getInstance().getReference(Common.DRIVER_LOCATION_REFERENCE)
        FirebaseAuth.getInstance().currentUser?.uid?.let {
            currentUserRef = FirebaseDatabase.getInstance().getReference(Common.DRIVER_LOCATION_REFERENCE).child(it)
        }
        geoFire = GeoFire(driverLocationRef)
        registerOnlineSystem()

        // The google map builder
        locationRequest = LocationRequest.Builder(5000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMinUpdateDistanceMeters(10f)
            .setMinUpdateIntervalMillis(3000).build()

        // Adding a location callback for the google map

        locationCallback = object: LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.latitude?.let {lat ->
                    locationResult.lastLocation?.longitude?.let {long ->
                        val newPos = LatLng(lat,long)
                        Log.i("App_Info", "on:locationCallback Lat: $lat, Lon: $long")
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos,18f))

                        //Update location in driver db
                        FirebaseAuth.getInstance().currentUser?.uid?.let {
                            geoFire.setLocation(it,
                                GeoLocation(lat,long)) { _: String?, error: DatabaseError? ->
                                    if (error != null) {
                                        mapFragment.view?.let {view -> Snackbar.make(view, error.message, Snackbar.LENGTH_LONG).show() }
                                    } else {
                                        mapFragment.view?.let {view -> Snackbar.make(view, "Location updated", Snackbar.LENGTH_LONG).show() }
                                    }
                                }
                        }
                    }
                }
            }
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
    }

    private val onlineValueEventListener = object: ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists()) {
                currentUserRef.onDisconnect().removeValue()
            }
        }

        override fun onCancelled(error: DatabaseError) {
            mapFragment.view?.let { Snackbar.make(it, error.message, Snackbar.LENGTH_LONG).show() }
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {

            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
        } else {
            Log.i("App_Info", "onResume  NO permissions")
        }
        registerOnlineSystem()
    }
    override fun onPause() {
        super.onPause()
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        FirebaseAuth.getInstance().currentUser?.uid.let {
            geoFire.removeLocation(it)
        }
        onlineRef.removeEventListener(onlineValueEventListener)

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        try {
            val success = googleMap.setMapStyle(context?.let {
                MapStyleOptions.loadRawResourceStyle(
                    it, R.raw.maps_style)
            })
            //googleMap.setMapStyle(null)
            if (!success) {
                Log.e("App_Error", "Style parsing error")
            } else {
                Log.e("App_Success", "Map loaded!")
                appRequiresPermission()
                fetchLastLocation()
            }

        } catch (e:Resources.NotFoundException) {
            Log.e("App_Error", e.message.toString())
        }
    }

    private fun registerOnlineSystem() {
        onlineRef.addValueEventListener(onlineValueEventListener)
    }

    private fun processMapAfterPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            mMap.setOnMyLocationButtonClickListener {
                fetchLastLocation()
                return@setOnMyLocationButtonClickListener true
            }

        }

    }

    private fun fetchLastLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient
                .lastLocation
                .addOnFailureListener { e ->
                    Toast.makeText(
                        requireContext(),
                        "Error: $e", Toast.LENGTH_SHORT
                    ).show();
                }.addOnSuccessListener { lastLocation ->
                    lastLocation?.latitude?.let { lat ->
                        lastLocation?.longitude?.let { long ->
                            val newPos = LatLng(lat, long)
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos, 18f))
                        }
                    }
                }
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

        Log.i("App_Info", "onRequestPermissionsResult permissions count: ${permissions.size}")
        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    @AfterPermissionGranted(REQUEST_LOCATION_PERMISSION)
    private fun appRequiresPermission() {
        Log.i("App_Info", "methodRequiresPermission called")
        if (EasyPermissions.hasPermissions(requireContext(), ACCESS_FINE_LOCATION)) {
            // Already have permission
            Log.i("App_Info", "ALREADY permissions granted!")
            processMapAfterPermissions()
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this,
                getString(R.string.rationale_message_for_location),
                REQUEST_LOCATION_PERMISSION,
                ACCESS_FINE_LOCATION)

        }
    }

    // Easy Permissions callbacks
    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        // Some permissions have been granted
        Log.i("App_Info", "onPermissionsGranted")
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        // Some permissions have been denied
        Log.i("App_Info", "permissions denied!")

        // Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            SettingsDialog.Builder(requireActivity()).build().show()
        } else {
            appRequiresPermission()
        }
    }


}