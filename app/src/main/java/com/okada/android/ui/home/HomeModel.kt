package com.okada.android.ui.home

import com.google.android.gms.maps.model.LatLng
import com.okada.android.data.model.DriverRequestModel

class HomeModel {
    var uid: String? = null
    var lastLocation: LatLng? = null
    var apiKey: String = ""
    var driverRequestModel: DriverRequestModel? = null
}