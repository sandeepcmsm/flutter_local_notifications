package com.dexterous.flutterlocalnotifications

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.app.Person
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.RemoteViews

import com.dexterous.flutterlocalnotifications.models.NotificationDetails
import com.dexterous.flutterlocalnotifications.models.styles.*
import com.dexterous.flutterlocalnotifications.utils.BooleanUtils
import com.dexterous.flutterlocalnotifications.utils.StringUtils
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

import java.io.IOException
import java.util.ArrayList
import java.util.Calendar

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar

class FlutterLocalNotificationsPlugin private constructor(private val registrar: Registrar) : BroadcastReceiver(), MethodCallHandler, PluginRegistry.NewIntentListener {


    init {
        this.registrar.addNewIntentListener(this)

        FirebaseApp.initializeApp(registrar.context())

        defaultIconResourceId = registrar.context().resources.getIdentifier("ic_notification", "drawable", registrar.context().packageName)

        val intentFilter = IntentFilter()
        intentFilter.addAction(FlutterFirebaseMessagingService.ACTION_TOKEN)
        intentFilter.addAction(FlutterFirebaseMessagingService.ACTION_REMOTE_MESSAGE)
        val manager = LocalBroadcastManager.getInstance(registrar.context())
        manager.registerReceiver(this, intentFilter)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {

            INITIALIZE_METHOD -> {
                // initializeHeadlessService(call, result);
                initialize(call, result)
            }
            GET_NOTIFICATION_APP_LAUNCH_DETAILS_METHOD -> {
            }//                getNotificationAppLaunchDetails(result);
            SHOW_METHOD -> {
                show(call, result)
            }
            SCHEDULE_METHOD -> {
                schedule(call, result)
            }
            PERIODICALLY_SHOW_METHOD, SHOW_DAILY_AT_TIME_METHOD, SHOW_WEEKLY_AT_DAY_AND_TIME_METHOD -> {
                repeat(call, result)
            }
            CANCEL_METHOD -> cancel(call, result)
            CANCEL_ALL_METHOD -> cancelAllNotifications(result)
            FCM_CONFIGURE -> {
                FlutterFirebaseMessagingService.broadcastToken(registrar.context())
                if (registrar.activity() != null) {
                    sendMessageFromIntent("onLaunch", registrar.activity().intent)
                }
                result.success(null)
            }
            FCM_SUBSCRIBETOTOPIC -> {
                val topic = call.arguments<String>()
                FirebaseMessaging.getInstance().subscribeToTopic(topic)
                result.success(null)
            }
            FCM_UNSUBSCRIBEFROMTOPIC -> {
                val unsubscribeTopic = call.arguments<String>()
                FirebaseMessaging.getInstance().unsubscribeFromTopic(unsubscribeTopic)
                result.success(null)
            }
            FCM_GETTOKEN -> FirebaseInstanceId.getInstance()
                    .instanceId
                    .addOnCompleteListener(
                            OnCompleteListener { task ->
                                if (!task.isSuccessful) {
                                    Log.w(TAG, "getToken, error fetching instanceID: ", task.exception)
                                    result.success(null)
                                    return@OnCompleteListener
                                }

                                result.success(task.result!!.token)
                            })
            FCM_AUTOINITENABLED -> Thread(
                    Runnable {
                        try {
                            FirebaseInstanceId.getInstance().deleteInstanceId()
                            result.success(true)
                        } catch (ex: IOException) {
                            Log.e(TAG, "deleteInstanceID, error:", ex)
                            result.success(false)
                        }
                    })
                    .start()
            FCM_DELETEINSTANCEID -> result.success(FirebaseMessaging.getInstance().isAutoInitEnabled)
            FCM_SETAUTOINITENABLED -> {
                val isEnabled = call.arguments<Boolean>()
                FirebaseMessaging.getInstance().isAutoInitEnabled = isEnabled!!
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    private fun cancel(call: MethodCall, result: Result) {
        val id = call.arguments<Int>()
        cancelNotification(id)
        result.success(null)
    }

    private fun repeat(call: MethodCall, result: Result) {
        val arguments = call.arguments<Map<String, Any>>()
        val notificationDetails = extractNotificationDetails(result, arguments)
        if (notificationDetails != null) {
            repeatNotification(registrar.context(), notificationDetails, true)
            result.success(null)
        }
    }

    private fun schedule(call: MethodCall, result: Result) {
        val arguments = call.arguments<Map<String, Any>>()
        val notificationDetails = extractNotificationDetails(result, arguments)
        if (notificationDetails != null) {
            scheduleNotification(registrar.context(), notificationDetails, true)
            result.success(null)
        }
    }

    private fun show(call: MethodCall, result: Result) {
        val arguments = call.arguments<Map<String, Any>>()
        val notificationDetails = extractNotificationDetails(result, arguments)
        if (notificationDetails != null) {
            showNotification(registrar.context(), notificationDetails)
            result.success(null)
        }
    }

    private fun initialize(call: MethodCall, result: Result) {
        val arguments = call.arguments<Map<String, Any>>()
        val defaultIcon = arguments[DEFAULT_ICON] as String?

        defaultIconResourceId = registrar.context().resources.getIdentifier(defaultIcon, "drawable", registrar.context().packageName)
        if (defaultIconResourceId == 0) {
            result.error(INVALID_ICON_ERROR_CODE, String.format(INVALID_DRAWABLE_RESOURCE_ERROR_MESSAGE, defaultIcon), null)
            return
        }
        if (registrar.activity() != null) {
            sendMessageFromIntent("selectNotification", registrar.activity().intent)
        }
        result.success(true)
    }

    /// Extracts the details of the notifications passed from the Flutter side and also validates that any specified drawable/raw resources exist
    private fun extractNotificationDetails(result: Result, arguments: Map<String, Any>): NotificationDetails? {
        val notificationDetails = NotificationDetails.from(arguments)
        // validate the icon resource
        if (!setIconResourceId(registrar.context(), notificationDetails, result)) {
            return null
        }
        if (!StringUtils.isNullOrEmpty(notificationDetails.largeIcon)) {
            // validate the large icon resource
            if (notificationDetails.largeIconBitmapSource === BitmapSource.Drawable) {
                if (!isValidDrawableResource(registrar.context(), notificationDetails.largeIcon, result, INVALID_LARGE_ICON_ERROR_CODE)) {
                    return null
                }
            }
        }
        if (notificationDetails.style === NotificationStyle.BigPicture) {
            // validate the big picture resources
            val bigPictureStyleInformation = notificationDetails.styleInformation as BigPictureStyleInformation?
            if (!StringUtils.isNullOrEmpty(bigPictureStyleInformation!!.largeIcon)) {
                if (bigPictureStyleInformation.largeIconBitmapSource === BitmapSource.Drawable && !isValidDrawableResource(registrar.context(), bigPictureStyleInformation.largeIcon, result, INVALID_LARGE_ICON_ERROR_CODE)) {
                    return null
                }
            }
            if (bigPictureStyleInformation.bigPictureBitmapSource === BitmapSource.Drawable && !isValidDrawableResource(registrar.context(), bigPictureStyleInformation.bigPicture, result, INVALID_BIG_PICTURE_ERROR_CODE)) {
                return null
            }
        }
        if (!StringUtils.isNullOrEmpty(notificationDetails.sound)) {
            val soundResourceId = registrar.context().resources.getIdentifier(notificationDetails.sound, "raw", registrar.context().packageName)
            if (soundResourceId == 0) {
                result.error(INVALID_SOUND_ERROR_CODE, INVALID_RAW_RESOURCE_ERROR_MESSAGE, null)
            }
        }

        return notificationDetails
    }

    private fun cancelNotification(id: Int?) {
        val context = registrar.context()
        val intent = Intent(context, ScheduledNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, id!!, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        val alarmManager = getAlarmManager(context)
        alarmManager.cancel(pendingIntent)
        val notificationManager = getNotificationManager(context)
        notificationManager.cancel(id)
        removeNotificationFromCache(id, context)
    }

    private fun cancelAllNotifications(result: Result) {
        val context = registrar.context()
        val notificationManager = getNotificationManager(context)
        notificationManager.cancelAll()
        val scheduledNotifications = loadScheduledNotifications(context)
        if (scheduledNotifications == null || scheduledNotifications.isEmpty()) {
            result.success(null)
            return
        }

        val intent = Intent(context, ScheduledNotificationReceiver::class.java)
        for (scheduledNotification in scheduledNotifications) {
            val pendingIntent = PendingIntent.getBroadcast(context, scheduledNotification.id!!, intent, PendingIntent.FLAG_CANCEL_CURRENT)
            val alarmManager = getAlarmManager(context)
            alarmManager.cancel(pendingIntent)
        }

        saveScheduledNotifications(context, ArrayList())
        result.success(null)
    }

    override fun onNewIntent(intent: Intent): Boolean {

        val res = sendMessageFromIntent("onResume", intent)

        if (res && registrar.activity() != null) {
            registrar.activity().intent = intent
        }
        return res

    }

    // BroadcastReceiver implementation.
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        if (action == FlutterFirebaseMessagingService.ACTION_TOKEN) {
            val token = intent.getStringExtra(FlutterFirebaseMessagingService.EXTRA_TOKEN)
            channel!!.invokeMethod("onToken", token)
        } else if (action == FlutterFirebaseMessagingService.ACTION_REMOTE_MESSAGE) {
            //            RemoteMessage message =
            //                    intent.getParcelableExtra(FlutterFirebaseMessagingService.EXTRA_REMOTE_MESSAGE);
            //            Map<String, Object> content = parseRemoteMessage(message);
            //            channel.invokeMethod("onMessage", content);
        }
    }

    /**
     * @return true if intent contained a message to send.
     */
    private fun sendMessageFromIntent(method: String, intent: Intent): Boolean {

        if (CLICK_ACTION_VALUE == intent.action || CLICK_ACTION_VALUE == intent.getStringExtra("click_action")) {
            val extras = intent.extras
            val payload = extras!!.getParcelable<RemoteMessage>(PAYLOAD) ?: return false

            channel!!.invokeMethod(method, payload.data)
            return true
        } else if (CLICK_ACTION_VALUE == intent.action) {
            val payload = intent.getStringExtra(PAYLOAD)
            channel!!.invokeMethod(method, payload)
            return true
        }
        return false
    }

    companion object {
        private const val TAG = "LocalFcmNotifyPlugin"

        private const val DRAWABLE = "drawable"
        private const val DEFAULT_ICON = "defaultIcon"
        private const val SCHEDULED_NOTIFICATIONS = "scheduled_notifications"
        private const val INITIALIZE_METHOD = "initialize"
        private const val SHOW_METHOD = "show"
        private const val CANCEL_METHOD = "cancel"
        private const val CANCEL_ALL_METHOD = "cancelAll"
        private const val SCHEDULE_METHOD = "schedule"
        private const val PERIODICALLY_SHOW_METHOD = "periodicallyShow"
        private const val SHOW_DAILY_AT_TIME_METHOD = "showDailyAtTime"
        private const val SHOW_WEEKLY_AT_DAY_AND_TIME_METHOD = "showWeeklyAtDayAndTime"
        private const val GET_NOTIFICATION_APP_LAUNCH_DETAILS_METHOD = "getNotificationAppLaunchDetails"
        private const val METHOD_CHANNEL = "dexterous.com/flutter/local_notifications"
        private const val PAYLOAD = "payload"
        private const val INVALID_ICON_ERROR_CODE = "INVALID_ICON"
        private const val INVALID_LARGE_ICON_ERROR_CODE = "INVALID_LARGE_ICON"
        private const val INVALID_BIG_PICTURE_ERROR_CODE = "INVALID_BIG_PICTURE"
        private const val INVALID_SOUND_ERROR_CODE = "INVALID_SOUND"
        private const val INVALID_DRAWABLE_RESOURCE_ERROR_MESSAGE = "The resource %s could not be found. Please make sure it has been added as a drawable resource to your Android head project."
        private const val INVALID_RAW_RESOURCE_ERROR_MESSAGE = "The resource %s could not be found. Please make sure it has been added as a raw resource to your Android head project."

        var NOTIFICATION_ID = "notification_id"
        var NOTIFICATION = "notification"
        var NOTIFICATION_DETAILS = "notificationDetails"
        var REPEAT = "repeat"
        private var channel: MethodChannel? = null
        private var defaultIconResourceId = 0


        //fcm related
        private const val CLICK_ACTION_VALUE = "FLUTTER_NOTIFICATION_CLICK"
        private const val FCM_CONFIGURE = "configure"
        private const val FCM_SUBSCRIBETOTOPIC = "subscribeToTopic"
        private const val FCM_UNSUBSCRIBEFROMTOPIC = "unsubscribeFromTopic"
        private const val FCM_GETTOKEN = "getToken"
        private const val FCM_AUTOINITENABLED = "autoInitEnabled"
        private const val FCM_DELETEINSTANCEID = "deleteInstanceID"
        private const val FCM_SETAUTOINITENABLED = "setAutoInitEnabled"

        fun rescheduleNotifications(context: Context) {
            val scheduledNotifications = loadScheduledNotifications(context)
            val it = scheduledNotifications!!.iterator()
            while (it.hasNext()) {
                val scheduledNotification = it.next()
                if (scheduledNotification.repeatInterval == null) {
                    scheduleNotification(context, scheduledNotification, false)
                } else {
                    repeatNotification(context, scheduledNotification, false)
                }
            }
        }

        private fun createNotification(context: Context, notificationDetails: NotificationDetails): Notification {
            setupNotificationChannel(context, notificationDetails)
            val intent = Intent(context, getMainActivityClass(context))
            intent.action = CLICK_ACTION_VALUE
            intent.putExtra(PAYLOAD, notificationDetails.payload)

            val pendingIntent = PendingIntent.getActivity(context, notificationDetails.id!!, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            val builder = NotificationCompat.Builder(context, notificationDetails.channelId!!)
                    .setContentTitle(notificationDetails.title)
                    .setContentText(notificationDetails.body)
                    .setDefaults(Notification.DEFAULT_VIBRATE)
                    .setAutoCancel(BooleanUtils.getValue(notificationDetails.autoCancel))
                    .setContentIntent(pendingIntent)
                    .setPriority(notificationDetails.priority!!)
                    .setOngoing(BooleanUtils.getValue(notificationDetails.ongoing))
                    .setOnlyAlertOnce(BooleanUtils.getValue(notificationDetails.onlyAlertOnce))

            if (!StringUtils.isNullOrEmpty(notificationDetails.icon)) {
                builder.setSmallIcon(getDefaultResourceIconId(context))
            }

            if (notificationDetails.color != null) {
                builder.color = notificationDetails.color!!.toInt()
            } else {
                builder.color = context.resources.getColor(R.color.notification_primary)
            }


            applyGrouping(notificationDetails, builder)
            setSound(context, notificationDetails, builder)
            setVibrationPattern(notificationDetails, builder)
            setStyle(context, notificationDetails, builder)

            setProgress(notificationDetails, builder)
            return builder.build()
        }

        private fun getDefaultResourceIconId(context: Context): Int {
            return if (defaultIconResourceId == 0) {
                defaultIconResourceId = context.resources.getIdentifier("ic_notification", "drawable", context.packageName)
                defaultIconResourceId
            } else {
                defaultIconResourceId
            }
        }

        fun buildGson(): Gson {
            val styleInformationAdapter = RuntimeTypeAdapterFactory
                    .of(StyleInformation::class.java)
                    .registerSubtype(BigTextStyleInformation::class.java)
                    .registerSubtype(BigPictureStyleInformation::class.java)
                    .registerSubtype(InboxStyleInformation::class.java)
            val builder = GsonBuilder().registerTypeAdapterFactory(styleInformationAdapter)
            return builder.create()
        }

        private fun loadScheduledNotifications(context: Context): ArrayList<NotificationDetails>? {
            var scheduledNotifications = ArrayList<NotificationDetails>()
            val sharedPreferences = context.getSharedPreferences(SCHEDULED_NOTIFICATIONS, Context.MODE_PRIVATE)
            val json = sharedPreferences.getString(SCHEDULED_NOTIFICATIONS, null)
            if (json != null) {
                val gson = buildGson()
                val type = object : TypeToken<ArrayList<NotificationDetails>>() {

                }.type
                scheduledNotifications = gson.fromJson(json, type)
            }
            return scheduledNotifications
        }


        private fun saveScheduledNotifications(context: Context, scheduledNotifications: ArrayList<NotificationDetails>) {
            val gson = buildGson()
            val json = gson.toJson(scheduledNotifications)
            val sharedPreferences = context.getSharedPreferences(SCHEDULED_NOTIFICATIONS, Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString(SCHEDULED_NOTIFICATIONS, json)
            editor.apply()
        }

        /**
         * Plugin registration.
         */

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            channel = MethodChannel(registrar.messenger(), METHOD_CHANNEL)
            val plugin = FlutterLocalNotificationsPlugin(registrar)
            channel!!.setMethodCallHandler(plugin)
        }

        fun removeNotificationFromCache(notificationId: Int?, context: Context) {
            val scheduledNotifications = loadScheduledNotifications(context)
            val it = scheduledNotifications!!.iterator()
            while (it.hasNext()) {
                val notificationDetails = it.next()
                if (notificationDetails.id == notificationId) {
                    it.remove()
                    break
                }
            }
            saveScheduledNotifications(context, scheduledNotifications)
        }

        private fun scheduleNotification(context: Context, notificationDetails: NotificationDetails, updateScheduledNotificationsCache: Boolean) {
            val gson = buildGson()
            val notificationDetailsJson = gson.toJson(notificationDetails)
            val notificationIntent = Intent(context, ScheduledNotificationReceiver::class.java)
            notificationIntent.putExtra(NOTIFICATION_DETAILS, notificationDetailsJson)
            val pendingIntent = PendingIntent.getBroadcast(context, notificationDetails.id!!, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT)

            val alarmManager = getAlarmManager(context)
            alarmManager.set(AlarmManager.RTC_WAKEUP, notificationDetails.millisecondsSinceEpoch!!, pendingIntent)
            if (updateScheduledNotificationsCache) {
                val scheduledNotifications = loadScheduledNotifications(context)
                scheduledNotifications!!.add(notificationDetails)
                saveScheduledNotifications(context, scheduledNotifications)
            }
        }

        private fun repeatNotification(context: Context, notificationDetails: NotificationDetails, updateScheduledNotificationsCache: Boolean?) {
            val gson = buildGson()
            val notificationDetailsJson = gson.toJson(notificationDetails)
            val notificationIntent = Intent(context, ScheduledNotificationReceiver::class.java)
            notificationIntent.putExtra(NOTIFICATION_DETAILS, notificationDetailsJson)
            notificationIntent.putExtra(REPEAT, true)
            val pendingIntent = PendingIntent.getBroadcast(context, notificationDetails.id!!, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT)

            val alarmManager = getAlarmManager(context)
            var repeatInterval: Long = 0
            when (notificationDetails.repeatInterval) {
                RepeatInterval.EveryMinute -> repeatInterval = 60000
                RepeatInterval.Hourly -> repeatInterval = (60000 * 60).toLong()
                RepeatInterval.Daily -> repeatInterval = (60000 * 60 * 24).toLong()
                RepeatInterval.Weekly -> repeatInterval = (60000 * 60 * 24 * 7).toLong()
                else -> {
                }
            }

            var startTimeMilliseconds = notificationDetails.calledAt!!
            if (notificationDetails.repeatTime != null) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.set(Calendar.HOUR_OF_DAY, notificationDetails.repeatTime!!.hour!!)
                calendar.set(Calendar.MINUTE, notificationDetails.repeatTime!!.minute!!)
                calendar.set(Calendar.SECOND, notificationDetails.repeatTime!!.second!!)
                if (notificationDetails.day != null) {
                    calendar.set(Calendar.DAY_OF_WEEK, notificationDetails.day!!)
                }

                startTimeMilliseconds = calendar.timeInMillis
            }

            // ensure that start time is in the future
            val currentTime = System.currentTimeMillis()
            while (startTimeMilliseconds < currentTime) {
                startTimeMilliseconds += repeatInterval
            }

            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, startTimeMilliseconds, repeatInterval, pendingIntent)

            if (updateScheduledNotificationsCache!!) {
                val scheduledNotifications = loadScheduledNotifications(context)
                scheduledNotifications!!.add(notificationDetails)
                saveScheduledNotifications(context, scheduledNotifications)
            }
        }

        private fun setIconResourceId(context: Context, notificationDetails: NotificationDetails, result: Result): Boolean {
            if (notificationDetails.iconResourceId == null) {
                val resourceId: Int
                if (notificationDetails.icon != null) {
                    resourceId = context.resources.getIdentifier(notificationDetails.icon, DRAWABLE, context.packageName)
                    if (resourceId == 0) {
                        result.error(INVALID_ICON_ERROR_CODE, String.format(INVALID_DRAWABLE_RESOURCE_ERROR_MESSAGE, notificationDetails.icon), null)
                    }
                } else {
                    resourceId = defaultIconResourceId
                }
                notificationDetails.iconResourceId = resourceId
            }

            return notificationDetails.iconResourceId != 0
        }

        private fun getBitmapFromSource(context: Context, bitmapPath: String? = null, bitmapSource: BitmapSource?, image: Bitmap? = null): Bitmap? {
            var bitmap: Bitmap? = null

            when (bitmapSource) {
                BitmapSource.Drawable -> {
                    val resourceId = context.resources.getIdentifier(bitmapPath, DRAWABLE, context.packageName)
                    bitmap = BitmapFactory.decodeResource(context.resources, resourceId)
                }
                BitmapSource.FilePath -> {
                    bitmap = BitmapFactory.decodeFile(bitmapPath)
                }
                BitmapSource.Bitmap -> {
                    bitmap = image
                }
                BitmapSource.Uri -> {

                }
                else -> {

                }
            }

            return bitmap
        }

        private fun applyGrouping(notificationDetails: NotificationDetails, builder: NotificationCompat.Builder) {
            var isGrouped: Boolean? = false
            if (!StringUtils.isNullOrEmpty(notificationDetails.groupKey)) {
                builder.setGroup(notificationDetails.groupKey)
                isGrouped = true
            }

            if (isGrouped!!) {
                if (BooleanUtils.getValue(notificationDetails.setAsGroupSummary)) {
                    builder.setGroupSummary(true)
                }

                builder.setGroupAlertBehavior(notificationDetails.groupAlertBehavior!!)
            }
        }

        private fun setVibrationPattern(notificationDetails: NotificationDetails, builder: NotificationCompat.Builder) {
            if (BooleanUtils.getValue(notificationDetails.enableVibration)) {
                if (notificationDetails.vibrationPattern != null && notificationDetails.vibrationPattern!!.isNotEmpty()) {
                    builder.setVibrate(notificationDetails.vibrationPattern)
                }
            } else {
                builder.setVibrate(longArrayOf(0))
            }
        }

        private fun setSound(context: Context, notificationDetails: NotificationDetails, builder: NotificationCompat.Builder) {
            if (BooleanUtils.getValue(notificationDetails.playSound)) {
                val uri = retrieveSoundResourceUri(context, notificationDetails)
                builder.setSound(uri)
            } else {
                builder.setSound(null)
            }
        }

        private fun getMainActivityClass(context: Context): Class<*>? {
            val packageName = context.packageName
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            val className = launchIntent!!.component!!.className
            return try {
                Class.forName(className)
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
                null
            }

        }

        private fun setStyle(context: Context, notificationDetails: NotificationDetails, builder: NotificationCompat.Builder) {
            when (notificationDetails.style) {
                NotificationStyle.Default -> {
                }
                NotificationStyle.BigPicture -> setBigPictureStyle(context, notificationDetails, builder)
                NotificationStyle.BigText -> setBigTextStyle(notificationDetails, builder)
                NotificationStyle.Inbox -> setInboxStyle(notificationDetails, builder)
                NotificationStyle.Message -> setMessageStyle(context, notificationDetails, builder)
                else -> {
                }
            }
        }

        private fun restoreMessagingStyle(context: Context, notificationId: Int): NotificationCompat.MessagingStyle? {

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                        .activeNotifications
                        .find { it.id == notificationId }
                        ?.notification
                        ?.let { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it) }
            } else {
                null
            }
        }

        private fun setMessageStyle(context: Context, notificationDetails: NotificationDetails, builder: NotificationCompat.Builder) {

            val messageStyleInformation = notificationDetails.styleInformation as MessageStyleInformation?

            var message = NotificationCompat.MessagingStyle.Message(messageStyleInformation?.message,
                    messageStyleInformation?.timeStamp!!, Person.Builder().setName(messageStyleInformation.sender).build()
            )


            var messagingStyle = restoreMessagingStyle(context = context, notificationId = notificationDetails.id!!)

            if(messagingStyle == null){
                messagingStyle = NotificationCompat.MessagingStyle(Person.Builder().setName(messageStyleInformation.sender).build())
                        .addMessage(message)
            }else{
                messagingStyle.addMessage(message)
            }

            val data = (notificationDetails.payload as RemoteMessage).data
            val type = data[FlutterFirebaseMessagingService.KEY_NOTIFICATION_CATEGORY]
            val messageId = data[FlutterFirebaseMessagingService.KEY_MESSAGE_ID]
            val roomId = data[FlutterFirebaseMessagingService.KEY_ROOM_ID]
            val tenantCode = data[FlutterFirebaseMessagingService.KEY_TENANT_CODE]

            when (type) {
                FlutterFirebaseMessagingService.NOTIFICATION_TYPE_MSGR_TOPIC, FlutterFirebaseMessagingService.NOTIFICATION_TYPE_MSGR_GROUP -> {
                    messagingStyle!!.conversationTitle = messageStyleInformation.title
                    messagingStyle!!.isGroupConversation = true
                }
                FlutterFirebaseMessagingService.NOTIFICATION_TYPE_MSGR_DIRECT -> {

                }
            }

            builder.setStyle(messagingStyle)
        }

        private fun setProgress(notificationDetails: NotificationDetails, builder: NotificationCompat.Builder) {
            if (BooleanUtils.getValue(notificationDetails.showProgress)) {
                builder.setProgress(notificationDetails.maxProgress!!, notificationDetails.progress!!, notificationDetails.indeterminate!!)
            }
        }

        private fun getCustomView(context: Context, tenantName: String?, content: String?, bigPicture: Bitmap): RemoteViews {

            val notificationView = RemoteViews(context.packageName, R.layout.notification_custom_view)

            notificationView.setImageViewBitmap(R.id.imgCard, bigPicture)

            notificationView.setTextViewText(R.id.txtTitle, content)
            notificationView.setTextViewText(R.id.txtTenantName, tenantName)
            return notificationView
        }

        private fun setBigPictureStyle(context: Context, notificationDetails: NotificationDetails, builder: NotificationCompat.Builder) {

            val bigPictureStyleInformation = notificationDetails.styleInformation as BigPictureStyleInformation?
            val bigPictureStyle = NotificationCompat.BigPictureStyle()
            bigPictureStyle.bigLargeIcon(null)

            if (bigPictureStyleInformation?.summaryText != null) {

                val contentTitle = bigPictureStyleInformation.summaryText
                bigPictureStyle.setBigContentTitle(contentTitle)

            }
            if (bigPictureStyleInformation?.contentTitle != null) {
                val summaryText = bigPictureStyleInformation.contentTitle
                bigPictureStyle.setSummaryText(summaryText)
            }
            if (!StringUtils.isNullOrEmpty(bigPictureStyleInformation?.largeIcon)) {
                bigPictureStyle.bigPicture(getBitmapFromSource(context, bigPictureStyleInformation?.largeIcon, bigPictureStyleInformation?.largeIconBitmapSource))
                builder.setStyle(bigPictureStyle)

            }

            if (!StringUtils.isNullOrEmpty(bigPictureStyleInformation?.bigPicture)) {
                bigPictureStyle.bigPicture(getBitmapFromSource(context, bigPictureStyleInformation?.bigPicture, bigPictureStyleInformation?.bigPictureBitmapSource))
                builder.setStyle(bigPictureStyle)

            }

            if (bigPictureStyleInformation?.image != null) {
                bigPictureStyle.bigPicture(getBitmapFromSource(context = context, bitmapSource = bigPictureStyleInformation.imageSource, image = bigPictureStyleInformation.image))
                builder.setStyle(bigPictureStyle)
            }


        }

        private fun setInboxStyle(notificationDetails: NotificationDetails, builder: NotificationCompat.Builder) {
            val inboxStyleInformation = notificationDetails.styleInformation as InboxStyleInformation?
            val inboxStyle = NotificationCompat.InboxStyle()
            if (inboxStyleInformation?.contentTitle != null) {
                val contentTitle = inboxStyleInformation.contentTitle
                inboxStyle.setBigContentTitle(contentTitle)
            }
            if (inboxStyleInformation?.summaryText != null) {
                val summaryText = inboxStyleInformation.summaryText
                inboxStyle.setSummaryText(summaryText)
            }
            if (inboxStyleInformation?.lines != null) {
                for (line in inboxStyleInformation.lines) {
                    inboxStyle.addLine(line)
                }
            }
            builder.setStyle(inboxStyle)
        }

        private fun setBigTextStyle(notificationDetails: NotificationDetails, builder: NotificationCompat.Builder) {
            val bigTextStyleInformation = notificationDetails.styleInformation as BigTextStyleInformation?
            val bigTextStyle = NotificationCompat.BigTextStyle()
            if (bigTextStyleInformation?.bigText != null) {
                val bigText = bigTextStyleInformation.bigText
                bigTextStyle.bigText(bigText)
            }
            if (bigTextStyleInformation?.contentTitle != null) {
                val contentTitle = bigTextStyleInformation.contentTitle
                bigTextStyle.setBigContentTitle(contentTitle)
            }
            if (bigTextStyleInformation?.summaryText != null) {
                val summaryText = bigTextStyleInformation.summaryText
                bigTextStyle.setSummaryText(summaryText)
            }
            builder.setStyle(bigTextStyle)
        }

        private fun setupNotificationChannel(context: Context, notificationDetails: NotificationDetails) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                var notificationChannel: NotificationChannel? = notificationManager.getNotificationChannel(notificationDetails.channelId)
                if (notificationChannel == null) {
                    notificationChannel = NotificationChannel(notificationDetails.channelId, notificationDetails.channelName, notificationDetails.importance!!)
                    notificationChannel.description = notificationDetails.channelDescription
                    if (notificationDetails.playSound != null && notificationDetails.playSound!!) {
                        val audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
                        val uri = retrieveSoundResourceUri(context, notificationDetails)
                        notificationChannel.setSound(uri, audioAttributes)
                    } else {
                        notificationChannel.setSound(null, null)
                    }
                    //                notificationChannel.enableVibration(BooleanUtils.INSTANCE.getValue(notificationDetails.getEnableVibration()));
                    //                if (notificationDetails.getVibrationPattern() != null && notificationDetails.getVibrationPattern().length > 0) {
                    //                    notificationChannel.setVibrationPattern(notificationDetails.getVibrationPattern());
                    //                }
                    //                notificationChannel.setShowBadge(BooleanUtils.INSTANCE.getValue(notificationDetails.getChannelShowBadge()));

                    notificationManager.createNotificationChannel(notificationChannel)
                }
            }
        }

        private fun retrieveSoundResourceUri(context: Context, notificationDetails: NotificationDetails): Uri {
            val uri: Uri
            if (StringUtils.isNullOrEmpty(notificationDetails.sound)) {
                uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            } else {

                val soundResourceId = context.resources.getIdentifier(notificationDetails.sound, "raw", context.packageName)
                return Uri.parse("android.resource://" + context.packageName + "/" + soundResourceId)
            }
            return uri
        }

        private fun getAlarmManager(context: Context): AlarmManager {
            return context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        }

        private fun isValidDrawableResource(context: Context, name: String?, result: Result, errorCode: String): Boolean {
            val resourceId = context.resources.getIdentifier(name, DRAWABLE, context.packageName)
            if (resourceId == 0) {
                result.error(errorCode, String.format(INVALID_DRAWABLE_RESOURCE_ERROR_MESSAGE, name), null)
                return false
            }
            return true
        }

        fun showNotification(context: Context, notificationDetails: NotificationDetails) {
            val notification = createNotification(context, notificationDetails)
            val notificationManagerCompat = getNotificationManager(context)
            notificationManagerCompat.notify(notificationDetails.id!!, notification)
        }

        private fun getNotificationManager(context: Context): NotificationManagerCompat {
            return NotificationManagerCompat.from(context)
        }
    }
}
