package com.dexterous.flutterlocalnotifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScheduledNotificationBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        println("rebooted")
        if (action != null && action == android.content.Intent.ACTION_BOOT_COMPLETED) {
            FlutterLocalNotificationsPlugin.rescheduleNotifications(context)
        }
    }
}
