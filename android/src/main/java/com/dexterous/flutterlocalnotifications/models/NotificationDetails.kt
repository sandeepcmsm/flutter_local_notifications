package com.dexterous.flutterlocalnotifications.models

import android.graphics.Color
import android.os.Build
import android.os.Parcelable

import com.dexterous.flutterlocalnotifications.BitmapSource
import com.dexterous.flutterlocalnotifications.NotificationStyle
import com.dexterous.flutterlocalnotifications.RepeatInterval
import com.dexterous.flutterlocalnotifications.models.styles.BigPictureStyleInformation
import com.dexterous.flutterlocalnotifications.models.styles.BigTextStyleInformation
import com.dexterous.flutterlocalnotifications.models.styles.DefaultStyleInformation
import com.dexterous.flutterlocalnotifications.models.styles.InboxStyleInformation
import com.dexterous.flutterlocalnotifications.models.styles.StyleInformation
import com.google.firebase.messaging.RemoteMessage

import java.util.ArrayList

class NotificationDetails constructor(builder: Builder) {

    var id: Int? = null
    var title: String? = null
    var body: String? = null
    var icon: String? = null
    var channelId: String? = "Default_Channel_Id"
    var channelName: String? = null
    var channelDescription: String? = null
    var channelShowBadge: Boolean? = null
    var importance: Int? = null
    var priority: Int? = null
    var playSound: Boolean? = null
    var sound: String? = null
    var enableVibration: Boolean? = null
    var vibrationPattern: LongArray? = null
    var style: NotificationStyle? = null
    var styleInformation: StyleInformation? = null
    var repeatInterval: RepeatInterval? = null
    var repeatTime: Time? = null
    var millisecondsSinceEpoch: Long? = null
    var calledAt: Long? = null
    var payload: Parcelable? = null
    var groupKey: String? = null
    var setAsGroupSummary: Boolean? = null
    var groupAlertBehavior: Int? = null
    var autoCancel: Boolean? = null
    var ongoing: Boolean? = null
    var day: Int? = null
    var color: Int? = null
    var largeIcon: String? = null
    var largeIconBitmapSource: BitmapSource? = null
    var onlyAlertOnce: Boolean? = null
    var showProgress: Boolean? = null
    var maxProgress: Int? = null
    var progress: Int? = null
    var indeterminate: Boolean? = null


    // Note: this is set on the Android to save details about the icon that should be used when re-hydrating scheduled notifications when a device has been restarted
    var iconResourceId: Int? = null

    init {
        id = builder.id
        title = builder.title
        body = builder.body
        icon = builder.icon
        channelId = builder.channelId
        channelName = builder.channelName
        channelDescription = builder.channelDescription
        channelShowBadge = builder.channelShowBadge
        importance = builder.importance
        priority = builder.priority
        playSound = builder.playSound
        sound = builder.sound
        enableVibration = builder.enableVibration
        vibrationPattern = builder.vibrationPattern
        style = builder.style
        styleInformation = builder.styleInformation
        repeatInterval = builder.repeatInterval
        repeatTime = builder.repeatTime
        millisecondsSinceEpoch = builder.millisecondsSinceEpoch
        calledAt = builder.calledAt
        payload = builder.payload
        groupKey = builder.groupKey
        setAsGroupSummary = builder.setAsGroupSummary
        groupAlertBehavior = builder.groupAlertBehavior
        autoCancel = builder.autoCancel
        ongoing = builder.ongoing
        day = builder.day
        color = builder.color
        largeIcon = builder.largeIcon
        largeIconBitmapSource = builder.largeIconBitmapSource
        onlyAlertOnce = builder.onlyAlertOnce
        showProgress = builder.showProgress
        maxProgress = builder.maxProgress
        progress = builder.progress
        indeterminate = builder.indeterminate
        iconResourceId = builder.iconResourceId
    }



