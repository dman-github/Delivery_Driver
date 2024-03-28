package com.okada.rider.android.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.okada.rider.android.data.model.LoggedInUser
import com.okada.rider.android.data.model.DriverInfo
import com.okada.rider.android.services.DataService
import kotlin.Result

/**
 * Class that requests authentication and user information from the remote data source and
 * maintains an in-memory cache of login status and user credentials information.
 */

class ProfileUsecase(val dataService: DataService) {

    private var profileExists: Boolean = false


    val isProfileExists: Boolean
        get() = profileExists

    fun checkProfileExists(user: LoggedInUser, completion: (Result<DriverInfo?>) -> Unit) {

        dataService.checkIfUserInfoExists(user.userId, object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get Post object and use the values to update the UI
                val driverInfo = dataSnapshot.getValue<DriverInfo>()
                profileExists = driverInfo != null
                completion(Result.success(driverInfo))
            }

            override fun onCancelled(databaseError: DatabaseError) {
                completion(Result.failure(databaseError.toException()))
            }

        })
    }

    fun createUserInfo(
        firstname: String,
        lastname: String,
        biometricId: String,
        user: LoggedInUser,
        completion: (Result<Unit>) -> Unit
    ) {
        var driverInfo = DriverInfo()
        driverInfo.firstname = firstname
        driverInfo.lastname = lastname
        driverInfo.email = user.email
        driverInfo.biometricId = biometricId
        dataService.createUserInfo(user.userId, driverInfo,
            failureListener = { exception ->
                completion(Result.failure(exception))
            }, {
                completion(Result.success(Unit))
            })

    }

}