package com.dexterous.flutterlocalnotifications

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log

import com.dexterous.flutterlocalnotifications.models.NotificationDetails
import com.dexterous.flutterlocalnotifications.models.styles.BigPictureStyleInformation
import com.dexterous.flutterlocalnotifications.models.styles.BigTextStyleInformation
import com.dexterous.flutterlocalnotifications.models.styles.DefaultStyleInformation
import com.dexterous.flutterlocalnotifications.models.styles.MessageStyleInformation
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.BaseDataSubscriber
import com.facebook.datasource.DataSource
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.core.DefaultExecutorSupplier
import com.facebook.imagepipeline.image.CloseableBitmap
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.request.ImageRequest
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FlutterFirebaseMessagingService : FirebaseMessagingService() {


    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        Log.i("fcmservice", "received the message " + remoteMessage!!.toString())
        val intent = Intent(ACTION_REMOTE_MESSAGE)
        intent.putExtra(EXTRA_REMOTE_MESSAGE, remoteMessage)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

        handleMessageCategory(remoteMessage)
    }

    private fun handleMessageCategory(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data

        val type = data[KEY_NOTIFICATION_CATEGORY]

        when (type) {
            NOTIFICATION_TYPE_CARD -> {

                val alertId = data[KEY_NOTIFICATION_EXTRA_STORY_ID]
                val tenantCode = data[KEY_TENANT_CODE]

                showAlertNotification(remoteMessage, alertId, tenantCode)
                val url = data[KEY_IMAGE]
                if (null != url) {
                    updateNotificationWithImage(remoteMessage, alertId, tenantCode, url = url)
                }
            }
            NOTIFICATION_SAFETY_CHECK -> {
            }
            NOTIFICATION_TYPE_KC, NOTIFICATION_TYPE_SURVEY -> {
                var id: String? = null
                if (type == NOTIFICATION_TYPE_KC) {
                    id = data[KEY_NOTIFICATION_KC_ID]
                } else if (type == NOTIFICATION_TYPE_SURVEY) {
                    id = data[KEY_NOTIFICATION_SURVEY_ID]
                }
                val tenantCode = data[KEY_TENANT_CODE]

                showKcNotification(remoteMessage, id, tenantCode)
            }
            NOTIFICATION_TYPE_TENANT_MSG_AVL -> {
            }
            NOTIFICATION_TYPE_ADD_DEVICE_TAP -> {
            }
            NOTIFICATION_TYPE_SIGNIN_DEVICE_TAP -> {

            }
            NOTIFICATION_TYPE_MSGR_TOPIC, NOTIFICATION_TYPE_MSGR_GROUP, NOTIFICATION_TYPE_MSGR_DIRECT -> {
                var id: String? = null
                id = data[KEY_ROOM_ID]
                val tenantCode = data[KEY_TENANT_CODE]
                showMessengerNotification(remoteMessage, id, tenantCode)
            }
        }
    }

    private fun updateNotificationWithImage(remoteMessage: RemoteMessage, alertId: String?, tenantId: String?, url: String?) {
        Log.e("fcm notify","updateNotificationWithImage")
        if (!Fresco.hasBeenInitialized()) {
            Fresco.initialize(this.applicationContext)
        }

        val imageRequest = ImageRequest.fromUri(url)
        val imagePipeline = Fresco.getImagePipeline()
        val mImageDataSource = imagePipeline.fetchDecodedImage(imageRequest, null)
        mImageDataSource.subscribe(object : BaseDataSubscriber<CloseableReference<CloseableImage>>() {

            override fun onFailureImpl(dataSource: DataSource<CloseableReference<CloseableImage>>?) {
                Log.e("fcm notify","onFailureImpl")

            }

            override fun onNewResultImpl(dataSource: DataSource<CloseableReference<CloseableImage>>) {
                Log.e("fcm notify","onNewResultImpl")
                val bitmapReference = dataSource.result
                if (null != bitmapReference && bitmapReference!!.get() is CloseableBitmap) {
                    val bitmap = (bitmapReference!!.get() as CloseableBitmap).underlyingBitmap
                    if (null != bitmap) {
                        Log.e("fcm notify","got the bitmap")
                        showAlertNotification(remoteMessage, alertId, tenantId = tenantId, image = bitmap)
                    }
                }
            }

        }, DefaultExecutorSupplier(1).forBackgroundTasks())
    }


    private fun showKcNotification(remoteMessage: RemoteMessage, id: String?, tenantId: String?) {
        if (id == null || id.isEmpty()) return
        if (tenantId == null || tenantId.isEmpty()) return

        val data = remoteMessage.data
        val type = data[KEY_NOTIFICATION_CATEGORY]

        val notificationDetailsBuilder = NotificationDetails.Builder()
        val styleInformation = DefaultStyleInformation(data[KEY_TITLE]!!,data[KEY_CONTENT]!!)

        lateinit var channel: CywNotificationChannel
        if (type == NOTIFICATION_TYPE_KC) {
            channel = KcCywNotificationChannel()
        } else if (type == NOTIFICATION_TYPE_SURVEY) {
            channel = SurveyCywNotificationChannel()
        }
        notificationDetailsBuilder
                .id((tenantId + id).hashCode())
                .channelId(channel.channelId)
                .channelDescription(channel.channelDesc)
                .channelName(channel.channelName)
                .title(styleInformation.contentTitle)
                .body(styleInformation.summaryText)
                .autoCancel(true)
                .icon("ic_launcher")
                .payload(remoteMessage)
                .priority(2)
                .importance(4)
                .style(NotificationStyle.Default)
                .styleInformation(styleInformation)

        FlutterLocalNotificationsPlugin.showNotification(applicationContext, notificationDetailsBuilder.build())
    }

    private fun showMessengerNotification(remoteMessage: RemoteMessage, roomId: String?, tenantId: String?) {
        if (roomId == null || roomId.isEmpty()) return
        if (tenantId == null || tenantId.isEmpty()) return

        val data = remoteMessage.data
        val type = data[KEY_NOTIFICATION_CATEGORY]
        val messageId = data[KEY_MESSAGE_ID]

        val notificationDetailsBuilder = NotificationDetails.Builder()

        val styleInformation = MessageStyleInformation(title = data[KEY_ROOM_NAME]!!, message = data[KEY_MESSAGE]!!, timeStamp = data[KEY_TIMESTAMP]?.toLong(), sender = data[KEY_SENDER])

        lateinit var channel: CywNotificationChannel

        when (type) {
            NOTIFICATION_TYPE_MSGR_TOPIC -> channel = TopicCywNotificationChannel()
            NOTIFICATION_TYPE_MSGR_GROUP -> channel = GroupCywNotificationChannel()
            NOTIFICATION_TYPE_MSGR_DIRECT -> {
                channel = DmCywNotificationChannel()
                styleInformation.isDirect = true
            }
        }
         var notifyId: Int

        // to support message grouping
        notifyId = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            (tenantId + roomId).hashCode()
        }else{
            (tenantId + messageId).hashCode()
        }

        notificationDetailsBuilder
                .id(notifyId)
                .channelId(channel.channelId)
                .channelDescription(channel.channelDesc)
                .channelName(channel.channelName)
                .groupKey(roomId)
                .groupAlertBehavior(NotificationCompat.GROUP_ALERT_ALL)
                .autoCancel(true)
                .icon("ic_launcher")
                .payload(remoteMessage)
                .priority(2)
                .importance(4)
                .style(NotificationStyle.Message)
                .styleInformation(styleInformation)

        FlutterLocalNotificationsPlugin.showNotification(applicationContext, notificationDetailsBuilder.build())
    }

    private fun showAlertNotification(remoteMessage: RemoteMessage, alertId: String?, tenantId: String?, image: Bitmap? = null) {

        if (alertId == null || alertId.isEmpty()) return
        if (tenantId == null || tenantId.isEmpty()) return

        val data = remoteMessage.data

        val notificationDetailsBuilder = NotificationDetails.Builder()
        val textStyleInformation = BigTextStyleInformation(data[KEY_CONTENT]!!, data[KEY_TITLE]!!, data[KEY_CONTENT]!!)

        var picStyleInformation = BigPictureStyleInformation(contentTitle = data[KEY_CONTENT]!!, summaryText = data[KEY_TITLE]!!)
        if (image != null) {
            picStyleInformation.imageSource = BitmapSource.Bitmap
            picStyleInformation.image = image
        }

        val channel = AlertNotificationChannel()
        notificationDetailsBuilder
                .id((tenantId + alertId).hashCode())
                .channelId(channel.channelId)
                .channelDescription(channel.channelDesc)
                .channelName(channel.channelName)
                .title(textStyleInformation.contentTitle)
                .body(textStyleInformation.bigText)
                .autoCancel(true)
                .icon("ic_launcher")
                .payload(remoteMessage)
                .priority(2)
                .importance(4)
        if (image != null) {
            notificationDetailsBuilder.styleInformation(picStyleInformation)
            notificationDetailsBuilder.style(NotificationStyle.BigPicture)
        } else {
            notificationDetailsBuilder.styleInformation(textStyleInformation)
            notificationDetailsBuilder.style(NotificationStyle.BigText)
        }

        FlutterLocalNotificationsPlugin.showNotification(applicationContext, notificationDetailsBuilder.build())

    }

    override fun onNewToken(s: String?) {
        super.onNewToken(s)
        broadcastToken(this)
    }

    companion object {

        const val ACTION_REMOTE_MESSAGE = "io.flutter.plugins.firebasemessaging.NOTIFICATION"
        const val EXTRA_REMOTE_MESSAGE = "notification"

        const val ACTION_TOKEN = "io.flutter.plugins.firebasemessaging.TOKEN"
        const val EXTRA_TOKEN = "token"

        const val KEY_NOTIFICATION_EXTRA_STORY_ID = "story_id"
        const val KEY_NOTIFICATION_KC_ID = "kc_id"
        const val KEY_NOTIFICATION_SURVEY_ID = "survey_id"
        const val KEY_NOTIFICATION_EXTRA_TENANT_ID = "tenant_app_id"
        const val KEY_TITLE = "title"

        const val KEY_IMAGE = "media_url"

        const val KEY_CONTENT = "content"
        const val KEY_MESSAGE = "message"
        const val KEY_MESSAGE_ID = "message_id"
        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_SENDER = "sender"

        const val KEY_INCIDENT_ID = "incident_id"
        const val KEY_NOTIFICATION_ID = "notificationId"
        const val KEY_NOTIFICATION_TAG = "notificationTag"
        const val KEY_TENANT_CODE = "tenant_code"
        const val KEY_TENANT_NAME = "tenant_name"
        const val KEY_NOTIFICATION__BODY = "body"
        const val KEY_ROOM_NAME = "room_name"
        const val KEY_ROOM_ID = "room_id"

        const val KEY_NOTIFICATION_CATEGORY = "category"

        //        Notification categories
        const val NOTIFICATION_TYPE_CARD = "card"
        const val NOTIFICATION_SAFETY_CHECK = "safetycheck"
        const val NOTIFICATION_TYPE_KC = "kc"
        const val NOTIFICATION_TYPE_SURVEY = "survey"
        const val NOTIFICATION_TYPE_TENANT_MSG_AVL = "tenant_msg_avl"
        const val NOTIFICATION_TYPE_ADD_DEVICE_TAP = "add_device_onetap"
        const val NOTIFICATION_TYPE_SIGNIN_DEVICE_TAP = "onetapsignin"
        const val NOTIFICATION_TYPE_MSGR_TOPIC = "msgr_topic"
        const val NOTIFICATION_TYPE_MSGR_GROUP = "msgr_group"
        const val NOTIFICATION_TYPE_MSGR_DIRECT = "msgr_direct"


        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_ADD_DEVICE_ID = "add_device_id"

        const val NOTIFICATION_RESPONSE_YES = 1
        const val NOTIFICATION_RESPONSE_NO = 2

        const val REQUEST_CODE = 1000

        const val SAFETY_CHECK_REQUEST_CODE = 2000

        fun broadcastToken(context: Context) {
            val intent = Intent(ACTION_TOKEN)
            intent.putExtra(EXTRA_TOKEN, FirebaseInstanceId.getInstance().token)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
    }
}