    class Builder {
        internal var id: Int? = null
        internal var title: String? = null
        internal var body: String? = null
        internal var icon: String? = null
        internal var channelId: String? = null
        internal var channelName: String? = null
        internal var channelDescription: String? = null
        internal var channelShowBadge: Boolean? = null
        internal var importance: Int? = null
        internal var priority: Int? = null
        internal var playSound: Boolean? = null
        internal var sound: String? = null
        internal var enableVibration: Boolean? = null
        internal var vibrationPattern: LongArray? = null
        internal var style: NotificationStyle? = null
        internal var styleInformation: StyleInformation? = null
        internal var repeatInterval: RepeatInterval? = null
        internal var repeatTime: Time? = null
        internal var millisecondsSinceEpoch: Long? = null
        internal var calledAt: Long? = null
        internal var payload: Parcelable? = null
        internal var groupKey: String? = null
        internal var setAsGroupSummary: Boolean? = null
        internal var groupAlertBehavior: Int? = null
        internal var autoCancel: Boolean? = null
        internal var ongoing: Boolean? = null
        internal var day: Int? = null
        internal var color: Int? = null
        internal var largeIcon: String? = null
        internal var largeIconBitmapSource: BitmapSource? = null
        internal var onlyAlertOnce: Boolean? = null
        internal var showProgress: Boolean? = null
        internal var maxProgress: Int? = null
        internal var progress: Int? = null
        internal var indeterminate: Boolean? = null
        internal var iconResourceId: Int? = null

        fun id(`val`: Int?): Builder {
            id = `val`
            return this
        }

        fun title(`val`: String): Builder {
            title = `val`
            return this
        }

        fun body(`val`: String): Builder {
            body = `val`
            return this
        }

        fun icon(`val`: String): Builder {
            icon = `val`
            return this
        }

        fun channelId(`val`: String): Builder {
            channelId = `val`
            return this
        }

        fun channelName(`val`: String): Builder {
            channelName = `val`
            return this
        }

        fun channelDescription(`val`: String): Builder {
            channelDescription = `val`
            return this
        }

        fun channelShowBadge(`val`: Boolean?): Builder {
            channelShowBadge = `val`
            return this
        }

        fun importance(`val`: Int?): Builder {
            importance = `val`
            return this
        }

        fun priority(`val`: Int?): Builder {
            priority = `val`
            return this
        }

        fun playSound(`val`: Boolean?): Builder {
            playSound = `val`
            return this
        }

        fun sound(`val`: String): Builder {
            sound = `val`
            return this
        }

        fun enableVibration(`val`: Boolean?): Builder {
            enableVibration = `val`
            return this
        }

        fun vibrationPattern(`val`: LongArray): Builder {
            vibrationPattern = `val`
            return this
        }

        fun style(`val`: NotificationStyle): Builder {
            style = `val`
            return this
        }

        fun styleInformation(`val`: StyleInformation): Builder {
            styleInformation = `val`
            return this
        }

        fun repeatInterval(`val`: RepeatInterval): Builder {
            repeatInterval = `val`
            return this
        }

        fun repeatTime(`val`: Time): Builder {
            repeatTime = `val`
            return this
        }

        fun millisecondsSinceEpoch(`val`: Long?): Builder {
            millisecondsSinceEpoch = `val`
            return this
        }

        fun calledAt(`val`: Long?): Builder {
            calledAt = `val`
            return this
        }

        fun payload(`val`: Parcelable): Builder {
            payload = `val`
            return this
        }

        fun groupKey(`val`: String): Builder {
            groupKey = `val`
            return this
        }

        fun setAsGroupSummary(`val`: Boolean?): Builder {
            setAsGroupSummary = `val`
            return this
        }

        fun groupAlertBehavior(`val`: Int?): Builder {
            groupAlertBehavior = `val`
            return this
        }

        fun autoCancel(`val`: Boolean?): Builder {
            autoCancel = `val`
            return this
        }

        fun ongoing(`val`: Boolean?): Builder {
            ongoing = `val`
            return this
        }

        fun day(`val`: Int?): Builder {
            day = `val`
            return this
        }

        fun color(`val`: Int?): Builder {
            color = `val`
            return this
        }

        fun largeIcon(`val`: String): Builder {
            largeIcon = `val`
            return this
        }

        fun largeIconBitmapSource(`val`: BitmapSource): Builder {
            largeIconBitmapSource = `val`
            return this
        }

        fun onlyAlertOnce(`val`: Boolean?): Builder {
            onlyAlertOnce = `val`
            return this
        }

        fun showProgress(`val`: Boolean?): Builder {
            showProgress = `val`
            return this
        }

