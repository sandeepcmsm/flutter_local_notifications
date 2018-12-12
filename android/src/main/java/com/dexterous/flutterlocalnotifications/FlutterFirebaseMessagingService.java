// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.dexterous.flutterlocalnotifications;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.dexterous.flutterlocalnotifications.models.NotificationDetails;
import com.dexterous.flutterlocalnotifications.models.styles.BigTextStyleInformation;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class FlutterFirebaseMessagingService extends FirebaseMessagingService {

    public static final String ACTION_REMOTE_MESSAGE =
            "io.flutter.plugins.firebasemessaging.NOTIFICATION";
    public static final String EXTRA_REMOTE_MESSAGE = "notification";

    public static final String ACTION_TOKEN = "io.flutter.plugins.firebasemessaging.TOKEN";
    public static final String EXTRA_TOKEN = "token";

    public static final String KEY_NOTIFICATION_EXTRA_STORY_ID = "story_id";
    public static final String KEY_NOTIFICATION_EXTRA_TENANT_ID = "tenant_app_id";
    public static final String KEY_TITLE = "title";

    public static final String KEY_IMAGE = "media-url";

    public static final String KEY_CONTENT = "content";

    public static final String KEY_INCIDENT_ID = "incident_id";
    public static final String KEY_NOTIFICATION_ID = "notificationId";
    public static final String KEY_NOTIFICATION_TAG = "notificationTag";
    public static final String KEY_TENANT_CODE = "tenant_code";
    public static final String KEY_TENANT_NAME = "tenant_name";
    public static final String KEY_NOTIFICATION__BODY = "body";
    public static final String KEY_ROOM_NAME = "room_name";
    public static final String KEY_ROOM_ID = "room_id";

    public static final String KEY_NOTIFICATION_CATEGORY = "category";

    public static final String NOTIFICATION_TYPE_CARD = "card";
    public static final String NOTIFICATION_SAFETY_CHECK = "safetycheck";
    public static final String NOTIFICATION_TYPE_KC = "kc";
    public static final String NOTIFICATION_TYPE_POLL = "poll";
    public static final String NOTIFICATION_TYPE_TENANT_MSG_AVL = "tenant_msg_avl";
    public static final String NOTIFICATION_TYPE_ADD_DEVICE_TAP = "add_device_onetap";
    public static final String NOTIFICATION_TYPE_SIGNIN_DEVICE_TAP = "onetapsignin";
    public static final String KEY_ACCESS_TOKEN = "access_token";
    public static final String KEY_ADD_DEVICE_ID = "add_device_id";

    public static final int NOTIFICATION_RESPONSE_YES = 1;
    public static final int NOTIFICATION_RESPONSE_NO = 2;

    public static final int REQUEST_CODE = 1000;

    public static final int SAFETY_CHECK_REQUEST_CODE = 2000;


    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.i("fcmservice", "received the message " + remoteMessage.toString());
        Intent intent = new Intent(ACTION_REMOTE_MESSAGE);
        intent.putExtra(EXTRA_REMOTE_MESSAGE, remoteMessage);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        handleMessageCategory(remoteMessage);
    }

    private void handleMessageCategory(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();

        String type = data.get(KEY_NOTIFICATION_CATEGORY);

        switch (type) {
            case NOTIFICATION_TYPE_CARD:
                String alertId = data.get(KEY_NOTIFICATION_EXTRA_STORY_ID);
                String tenantId = data.get(KEY_NOTIFICATION_EXTRA_TENANT_ID);

                showAlertNotification(remoteMessage, alertId, tenantId, type);
                String url = data.get(KEY_IMAGE);
                if (null != url) {
//                    updateNotificationWithImage(tag, url, data, type);
                }
                break;
            case NOTIFICATION_SAFETY_CHECK:
                break;
            case NOTIFICATION_TYPE_KC:
                break;
            case NOTIFICATION_TYPE_POLL:
                break;
            case NOTIFICATION_TYPE_TENANT_MSG_AVL:
                break;
            case NOTIFICATION_TYPE_ADD_DEVICE_TAP:
                break;
            case NOTIFICATION_TYPE_SIGNIN_DEVICE_TAP:
                break;
            default:
        }
    }

    private void showAlertNotification(RemoteMessage remoteMessage, String alertId, String tenantId, String type) {

        if (alertId == null || alertId.isEmpty()) return;
        Map<String, String> data = remoteMessage.getData();

        NotificationDetails.Builder notificationDetailsBuilder = new NotificationDetails.Builder();
        BigTextStyleInformation styleInformation = new BigTextStyleInformation(false, false, data.get(KEY_CONTENT), false, data.get(KEY_TITLE), false, data.get(KEY_CONTENT), false);

        AlertNotificationChannel channel = new AlertNotificationChannel();
        notificationDetailsBuilder
                .id((tenantId + alertId).hashCode())
                .channelId(channel.getChannelId())
                .channelDescription(channel.getChannelDesc())
                .channelName(channel.getChannelName())
                .autoCancel(true)
                .payload(remoteMessage)
                .priority(2)
                .importance(4)
                .style(NotificationStyle.BigText)
                .styleInformation(styleInformation);

        FlutterLocalNotificationsPlugin.showNotification(getApplicationContext(), notificationDetailsBuilder.build());
    }

    @Override
    public void onNewToken(String s) {
        Log.i("fcmservice", "onNewToken " + s);
        super.onNewToken(s);
        broadcastToken(this);
    }

    public static void broadcastToken(Context context) {
        Intent intent = new Intent(ACTION_TOKEN);
        intent.putExtra(EXTRA_TOKEN, FirebaseInstanceId.getInstance().getToken());
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
