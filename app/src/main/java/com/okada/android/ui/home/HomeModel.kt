package com.okada.android.ui.home

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.okada.android.data.model.DriverRequestModel
import com.okada.android.data.model.JobInfoModel
import com.okada.android.data.model.UserInfo

class HomeModel {
    var uid: String? = null // driver's uid
    var lastLocation: Location? = null
    var apiKey: String = ""
    var driverRequestModel: DriverRequestModel? = null
    var curentJobInfo: JobInfoModel? = null
    var currentJobClient: UserInfo? = null
    var acceptJob: Boolean = false

    fun hasAcceptedJob(): Boolean  {
        return curentJobInfo != null && acceptJob
    }
}