        fun maxProgress(`val`: Int?): Builder {
            maxProgress = `val`
            return this
        }

        fun progress(`val`: Int?): Builder {
            progress = `val`
            return this
        }

        fun indeterminate(`val`: Boolean?): Builder {
            indeterminate = `val`
            return this
        }

        fun iconResourceId(`val`: Int?): Builder {
            iconResourceId = `val`
            return this
        }

        fun build(): NotificationDetails {
            return NotificationDetails(this)
        }
    }

    companion object {
        private val PAYLOAD = "payload"
        private val MILLISECONDS_SINCE_EPOCH = "millisecondsSinceEpoch"
        private val CALLED_AT = "calledAt"
        private val REPEAT_INTERVAL = "repeatInterval"
        private val REPEAT_TIME = "repeatTime"
        private val PLATFORM_SPECIFICS = "platformSpecifics"
        private val AUTO_CANCEL = "autoCancel"
        private val ONGOING = "ongoing"
        private val STYLE = "style"
        private val ICON = "icon"
        private val PRIORITY = "priority"
        private val PLAY_SOUND = "playSound"
        private val SOUND = "sound"
        private val ENABLE_VIBRATION = "enableVibration"
        private val VIBRATION_PATTERN = "vibrationPattern"
        private val GROUP_KEY = "groupKey"
        private val SET_AS_GROUP_SUMMARY = "setAsGroupSummary"
        private val GROUP_ALERT_BEHAVIOR = "groupAlertBehavior"
        private val ONLY_ALERT_ONCE = "onlyAlertOnce"
        private val CHANNEL_ID = "channelId"
        private val CHANNEL_NAME = "channelName"
        private val CHANNEL_DESCRIPTION = "channelDescription"
        private val CHANNEL_SHOW_BADGE = "channelShowBadge"
        private val IMPORTANCE = "importance"
        private val STYLE_INFORMATION = "styleInformation"
        private val BIG_TEXT = "bigText"
        private val HTML_FORMAT_BIG_TEXT = "htmlFormatBigText"
        private val CONTENT_TITLE = "contentTitle"
        private val HTML_FORMAT_CONTENT_TITLE = "htmlFormatContentTitle"
        private val SUMMARY_TEXT = "summaryText"
        private val HTML_FORMAT_SUMMARY_TEXT = "htmlFormatSummaryText"
        private val LINES = "lines"
        private val HTML_FORMAT_LINES = "htmlFormatLines"
        private val HTML_FORMAT_TITLE = "htmlFormatTitle"
        private val HTML_FORMAT_CONTENT = "htmlFormatContent"
        private val DAY = "day"
        private val COLOR_ALPHA = "colorAlpha"
        private val COLOR_RED = "colorRed"
        private val COLOR_GREEN = "colorGreen"
        private val COLOR_BLUE = "colorBlue"
        private val LARGE_ICON = "largeIcon"
        private val LARGE_ICON_BITMAP_SOURCE = "largeIconBitmapSource"
        private val BIG_PICTURE = "bigPicture"
        private val BIG_PICTURE_BITMAP_SOURCE = "bigPictureBitmapSource"
        private val SHOW_PROGRESS = "showProgress"
        private val MAX_PROGRESS = "maxProgress"
        private val PROGRESS = "progress"
        private val INDETERMINATE = "indeterminate"

        val ID = "id"
        val TITLE = "title"
        val BODY = "body"

        fun from(arguments: Map<String, Any>): NotificationDetails {
            val notificationDetails = NotificationDetails.Builder().build()
            notificationDetails.payload = arguments[PAYLOAD] as RemoteMessage
            notificationDetails.id = arguments[ID] as Int
            notificationDetails.title = arguments[TITLE] as String
            notificationDetails.body = arguments[BODY] as String
            if (arguments.containsKey(MILLISECONDS_SINCE_EPOCH)) {
                notificationDetails.millisecondsSinceEpoch = arguments[MILLISECONDS_SINCE_EPOCH] as Long
            }
            if (arguments.containsKey(CALLED_AT)) {
                notificationDetails.calledAt = arguments[CALLED_AT] as Long
            }
            if (arguments.containsKey(REPEAT_INTERVAL)) {
                notificationDetails.repeatInterval = RepeatInterval.values()[arguments[REPEAT_INTERVAL] as Int]
            }
            if (arguments.containsKey(REPEAT_TIME)) {
                val repeatTimeParams = arguments[REPEAT_TIME] as Map<String, Any>
                notificationDetails.repeatTime = Time.from(repeatTimeParams)
            }
            if (arguments.containsKey(DAY)) {
                notificationDetails.day = arguments[DAY] as Int
            }
            val platformChannelSpecifics = arguments[PLATFORM_SPECIFICS] as Map<String, Any>
            if (platformChannelSpecifics != null) {
                notificationDetails.autoCancel = platformChannelSpecifics[AUTO_CANCEL] as Boolean
                notificationDetails.ongoing = platformChannelSpecifics[ONGOING] as Boolean
                notificationDetails.style = NotificationStyle.values()[platformChannelSpecifics[STYLE] as Int]
                readStyleInformation(notificationDetails, platformChannelSpecifics)
                notificationDetails.icon = platformChannelSpecifics[ICON] as String
                notificationDetails.priority = platformChannelSpecifics[PRIORITY] as Int
                notificationDetails.playSound = platformChannelSpecifics[PLAY_SOUND] as Boolean
                notificationDetails.sound = platformChannelSpecifics[SOUND] as String
                notificationDetails.enableVibration = platformChannelSpecifics[ENABLE_VIBRATION] as Boolean
                notificationDetails.vibrationPattern = platformChannelSpecifics[VIBRATION_PATTERN] as LongArray
                notificationDetails.groupKey = platformChannelSpecifics[GROUP_KEY] as String
                notificationDetails.setAsGroupSummary = platformChannelSpecifics[SET_AS_GROUP_SUMMARY] as Boolean
                notificationDetails.groupAlertBehavior = platformChannelSpecifics[GROUP_ALERT_BEHAVIOR] as Int
                notificationDetails.onlyAlertOnce = platformChannelSpecifics[ONLY_ALERT_ONCE] as Boolean
                notificationDetails.showProgress = platformChannelSpecifics[SHOW_PROGRESS] as Boolean
                if (platformChannelSpecifics.containsKey(MAX_PROGRESS)) {
                    notificationDetails.maxProgress = platformChannelSpecifics[MAX_PROGRESS] as Int
                }

                if (platformChannelSpecifics.containsKey(PROGRESS)) {
                    notificationDetails.progress = platformChannelSpecifics[PROGRESS] as Int
                }

                if (platformChannelSpecifics.containsKey(INDETERMINATE)) {
                    notificationDetails.indeterminate = platformChannelSpecifics[INDETERMINATE] as Boolean
                }

                readColor(notificationDetails, platformChannelSpecifics)
                readChannelInformation(notificationDetails, platformChannelSpecifics)
                notificationDetails.largeIcon = platformChannelSpecifics[LARGE_ICON] as String
                if (platformChannelSpecifics.containsKey(LARGE_ICON_BITMAP_SOURCE)) {
                    val argumentValue = platformChannelSpecifics[LARGE_ICON_BITMAP_SOURCE] as Int
                    if (argumentValue != null) {
                        notificationDetails.largeIconBitmapSource = BitmapSource.values()[argumentValue]
                    }
                }
            }
            return notificationDetails
        }

        private fun readColor(notificationDetails: NotificationDetails, platformChannelSpecifics: Map<String, Any>) {
            val a = platformChannelSpecifics[COLOR_ALPHA] as Int
            val r = platformChannelSpecifics[COLOR_RED] as Int
            val g = platformChannelSpecifics[COLOR_GREEN] as Int
            val b = platformChannelSpecifics[COLOR_BLUE] as Int
            if (a != null && r != null && g != null && b != null) {
                notificationDetails.color = Color.argb(a, r, g, b)
            }
        }

        private fun readChannelInformation(notificationDetails: NotificationDetails, platformChannelSpecifics: Map<String, Any>) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationDetails.channelId = platformChannelSpecifics[CHANNEL_ID] as String
                notificationDetails.channelName = platformChannelSpecifics[CHANNEL_NAME] as String
                notificationDetails.channelDescription = platformChannelSpecifics[CHANNEL_DESCRIPTION] as String
                notificationDetails.importance = platformChannelSpecifics[IMPORTANCE] as Int
                notificationDetails.channelShowBadge = platformChannelSpecifics[CHANNEL_SHOW_BADGE] as Boolean
            }
        }

        private fun readStyleInformation(notificationDetails: NotificationDetails, platformSpecifics: Map<String, Any>) {
            val styleInformation = platformSpecifics[STYLE_INFORMATION] as Map<String, Any>
            val defaultStyleInformation = getDefaultStyleInformation(styleInformation)
            if (notificationDetails.style == NotificationStyle.Default) {
                notificationDetails.styleInformation = defaultStyleInformation
            } else if (notificationDetails.style == NotificationStyle.BigPicture) {
                val contentTitle = styleInformation[CONTENT_TITLE] as String
                val htmlFormatContentTitle = styleInformation[HTML_FORMAT_CONTENT_TITLE] as Boolean
                val summaryText = styleInformation[SUMMARY_TEXT] as String
                val htmlFormatSummaryText = styleInformation[HTML_FORMAT_SUMMARY_TEXT] as Boolean
                val largeIcon = styleInformation[LARGE_ICON] as String
                var largeIconBitmapSource: BitmapSource? = null
                if (styleInformation.containsKey(LARGE_ICON_BITMAP_SOURCE)) {
                    val largeIconBitmapSourceArgument = styleInformation[LARGE_ICON_BITMAP_SOURCE] as Int
                    largeIconBitmapSource = BitmapSource.values()[largeIconBitmapSourceArgument]
                }
                val bigPicture = styleInformation[BIG_PICTURE] as String
                val bigPictureBitmapSourceArgument = styleInformation[BIG_PICTURE_BITMAP_SOURCE] as Int
                val bigPictureBitmapSource = BitmapSource.values()[bigPictureBitmapSourceArgument]
                notificationDetails.styleInformation = BigPictureStyleInformation(defaultStyleInformation.htmlFormatTitle, defaultStyleInformation.htmlFormatBody, contentTitle, htmlFormatContentTitle, summaryText, htmlFormatSummaryText, largeIcon, largeIconBitmapSource!!, bigPicture, bigPictureBitmapSource)
            } else if (notificationDetails.style == NotificationStyle.BigText) {
                val bigText = styleInformation[BIG_TEXT] as String
                val htmlFormatBigText = styleInformation[HTML_FORMAT_BIG_TEXT] as Boolean
                val contentTitle = styleInformation[CONTENT_TITLE] as String
                val htmlFormatContentTitle = styleInformation[HTML_FORMAT_CONTENT_TITLE] as Boolean
                val summaryText = styleInformation[SUMMARY_TEXT] as String
                val htmlFormatSummaryText = styleInformation[HTML_FORMAT_SUMMARY_TEXT] as Boolean
                notificationDetails.styleInformation = BigTextStyleInformation(defaultStyleInformation.htmlFormatTitle, defaultStyleInformation.htmlFormatBody, bigText, htmlFormatBigText, contentTitle, htmlFormatContentTitle, summaryText, htmlFormatSummaryText)
            } else if (notificationDetails.style == NotificationStyle.Inbox) {
                val contentTitle = styleInformation[CONTENT_TITLE] as String
                val htmlFormatContentTitle = styleInformation[HTML_FORMAT_CONTENT_TITLE] as Boolean
                val summaryText = styleInformation[SUMMARY_TEXT] as String
                val htmlFormatSummaryText = styleInformation[HTML_FORMAT_SUMMARY_TEXT] as Boolean
                val lines = styleInformation[LINES] as ArrayList<String>
                val htmlFormatLines = styleInformation[HTML_FORMAT_LINES] as Boolean
                notificationDetails.styleInformation = InboxStyleInformation(defaultStyleInformation.htmlFormatTitle, defaultStyleInformation.htmlFormatBody, contentTitle, htmlFormatContentTitle, summaryText, htmlFormatSummaryText, lines, htmlFormatLines)
            }
        }

        private fun getDefaultStyleInformation(styleInformation: Map<String, Any>): DefaultStyleInformation {
            val htmlFormatTitle = styleInformation[HTML_FORMAT_TITLE] as Boolean
            val htmlFormatBody = styleInformation[HTML_FORMAT_CONTENT] as Boolean
            return DefaultStyleInformation(htmlFormatTitle, htmlFormatBody)
        }
    }
}
