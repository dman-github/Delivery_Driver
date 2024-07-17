package com.okada.android.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.okada.android.data.model.LoggedInUser
import com.okada.android.data.model.DriverInfo
import com.okada.android.data.model.TokenModel
import com.okada.android.data.model.UserInfo
import com.okada.android.services.DataService
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

        dataService.checkIfDriverInfoExists(user.userId, object : ValueEventListener {
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

    fun createDriverInfo(
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
        dataService.createDriverInfo(user.userId, driverInfo,
            failureListener = { exception ->
                completion(Result.failure(exception))
            }, {
                completion(Result.success(Unit))
            })

    }

    fun fetchClientInfo(clientId: String, completion: (Result<UserInfo?>) -> Unit) {
        dataService.checkIfDriverInfoExists(clientId, object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Get Post object and use the values to update the UI
                val userInfo = dataSnapshot.getValue<UserInfo>()
                completion(Result.success(userInfo))
            }

            override fun onCancelled(databaseError: DatabaseError) {
                completion(Result.failure(databaseError.toException()))
            }
        })
    }

    fun sendPushNotificationToken(uid: String, tokenM: TokenModel, completion: (Result<Unit>) -> Unit) {
        dataService.updatePushMessagingToken(uid, tokenM, completion)
    }

}