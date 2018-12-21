package com.dexterous.flutterlocalnotifications

enum class NotificationStyle {
    Default,
    BigPicture,
    BigText,
    Inbox,
    Message
}


open interface CywNotificationChannel {
    val channelName: String
    val channelId: String
    val channelDesc: String
}

class AlertNotificationChannel(override val channelName: String = "Alerts", override val channelId: String = "alerts", override val channelDesc: String = "Alert Notification") : CywNotificationChannel

class TopicCywNotificationChannel(override val channelName: String = "Topic", override val channelId: String = "topic", override val channelDesc: String = "Topic Notification") : CywNotificationChannel {
}

class GroupCywNotificationChannel(override val channelName: String = "Group", override val channelId: String = "group", override val channelDesc: String = "Group Notification") : CywNotificationChannel {
}

class DmCywNotificationChannel(override val channelName: String = "Direct", override val channelId: String = "direct", override val channelDesc: String = "Direct Notification") : CywNotificationChannel {
}

class KcCywNotificationChannel(override val channelName: String = "KC Notification", override val channelId: String = "kc", override val channelDesc: String = "KC Notification") : CywNotificationChannel {
}

class SurveyCywNotificationChannel(override val channelName: String = "Survey Notification", override val channelId: String = "survey", override val channelDesc: String = "Survey Notification") : CywNotificationChannel {
}
