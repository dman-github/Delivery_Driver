package com.okada.android.Services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.okada.android.Common
import com.okada.android.Utils.UserUtils
import java.util.Random

class FirebaseMessagingIdService: FirebaseMessagingService() {


    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        data?.let {
            Common.showNotification(this, Random().nextInt(), data[Common.NOTI_TITLE], data[Common.NOTI_BODY],null)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        FirebaseAuth.getInstance().currentUser?.let { UserUtils.updateToken(this,token) }
    }


}