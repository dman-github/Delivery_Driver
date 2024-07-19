package com.okada.android.data.model

import com.okada.android.data.model.enum.JobStatus


data class JobInfoModel(
    var driverUid: String? = null,
    var clientUid: String? = null,
    var status: JobStatus? = null,
    var jobDetails: JobDetails? = null
) {
    // Null default values create a no-argument default constructor, which is needed
    // for deserialization from a DataSnapshot.
}

data class JobDetails(
    var type: String? = null,
    var info: String? = null,
    var pickupLocation: AppLocation? = null,
    var deliveryLocation: AppLocation? = null,
    var driverLocation: AppLocation? = null
)


data class AppLocation (
    var latitude: Double? = null,
    var longitude: Double? = null
)


