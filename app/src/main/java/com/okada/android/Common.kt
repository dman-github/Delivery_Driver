package com.okada.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.okada.android.data.model.DriverInfo

object Common {
    fun buildWelcomeMessage(): String {
        return StringBuilder("Welcome, ")
            .append(currentUser!!.firstname)
            .append(" ")
            .append(currentUser!!.lastname)
            .toString()
    }

    fun showNotification(
        context: Context,
        id: Int,
        title: String?,
        body: String?,
        intent: Intent?
    ) {
        var pendingIntent: PendingIntent? = null
        if (intent != null) {
            pendingIntent =
                PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val NOTIFICATION_CHANNEL_ID = "okada_driver"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, "okada_driver",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.description = "okada_driver"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        builder.setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setSmallIcon(R.drawable.baseline_drive_eta_24)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.baseline_drive_eta_24
                )
            )
        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }
        val notification = builder.build()
        notificationManager.notify(id, notification)


    }

    val NOTI_BODY: String = "body"
    val NOTI_TITLE: String = "title"
    val TOKEN_REFERENCE = "PushTokens"
    var currentUser: DriverInfo? = null
    val DRIVER_INFO_REFERENCE = "DriverInfo"
    val CLIENT_KEY: String = "clientKey"
    val PICKUP_LOCATION: String = "pickupLoc"
    val REQUEST_DRIVER_MSG_TITLE: String = "Driver requested!"
    val DECLINE_REQUEST_MSG_TITLE: String = "Request Cancelled!"
    val DRIVER_ARRIVED_REQUEST_MSG_TITLE: String = "Driver has Arrived!"
    val JOB_COMPLETED_REQUEST_MSG_TITLE: String = "Job Completed!"
    val MIN_DISTANCE_TO_DESIRED_LOCATION: Int = 50
    val MAX_WAIT_TIME_IN_MINS: Int = 1
}
