package com.dexterous.flutterlocalnotifications

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationManagerCompat

import com.dexterous.flutterlocalnotifications.models.NotificationDetails
import com.dexterous.flutterlocalnotifications.utils.StringUtils
import com.google.gson.reflect.TypeToken

/**
 * Created by michaelbui on 24/3/18.
 */

class ScheduledNotificationReceiver : BroadcastReceiver() {


    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = NotificationManagerCompat.from(context)
        val notificationDetailsJson = intent.getStringExtra(FlutterLocalNotificationsPlugin.NOTIFICATION_DETAILS)
        val repeat = intent.getBooleanExtra(FlutterLocalNotificationsPlugin.REPEAT, false)

        // TODO: remove this branching logic as it's legacy code to fix an issue where notifications weren't reporting the correct time
        if (StringUtils.isNullOrEmpty(notificationDetailsJson)) {
            val notification = intent.getParcelableExtra<Notification>(FlutterLocalNotificationsPlugin.NOTIFICATION)
            notification.`when` = System.currentTimeMillis()
            val notificationId = intent.getIntExtra(FlutterLocalNotificationsPlugin.NOTIFICATION_ID,
                    0)
            notificationManager.notify(notificationId, notification)
            if (repeat) {
                return
            }
            FlutterLocalNotificationsPlugin.removeNotificationFromCache(notificationId, context)
        } else {
            val gson = FlutterLocalNotificationsPlugin.buildGson()
            val type = object : TypeToken<NotificationDetails>() {

            }.type
            val notificationDetails = gson.fromJson<NotificationDetails>(notificationDetailsJson, type)
            FlutterLocalNotificationsPlugin.showNotification(context, notificationDetails)
            if (repeat) {
                return
            }
            FlutterLocalNotificationsPlugin.removeNotificationFromCache(notificationDetails.id, context)
        }

    }

}
