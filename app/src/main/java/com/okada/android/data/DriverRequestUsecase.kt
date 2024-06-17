package com.okada.android.data

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.okada.android.data.model.TokenModel
import com.okada.android.services.DataService
import com.okada.android.services.DriverRequestService

class DriverRequestUsecase(
    val driverRequestService: DriverRequestService,
    val dataService: DataService
) {
    fun declineRouteRequest(
        driverUid: String,
        clientUid: String,
        completion: (Result<Unit>) -> Unit
    ) {
        dataService.retrievePushMessagingToken(clientUid, object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.hasChildren()) {

                } else {
                    snapshot.getValue(TokenModel::class.java)?.let { model ->
                        driverRequestService.declineRouteRequest(
                            model.token,
                            driverUid
                        ) { result ->
                            result.fold(onSuccess = {
                                // Notification sent
                                completion(Result.success(Unit))
                            }, onFailure = {
                                // Error occurred
                                completion(Result.failure(it))
                            })
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                completion(Result.failure(error.toException()))
            }
        })
    }
}