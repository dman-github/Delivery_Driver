package com.okada.android

import com.okada.android.Model.DriverInfoModel

object Common {
    fun buildWelcomeMessage(): String {
        return StringBuilder("Welcome, ")
            .append(currentUser!!.firstName)
            .append(" ")
            .append(currentUser!!.lastName)
            .toString()
    }

    var currentUser: DriverInfoModel? = null
    val DRIVER_INFO_REFERENCE = "DriverInfo"
    var DRIVER_LOCATION_REFERENCE = "DriverLocation"
}
