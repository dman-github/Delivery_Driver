package com.okada.android.services

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.okada.android.Common
import com.okada.android.Common.CLIENT_KEY
import com.okada.android.Common.NOTI_TITLE
import com.okada.android.Common.PICKUP_LOCATION
import com.okada.android.Common.REQUEST_DRIVER_MSG_TITLE
import com.okada.android.data.model.DriverRequestModel
import com.okada.android.data.model.TokenModel
import org.greenrobot.eventbus.EventBus
import java.util.Random

class FirebaseMessagingIdService : FirebaseMessagingService() {


    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        data?.let { data ->
            if (data[NOTI_TITLE].equals(REQUEST_DRIVER_MSG_TITLE)) {
                data[CLIENT_KEY]?.let { key ->
                    data[PICKUP_LOCATION]?.let { loc ->
                        EventBus.getDefault().postSticky(DriverRequestModel(key, loc))
                    }
                }
            } else {
                Common.showNotification(
                    this,
                    Random().nextInt(),
                    data[Common.NOTI_TITLE],
                    data[Common.NOTI_BODY],
                    null
                )
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val model = TokenModel()
        model.token = token
        FirebaseAuth.getInstance().currentUser?.let { user ->
            DataServiceImpl().updatePushMessagingToken(user.uid, model) { result ->
                result.fold(onSuccess = {
                    //check if the logged in user has a profile
                    Log.i("App_info", "FirebaseMessagingIdService, Token sent: $token")
                }, onFailure = {
                })
            }
        }
    }


}