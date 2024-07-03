package com.okada.android.ui.home

import HomeViewModelFactory
import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.SquareCap
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import com.okada.android.R
import com.okada.android.data.model.DriverRequestModel
import com.okada.android.data.model.SelectedPlaceModel
import com.okada.android.databinding.FragmentHomeBinding
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.annotations.AfterPermissionGranted
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.IOException
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment(), OnMapReadyCallback, EasyPermissions.PermissionCallbacks,
    View.OnClickListener {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var mMap: GoogleMap
    private lateinit var declineView: Chip
    private lateinit var jobView: CardView
    private lateinit var estimatedDistanceTxtView: TextView
    private lateinit var estimatedTimeTextView: TextView
    private lateinit var circularProgressBar: CircularProgressBar
    private var _binding: FragmentHomeBinding? = null
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var driverRequestModel: DriverRequestModel? = null
    private lateinit var valueAnimator: ValueAnimator
    private var requestObservable: Disposable? = null

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
        homeViewModel =
            ViewModelProvider(this, HomeViewModelFactory()).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        initViews()
        init()
        return root
    }

    private fun initViews() {
        declineView = binding.chipDecline
        jobView = binding.layoutAccept
        estimatedDistanceTxtView = binding.textEstimatedDistance
        estimatedTimeTextView = binding.textEstimatedTime
        circularProgressBar = binding.circularProgressbar

        declineView.setOnClickListener(this)
    }

    private fun init() {
        //set google map api key
        homeViewModel.setGoogleApiKey(resources.getString(R.string.GOOGLE_MAPS_API_KEY))

        // Create the observer which updates the UI.
        homeViewModel.showSnackbarMessage.observe(viewLifecycleOwner,
            Observer { newMessage ->
                newMessage?.let { message ->
                    mapFragment.view?.let {
                        Snackbar.make(it, message, Snackbar.LENGTH_LONG).show()
                    }
                }
            })

        homeViewModel.updateMap.observe(viewLifecycleOwner,
            Observer { newPos ->
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPos, 18f));
            })

        homeViewModel.updateMapWithPlace.observe(viewLifecycleOwner,
            Observer { model ->
                drawPath(model)
            })

        homeViewModel.activeJobRxd.observe(viewLifecycleOwner,
            Observer { ifRxd ->
                if (ifRxd) {
                    jobRequestShowPath()
                }
            })

        // The google map builder
        locationRequest = LocationRequest.Builder(10000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMinUpdateDistanceMeters(50f)
            .setMinUpdateIntervalMillis(5000)
            .build() //If a location is available sooner by another app then use it

        // Adding a location callback for the google map
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                try {
                    homeViewModel.updateLocation(locationResult.lastLocation, requireContext())
                } catch (e: IOException) {
                    e.message?.let {
                        Snackbar.make(requireView(), "IOException: $it", Snackbar.LENGTH_LONG)
                            .show()
                    }
                }
            }
        }
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.myLooper()
            )
            fetchLastLocation()
        } else {
            Log.i("App_Info", "onResume  NO permissions")
        }
        //registerOnlineSystem()
    }

    override fun onPause() {
        super.onPause()
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onStop() {
        super.onStop()
        if (EventBus.getDefault().hasSubscriberForEvent(SelectedPlaceModel::class.java))
            EventBus.getDefault().removeStickyEvent(SelectedPlaceModel::class.java)
        EventBus.getDefault().unregister(this)
        valueAnimator.end()
        valueAnimator.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        homeViewModel.clearMessage()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeLocation()

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        try {
            val success = googleMap.setMapStyle(context?.let {
                MapStyleOptions.loadRawResourceStyle(
                    it, R.raw.maps_style
                )
            })
            //googleMap.setMapStyle(null)
            if (!success) {
                Log.e("App_Error", "Style parsing error")
            } else {
                Log.e("App_Success", "Map loaded!")
                appRequiresPermission()
                fetchLastLocation()
            }

        } catch (e: Resources.NotFoundException) {
            Log.e("App_Error", e.message.toString())
        }
    }


    private fun processMapAfterPermissions() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            mMap.setOnMyLocationButtonClickListener {
                fetchLastLocation()
                return@setOnMyLocationButtonClickListener true
            }

        }

    }

    private fun fetchLastLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient
                .lastLocation
                .addOnFailureListener { e ->
                    Toast.makeText(
                        requireContext(),
                        "Error: $e", Toast.LENGTH_SHORT
                    ).show();
                }.addOnSuccessListener { lastLocation ->
                    homeViewModel.updateLocation(lastLocation, requireContext())
                }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

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
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.rationale_message_for_location),
                REQUEST_LOCATION_PERMISSION,
                ACCESS_FINE_LOCATION
            )

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

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onDriverRequest(jobId: String) {
        if (homeViewModel.hasJob()) {
            homeViewModel.declineOtherJob(jobId)
        } else {
            homeViewModel.setActiveJob(jobId)
            homeViewModel.retrieveActiveJob()
        }
    }

    fun jobRequestShowPath() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationProviderClient
                .lastLocation
                .addOnFailureListener { e ->
                    Toast.makeText(
                        requireContext(),
                        "Error: $e", Toast.LENGTH_SHORT
                    ).show();
                }.addOnSuccessListener { lastLocation ->
                   homeViewModel.calculatePath(lastLocation)
                }
        }
    }

    fun removeLocation() {
        homeViewModel.removeUserLocation()
    }

    private fun drawPath(model: SelectedPlaceModel) {
        var blackPolyLine: Polyline? = null
        var greyPolyLine: Polyline? = null
        var polylineList: List<LatLng>? = null
        var polylineOptions: PolylineOptions? = null
        var blackPolyLineOptions: PolylineOptions? = null

        polylineList = model.polylineList
        polylineOptions = PolylineOptions()
        polylineOptions?.color(Color.GRAY)
        polylineOptions?.width(12f)
        polylineOptions?.startCap(SquareCap())
        polylineOptions?.jointType(JointType.ROUND)
        polylineList?.asIterable()?.let { iterable ->
            polylineOptions?.addAll(iterable)
        }
        polylineOptions?.let { options ->
            greyPolyLine = mMap.addPolyline(options)
        }

        blackPolyLineOptions = PolylineOptions()
        blackPolyLineOptions?.color(Color.BLACK)
        blackPolyLineOptions?.width(5f)
        blackPolyLineOptions?.startCap(SquareCap())
        blackPolyLineOptions?.jointType(JointType.ROUND)
        polylineList?.asIterable()?.let { iterable ->
            blackPolyLineOptions?.addAll(iterable)
        }
        blackPolyLineOptions?.let { options ->
            blackPolyLine = mMap.addPolyline(options)
        }

        //Animation
        valueAnimator = ValueAnimator.ofInt(0, 100)
        valueAnimator.duration = 1100
        valueAnimator.repeatCount = ValueAnimator.INFINITE
        valueAnimator.interpolator = LinearInterpolator()
        valueAnimator.addUpdateListener { value ->
            val points = greyPolyLine!!.points
            val percentValue = value.animatedValue.toString().toInt()
            val size = points.size
            val newPoints = (size * (percentValue / 100.0f)).toInt()
            val p = points.subList(0, newPoints)
            blackPolyLine!!.points = p
        }
        valueAnimator.start()

        val latLngBound = LatLngBounds.Builder().include(model.eventOrigin!!)
            .include(model.eventDest!!)
            .build()
        //Add icon for origin
        model.eventDest?.let { dest ->
            mMap.addMarker(
                MarkerOptions().position(dest)
                    .icon(BitmapDescriptorFactory.defaultMarker()).title("Pickup Location")
            )
        }
        // Populate views
        estimatedTimeTextView.text = model.boundedTime
        estimatedDistanceTxtView.text = model.distance

        val cameraUpdate = CameraUpdateFactory
            .newLatLngBounds(latLngBound, 100)
        // moveCamera instead of animateCamera
        mMap.moveCamera(cameraUpdate)
        mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.cameraPosition!!.zoom - 1))

        //Set layout visibility
        declineView.visibility = View.VISIBLE
        jobView.visibility = View.VISIBLE

        //Countdown timer animation
        requestObservable = Observable.interval(100, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { x ->
                circularProgressBar.progress += 1f
            }
            .takeUntil { aLong -> aLong == "100".toLong() }
            .doOnComplete {
                Snackbar.make(requireView(), "Accept Request", Snackbar.LENGTH_LONG)
                    .show()
            }.subscribe()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.chip_decline -> {
                if (homeViewModel.hasJob()) {
                    declineView.visibility = View.GONE
                    jobView.visibility = View.GONE
                    requestObservable?.dispose()
                    circularProgressBar.progress = 0f
                    homeViewModel.declineActiveJob()
                }
            }
        }
    }


}