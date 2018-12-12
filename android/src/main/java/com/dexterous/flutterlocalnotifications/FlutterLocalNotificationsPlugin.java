package com.dexterous.flutterlocalnotifications;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import com.dexterous.flutterlocalnotifications.models.NotificationDetails;
import com.dexterous.flutterlocalnotifications.models.styles.BigPictureStyleInformation;
import com.dexterous.flutterlocalnotifications.models.styles.BigTextStyleInformation;
import com.dexterous.flutterlocalnotifications.models.styles.DefaultStyleInformation;
import com.dexterous.flutterlocalnotifications.models.styles.InboxStyleInformation;
import com.dexterous.flutterlocalnotifications.models.styles.StyleInformation;
import com.dexterous.flutterlocalnotifications.utils.BooleanUtils;
import com.dexterous.flutterlocalnotifications.utils.StringUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static com.dexterous.flutterlocalnotifications.NotificationStyle.Default;

/**
 * FlutterLocalNotificationsPlugin
 */
public class FlutterLocalNotificationsPlugin extends BroadcastReceiver implements MethodCallHandler, PluginRegistry.NewIntentListener {
    private static final String TAG = "LocalFcmNotifyPlugin";

    private static final String DRAWABLE = "drawable";
    private static final String DEFAULT_ICON = "defaultIcon";
    private static final String SCHEDULED_NOTIFICATIONS = "scheduled_notifications";
    private static final String INITIALIZE_METHOD = "initialize";
    private static final String INITIALIZE_HEADLESS_SERVICE_METHOD = "initializeHeadlessService";
    private static final String SHOW_METHOD = "show";
    private static final String CANCEL_METHOD = "cancel";
    private static final String CANCEL_ALL_METHOD = "cancelAll";
    private static final String SCHEDULE_METHOD = "schedule";
    private static final String PERIODICALLY_SHOW_METHOD = "periodicallyShow";
    private static final String SHOW_DAILY_AT_TIME_METHOD = "showDailyAtTime";
    private static final String SHOW_WEEKLY_AT_DAY_AND_TIME_METHOD = "showWeeklyAtDayAndTime";
    private static final String GET_NOTIFICATION_APP_LAUNCH_DETAILS_METHOD = "getNotificationAppLaunchDetails";
    private static final String METHOD_CHANNEL = "dexterous.com/flutter/local_notifications";
    private static final String PAYLOAD = "payload";
    private static final String INVALID_ICON_ERROR_CODE = "INVALID_ICON";
    private static final String INVALID_LARGE_ICON_ERROR_CODE = "INVALID_LARGE_ICON";
    private static final String INVALID_BIG_PICTURE_ERROR_CODE = "INVALID_BIG_PICTURE";
    private static final String INVALID_SOUND_ERROR_CODE = "INVALID_SOUND";
    private static final String NOTIFICATION_LAUNCHED_APP = "notificationLaunchedApp";
    private static final String INVALID_DRAWABLE_RESOURCE_ERROR_MESSAGE = "The resource %s could not be found. Please make sure it has been added as a drawable resource to your Android head project.";
    private static final String INVALID_RAW_RESOURCE_ERROR_MESSAGE = "The resource %s could not be found. Please make sure it has been added as a raw resource to your Android head project.";

    /*public static final String ON_NOTIFICATION_ACTION = "onNotification";
    public static final String ON_NOTIFICATION_ARGS = "onNotificationArgs";
    public static final String CALLBACK_DISPATCHER = "callbackDispatcher";
    public static final String ON_NOTIFICATION_CALLBACK_DISPATCHER = "onNotificationCallbackDispatcher";*/
    public static final String SHARED_PREFERENCES_KEY = "notification_plugin_cache";
    public static String NOTIFICATION_ID = "notification_id";
    public static String NOTIFICATION = "notification";
    public static String NOTIFICATION_DETAILS = "notificationDetails";
    public static String REPEAT = "repeat";
    private static MethodChannel channel;
    private static int defaultIconResourceId = 0;
    private final Registrar registrar;


    //fcm related
    private static final String CLICK_ACTION_VALUE = "FLUTTER_NOTIFICATION_CLICK";
    private static final String FCM_CONFIGURE = "configure";
    private static final String FCM_SUBSCRIBETOTOPIC = "subscribeToTopic";
    private static final String FCM_UNSUBSCRIBEFROMTOPIC = "unsubscribeFromTopic";
    private static final String FCM_GETTOKEN = "getToken";
    private static final String FCM_AUTOINITENABLED = "autoInitEnabled";
    private static final String FCM_DELETEINSTANCEID = "deleteInstanceID";
    private static final String FCM_SETAUTOINITENABLED = "setAutoInitEnabled";


    private FlutterLocalNotificationsPlugin(Registrar registrar) {
        this.registrar = registrar;
        this.registrar.addNewIntentListener(this);

        FirebaseApp.initializeApp(registrar.context());

        defaultIconResourceId = registrar.context().getResources().getIdentifier("ic_launcher", "drawable", registrar.context().getPackageName());

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(FlutterFirebaseMessagingService.ACTION_TOKEN);
        intentFilter.addAction(FlutterFirebaseMessagingService.ACTION_REMOTE_MESSAGE);
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(registrar.context());
        manager.registerReceiver(this, intentFilter);
    }

    public static void rescheduleNotifications(Context context) {
        ArrayList<NotificationDetails> scheduledNotifications = loadScheduledNotifications(context);
        for (Iterator<NotificationDetails> it = scheduledNotifications.iterator(); it.hasNext(); ) {
            NotificationDetails scheduledNotification = it.next();
            if (scheduledNotification.getRepeatInterval() == null) {
                scheduleNotification(context, scheduledNotification, false);
            } else {
                repeatNotification(context, scheduledNotification, false);
            }
        }
    }

    public static Notification createNotification(Context context, NotificationDetails notificationDetails) {
        setupNotificationChannel(context, notificationDetails);
        Intent intent = new Intent(context, getMainActivityClass(context));
        intent.setAction(CLICK_ACTION_VALUE);
        intent.putExtra(PAYLOAD, notificationDetails.getPayload());
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationDetails.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        DefaultStyleInformation defaultStyleInformation = (DefaultStyleInformation) notificationDetails.getStyleInformation();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, notificationDetails.getChannelId())
                .setSmallIcon(getDefaultResourceIconId(context))
                .setContentTitle(defaultStyleInformation.getHtmlFormatTitle() ? fromHtml(notificationDetails.getTitle()) : notificationDetails.getTitle())
                .setContentText(defaultStyleInformation.getHtmlFormatBody() ? fromHtml(notificationDetails.getBody()) : notificationDetails.getBody())
                .setAutoCancel(BooleanUtils.INSTANCE.getValue(notificationDetails.getAutoCancel()))
                .setContentIntent(pendingIntent)
                .setPriority(notificationDetails.getPriority())
                .setOngoing(BooleanUtils.INSTANCE.getValue(notificationDetails.getOngoing()))
                .setOnlyAlertOnce(BooleanUtils.INSTANCE.getValue(notificationDetails.getOnlyAlertOnce()));

        if (!StringUtils.INSTANCE.isNullOrEmpty(notificationDetails.getLargeIcon())) {
            builder.setLargeIcon(getBitmapFromSource(context, notificationDetails.getLargeIcon(), notificationDetails.getLargeIconBitmapSource()));
        }
        if (notificationDetails.getColor() != null) {
            builder.setColor(notificationDetails.getColor().intValue());
        }

        applyGrouping(notificationDetails, builder);
        setSound(context, notificationDetails, builder);
        setVibrationPattern(notificationDetails, builder);
        setStyle(context, notificationDetails, builder);
        setProgress(notificationDetails, builder);
        return builder.build();
    }

    private static int getDefaultResourceIconId(Context context) {
        if (defaultIconResourceId == 0) {
            defaultIconResourceId = context.getResources().getIdentifier("ic_launcher", "drawable", context.getPackageName());
            return defaultIconResourceId;
        } else {
            return defaultIconResourceId;
        }
    }

    @NonNull
    public static Gson buildGson() {
        RuntimeTypeAdapterFactory<StyleInformation> styleInformationAdapter =
                RuntimeTypeAdapterFactory
                        .of(StyleInformation.class)
                        .registerSubtype(DefaultStyleInformation.class)
                        .registerSubtype(BigTextStyleInformation.class)
                        .registerSubtype(BigPictureStyleInformation.class)
                        .registerSubtype(InboxStyleInformation.class);
        GsonBuilder builder = new GsonBuilder().registerTypeAdapterFactory(styleInformationAdapter);
        return builder.create();
    }

    private static ArrayList<NotificationDetails> loadScheduledNotifications(Context context) {
        ArrayList<NotificationDetails> scheduledNotifications = new ArrayList<>();
        SharedPreferences sharedPreferences = context.getSharedPreferences(SCHEDULED_NOTIFICATIONS, Context.MODE_PRIVATE);
        String json = sharedPreferences.getString(SCHEDULED_NOTIFICATIONS, null);
        if (json != null) {
            Gson gson = buildGson();
            Type type = new TypeToken<ArrayList<NotificationDetails>>() {
            }.getType();
            scheduledNotifications = gson.fromJson(json, type);
        }
        return scheduledNotifications;
    }


    private static void saveScheduledNotifications(Context context, ArrayList<NotificationDetails> scheduledNotifications) {
        Gson gson = buildGson();
        String json = gson.toJson(scheduledNotifications);
        SharedPreferences sharedPreferences = context.getSharedPreferences(SCHEDULED_NOTIFICATIONS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SCHEDULED_NOTIFICATIONS, json);
        editor.commit();
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        channel = new MethodChannel(registrar.messenger(), METHOD_CHANNEL);
        FlutterLocalNotificationsPlugin plugin = new FlutterLocalNotificationsPlugin(registrar);
        channel.setMethodCallHandler(plugin);
    }

    public static void removeNotificationFromCache(Integer notificationId, Context context) {
        ArrayList<NotificationDetails> scheduledNotifications = loadScheduledNotifications(context);
        for (Iterator<NotificationDetails> it = scheduledNotifications.iterator(); it.hasNext(); ) {
            NotificationDetails notificationDetails = it.next();
            if (notificationDetails.getId().equals(notificationId)) {
                it.remove();
                break;
            }
        }
        saveScheduledNotifications(context, scheduledNotifications);
    }

    @SuppressWarnings("deprecation")
    private static Spanned fromHtml(String html) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(html);
        }
    }

    private static void scheduleNotification(Context context, NotificationDetails notificationDetails, Boolean updateScheduledNotificationsCache) {
        Gson gson = buildGson();
        String notificationDetailsJson = gson.toJson(notificationDetails);
        Intent notificationIntent = new Intent(context, ScheduledNotificationReceiver.class);
        notificationIntent.putExtra(NOTIFICATION_DETAILS, notificationDetailsJson);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationDetails.getId(), notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = getAlarmManager(context);
        alarmManager.set(AlarmManager.RTC_WAKEUP, notificationDetails.getMillisecondsSinceEpoch(), pendingIntent);
        if (updateScheduledNotificationsCache) {
            ArrayList<NotificationDetails> scheduledNotifications = loadScheduledNotifications(context);
            scheduledNotifications.add(notificationDetails);
            saveScheduledNotifications(context, scheduledNotifications);
        }
    }

    private static void repeatNotification(Context context, NotificationDetails notificationDetails, Boolean updateScheduledNotificationsCache) {
        Gson gson = buildGson();
        String notificationDetailsJson = gson.toJson(notificationDetails);
        Intent notificationIntent = new Intent(context, ScheduledNotificationReceiver.class);
        notificationIntent.putExtra(NOTIFICATION_DETAILS, notificationDetailsJson);
        notificationIntent.putExtra(REPEAT, true);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, notificationDetails.getId(), notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = getAlarmManager(context);
        long repeatInterval = 0;
        switch (notificationDetails.getRepeatInterval()) {
            case EveryMinute:
                repeatInterval = 60000;
                break;
            case Hourly:
                repeatInterval = 60000 * 60;
                break;
            case Daily:
                repeatInterval = 60000 * 60 * 24;
                break;
            case Weekly:
                repeatInterval = 60000 * 60 * 24 * 7;
                break;
            default:
                break;
        }

        long startTimeMilliseconds = notificationDetails.getCalledAt();
        if (notificationDetails.getRepeatTime() != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(System.currentTimeMillis());
            calendar.set(Calendar.HOUR_OF_DAY, notificationDetails.getRepeatTime().getHour());
            calendar.set(Calendar.MINUTE, notificationDetails.getRepeatTime().getMinute());
            calendar.set(Calendar.SECOND, notificationDetails.getRepeatTime().getSecond());
            if (notificationDetails.getDay() != null) {
                calendar.set(Calendar.DAY_OF_WEEK, notificationDetails.getDay());
            }

            startTimeMilliseconds = calendar.getTimeInMillis();
        }

        // ensure that start time is in the future
        long currentTime = System.currentTimeMillis();
        while (startTimeMilliseconds < currentTime) {
            startTimeMilliseconds += repeatInterval;
        }

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, startTimeMilliseconds, repeatInterval, pendingIntent);

        if (updateScheduledNotificationsCache) {
            ArrayList<NotificationDetails> scheduledNotifications = loadScheduledNotifications(context);
            scheduledNotifications.add(notificationDetails);
            saveScheduledNotifications(context, scheduledNotifications);
        }
    }

    private static boolean setIconResourceId(Context context, NotificationDetails notificationDetails, Result result) {
        if (notificationDetails.getIconResourceId() == null) {
            int resourceId;
            if (notificationDetails.getIcon() != null) {
                resourceId = context.getResources().getIdentifier(notificationDetails.getIcon(), DRAWABLE, context.getPackageName());
                if (resourceId == 0) {
                    result.error(INVALID_ICON_ERROR_CODE, String.format(INVALID_DRAWABLE_RESOURCE_ERROR_MESSAGE, notificationDetails.getIcon()), null);
                }
            } else {
                resourceId = defaultIconResourceId;
            }
            notificationDetails.setIconResourceId(resourceId);
        }

        return notificationDetails.getIconResourceId() != 0;
    }

    private static Bitmap getBitmapFromSource(Context context, String bitmapPath, BitmapSource bitmapSource) {
        Bitmap bitmap = null;
        if (bitmapSource == BitmapSource.Drawable) {
            int resourceId = context.getResources().getIdentifier(bitmapPath, DRAWABLE, context.getPackageName());
            bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId);
        } else if (bitmapSource == BitmapSource.FilePath) {
            bitmap = BitmapFactory.decodeFile(bitmapPath);
        }

        return bitmap;
    }

    private static void applyGrouping(NotificationDetails notificationDetails, NotificationCompat.Builder builder) {
        Boolean isGrouped = false;
        if (!StringUtils.INSTANCE.isNullOrEmpty(notificationDetails.getGroupKey())) {
            builder.setGroup(notificationDetails.getGroupKey());
            isGrouped = true;
        }

        if (isGrouped) {
            if (BooleanUtils.INSTANCE.getValue(notificationDetails.getSetAsGroupSummary())) {
                builder.setGroupSummary(true);
            }

            builder.setGroupAlertBehavior(notificationDetails.getGroupAlertBehavior());
        }
    }

    private static void setVibrationPattern(NotificationDetails notificationDetails, NotificationCompat.Builder builder) {
        if (BooleanUtils.INSTANCE.getValue(notificationDetails.getEnableVibration())) {
            if (notificationDetails.getVibrationPattern() != null && notificationDetails.getVibrationPattern().length > 0) {
                builder.setVibrate(notificationDetails.getVibrationPattern());
            }
        } else {
            builder.setVibrate(new long[]{0});
        }
    }

    private static void setSound(Context context, NotificationDetails notificationDetails, NotificationCompat.Builder builder) {
        if (BooleanUtils.INSTANCE.getValue(notificationDetails.getPlaySound())) {
            Uri uri = retrieveSoundResourceUri(context, notificationDetails);
            builder.setSound(uri);
        } else {
            builder.setSound(null);
        }
    }

    private static Class getMainActivityClass(Context context) {
        String packageName = context.getPackageName();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        String className = launchIntent.getComponent().getClassName();
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void setStyle(Context context, NotificationDetails notificationDetails, NotificationCompat.Builder builder) {
        switch (notificationDetails.getStyle()) {
            case Default:
                break;
            case BigPicture:
                setBigPictureStyle(context, notificationDetails, builder);
                break;
            case BigText:
                setBigTextStyle(notificationDetails, builder);
                break;
            case Inbox:
                setInboxStyle(notificationDetails, builder);
                break;
            default:
                break;
        }
    }

    private static void setProgress(NotificationDetails notificationDetails, NotificationCompat.Builder builder) {
        if (BooleanUtils.INSTANCE.getValue(notificationDetails.getShowProgress())) {
            builder.setProgress(notificationDetails.getMaxProgress(), notificationDetails.getProgress(), notificationDetails.getIndeterminate());
        }
    }

    private static void setBigPictureStyle(Context context, NotificationDetails notificationDetails, NotificationCompat.Builder builder) {
        BigPictureStyleInformation bigPictureStyleInformation = (BigPictureStyleInformation) notificationDetails.getStyleInformation();
        NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();
        if (bigPictureStyleInformation.getContentTitle() != null) {
            CharSequence contentTitle = bigPictureStyleInformation.getHtmlFormatContentTitle() ? fromHtml(bigPictureStyleInformation.getContentTitle()) : bigPictureStyleInformation.getContentTitle();
            bigPictureStyle.setBigContentTitle(contentTitle);
        }
        if (bigPictureStyleInformation.getSummaryText() != null) {
            CharSequence summaryText = bigPictureStyleInformation.getHtmlFormatSummaryText() ? fromHtml(bigPictureStyleInformation.getSummaryText()) : bigPictureStyleInformation.getSummaryText();
            bigPictureStyle.setSummaryText(summaryText);
        }
        if (bigPictureStyleInformation.getLargeIcon() != null) {
            bigPictureStyle.bigLargeIcon(getBitmapFromSource(context, bigPictureStyleInformation.getLargeIcon(), bigPictureStyleInformation.getLargeIconBitmapSource()));
        }
        bigPictureStyle.bigPicture(getBitmapFromSource(context, bigPictureStyleInformation.getBigPicture(), bigPictureStyleInformation.getBigPictureBitmapSource()));
        builder.setStyle(bigPictureStyle);
    }

    private static void setInboxStyle(NotificationDetails notificationDetails, NotificationCompat.Builder builder) {
        InboxStyleInformation inboxStyleInformation = (InboxStyleInformation) notificationDetails.getStyleInformation();
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        if (inboxStyleInformation.getContentTitle() != null) {
            CharSequence contentTitle = inboxStyleInformation.getHtmlFormatContentTitle() ? fromHtml(inboxStyleInformation.getContentTitle()) : inboxStyleInformation.getContentTitle();
            inboxStyle.setBigContentTitle(contentTitle);
        }
        if (inboxStyleInformation.getSummaryText() != null) {
            CharSequence summaryText = inboxStyleInformation.getHtmlFormatSummaryText() ? fromHtml(inboxStyleInformation.getSummaryText()) : inboxStyleInformation.getSummaryText();
            inboxStyle.setSummaryText(summaryText);
        }
        if (inboxStyleInformation.getLines() != null) {
            for (String line : inboxStyleInformation.getLines()) {
                inboxStyle.addLine(inboxStyleInformation.getHtmlFormatLines() ? fromHtml(line) : line);
            }
        }
        builder.setStyle(inboxStyle);
    }

    private static void setBigTextStyle(NotificationDetails notificationDetails, NotificationCompat.Builder builder) {
        BigTextStyleInformation bigTextStyleInformation = (BigTextStyleInformation) notificationDetails.getStyleInformation();
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        if (bigTextStyleInformation.getBigText() != null) {
            CharSequence bigText = bigTextStyleInformation.getHtmlFormatBigText() ? fromHtml(bigTextStyleInformation.getBigText()) : bigTextStyleInformation.getBigText();
            bigTextStyle.bigText(bigText);
        }
        if (bigTextStyleInformation.getContentTitle() != null) {
            CharSequence contentTitle = bigTextStyleInformation.getHtmlFormatContentTitle() ? fromHtml(bigTextStyleInformation.getContentTitle()) : bigTextStyleInformation.getContentTitle();
            bigTextStyle.setBigContentTitle(contentTitle);
        }
        if (bigTextStyleInformation.getSummaryText() != null) {
            CharSequence summaryText = bigTextStyleInformation.getHtmlFormatSummaryText() ? fromHtml(bigTextStyleInformation.getSummaryText()) : bigTextStyleInformation.getSummaryText();
            bigTextStyle.setSummaryText(summaryText);
        }
        builder.setStyle(bigTextStyle);
    }

    private static void setupNotificationChannel(Context context, NotificationDetails notificationDetails) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel = notificationManager.getNotificationChannel(notificationDetails.getChannelId());
            if (notificationChannel == null) {
                notificationChannel = new NotificationChannel(notificationDetails.getChannelId(), notificationDetails.getChannelName(), notificationDetails.getImportance());
                notificationChannel.setDescription(notificationDetails.getChannelDescription());
                if (notificationDetails.getPlaySound() != null && notificationDetails.getPlaySound()) {
                    AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build();
                    Uri uri = retrieveSoundResourceUri(context, notificationDetails);
                    notificationChannel.setSound(uri, audioAttributes);
                } else {
                    notificationChannel.setSound(null, null);
                }
//                notificationChannel.enableVibration(BooleanUtils.INSTANCE.getValue(notificationDetails.getEnableVibration()));
//                if (notificationDetails.getVibrationPattern() != null && notificationDetails.getVibrationPattern().length > 0) {
//                    notificationChannel.setVibrationPattern(notificationDetails.getVibrationPattern());
//                }
//                notificationChannel.setShowBadge(BooleanUtils.INSTANCE.getValue(notificationDetails.getChannelShowBadge()));
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
    }

    private static Uri retrieveSoundResourceUri(Context context, NotificationDetails notificationDetails) {
        Uri uri;
        if (StringUtils.INSTANCE.isNullOrEmpty(notificationDetails.getSound())) {
            uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        } else {

            int soundResourceId = context.getResources().getIdentifier(notificationDetails.getSound(), "raw", context.getPackageName());
            return Uri.parse("android.resource://" + context.getPackageName() + "/" + soundResourceId);
        }
        return uri;
    }

    private static AlarmManager getAlarmManager(Context context) {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    private static boolean isValidDrawableResource(Context context, String name, Result result, String errorCode) {
        int resourceId = context.getResources().getIdentifier(name, DRAWABLE, context.getPackageName());
        if (resourceId == 0) {
            result.error(errorCode, String.format(INVALID_DRAWABLE_RESOURCE_ERROR_MESSAGE, name), null);
            return false;
        }
        return true;
    }

    @Override
    public void onMethodCall(final MethodCall call, final Result result) {
        switch (call.method) {

            case INITIALIZE_METHOD: {
                // initializeHeadlessService(call, result);
                initialize(call, result);
                break;
            }
            case GET_NOTIFICATION_APP_LAUNCH_DETAILS_METHOD: {
//                getNotificationAppLaunchDetails(result);
                break;
            }
            case SHOW_METHOD: {
                show(call, result);
                break;
            }
            case SCHEDULE_METHOD: {
                schedule(call, result);
                break;
            }
            case PERIODICALLY_SHOW_METHOD:
            case SHOW_DAILY_AT_TIME_METHOD:
            case SHOW_WEEKLY_AT_DAY_AND_TIME_METHOD: {
                repeat(call, result);
                break;
            }
            case CANCEL_METHOD:
                cancel(call, result);
                break;
            case CANCEL_ALL_METHOD:
                cancelAllNotifications(result);
                break;
            case FCM_CONFIGURE:
                FlutterFirebaseMessagingService.broadcastToken(registrar.context());
                if (registrar.activity() != null) {
                    sendMessageFromIntent("onLaunch", registrar.activity().getIntent());
                }
                result.success(null);
                break;
            case FCM_SUBSCRIBETOTOPIC:
                String topic = call.arguments();
                FirebaseMessaging.getInstance().subscribeToTopic(topic);
                result.success(null);
                break;
            case FCM_UNSUBSCRIBEFROMTOPIC:
                String unsubscribeTopic = call.arguments();
                FirebaseMessaging.getInstance().unsubscribeFromTopic(unsubscribeTopic);
                result.success(null);
                break;
            case FCM_GETTOKEN:
                FirebaseInstanceId.getInstance()
                        .getInstanceId()
                        .addOnCompleteListener(
                                new OnCompleteListener<InstanceIdResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                                        if (!task.isSuccessful()) {
                                            Log.w(TAG, "getToken, error fetching instanceID: ", task.getException());
                                            result.success(null);
                                            return;
                                        }

                                        result.success(task.getResult().getToken());
                                    }
                                });
                break;
            case FCM_AUTOINITENABLED:
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    FirebaseInstanceId.getInstance().deleteInstanceId();
                                    result.success(true);
                                } catch (IOException ex) {
                                    Log.e(TAG, "deleteInstanceID, error:", ex);
                                    result.success(false);
                                }
                            }
                        })
                        .start();
                break;
            case FCM_DELETEINSTANCEID:
                result.success(FirebaseMessaging.getInstance().isAutoInitEnabled());
                break;
            case FCM_SETAUTOINITENABLED:
                Boolean isEnabled = call.arguments();
                FirebaseMessaging.getInstance().setAutoInitEnabled(isEnabled);
                result.success(null);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    /*private void initializeHeadlessService(MethodCall call, Result result) {
        Map<String, Object> arguments = call.arguments();
        SharedPreferences sharedPreferences = registrar.context().getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if(arguments.containsKey(CALLBACK_DISPATCHER)) {
            Object callbackDispatcher = arguments.get(CALLBACK_DISPATCHER);
            if (callbackDispatcher instanceof Long) {
                editor.putLong(CALLBACK_DISPATCHER, (Long) callbackDispatcher);
            } else if (callbackDispatcher instanceof Integer) {
                editor.putLong(CALLBACK_DISPATCHER, (Integer) callbackDispatcher);
            }
        } else if(sharedPreferences.contains(CALLBACK_DISPATCHER)){
            editor.remove(CALLBACK_DISPATCHER);
        }

        if(arguments.containsKey(ON_NOTIFICATION_CALLBACK_DISPATCHER)) {
            Object onNotificationCallbackDispatcher = arguments.get(ON_NOTIFICATION_CALLBACK_DISPATCHER);
            if(onNotificationCallbackDispatcher instanceof Long) {
                editor.putLong(ON_NOTIFICATION_CALLBACK_DISPATCHER, (Long)onNotificationCallbackDispatcher);
            } else if(onNotificationCallbackDispatcher instanceof Integer) {
                editor.putLong(ON_NOTIFICATION_CALLBACK_DISPATCHER, (Integer)onNotificationCallbackDispatcher);
            }
        } else if(sharedPreferences.contains(ON_NOTIFICATION_CALLBACK_DISPATCHER)){
            editor.remove(ON_NOTIFICATION_CALLBACK_DISPATCHER);
        }

        editor.commit();
    }*/

    private void cancel(MethodCall call, Result result) {
        Integer id = call.arguments();
        cancelNotification(id);
        result.success(null);
    }

    private void repeat(MethodCall call, Result result) {
        Map<String, Object> arguments = call.arguments();
        NotificationDetails notificationDetails = extractNotificationDetails(result, arguments);
        if (notificationDetails != null) {
            repeatNotification(registrar.context(), notificationDetails, true);
            result.success(null);
        }
    }

    private void schedule(MethodCall call, Result result) {
        Map<String, Object> arguments = call.arguments();
        NotificationDetails notificationDetails = extractNotificationDetails(result, arguments);
        if (notificationDetails != null) {
            scheduleNotification(registrar.context(), notificationDetails, true);
            result.success(null);
        }
    }

    private void show(MethodCall call, Result result) {
        Map<String, Object> arguments = call.arguments();
        NotificationDetails notificationDetails = extractNotificationDetails(result, arguments);
        if (notificationDetails != null) {
            showNotification(registrar.context(), notificationDetails);
            result.success(null);
        }
    }

    private void getNotificationAppLaunchDetails(Result result) {
        Map<String, Object> notificationAppLaunchDetails = new HashMap<>();
        String payload = null;
        Boolean notificationLaunchedApp = (registrar.activity() != null && CLICK_ACTION_VALUE.equals(registrar.activity().getIntent().getAction()));
        notificationAppLaunchDetails.put(NOTIFICATION_LAUNCHED_APP, notificationLaunchedApp);
        if (notificationLaunchedApp) {
            payload = registrar.activity().getIntent().getStringExtra(PAYLOAD);
        }
        notificationAppLaunchDetails.put(PAYLOAD, payload);
        result.success(notificationAppLaunchDetails);
    }

    private void initialize(MethodCall call, Result result) {
        Map<String, Object> arguments = call.arguments();
        String defaultIcon = (String) arguments.get(DEFAULT_ICON);

        defaultIconResourceId = registrar.context().getResources().getIdentifier(defaultIcon, "drawable", registrar.context().getPackageName());
        if (defaultIconResourceId == 0) {
            result.error(INVALID_ICON_ERROR_CODE, String.format(INVALID_DRAWABLE_RESOURCE_ERROR_MESSAGE, defaultIcon), null);
            return;
        }
        if (registrar.activity() != null) {
            sendMessageFromIntent("selectNotification", registrar.activity().getIntent());
        }
        result.success(true);
    }

    /// Extracts the details of the notifications passed from the Flutter side and also validates that any specified drawable/raw resources exist
    private NotificationDetails extractNotificationDetails(Result result, Map<String, Object> arguments) {
        NotificationDetails notificationDetails = NotificationDetails.Companion.from(arguments);
        // validate the icon resource
        if (!setIconResourceId(registrar.context(), notificationDetails, result)) {
            return null;
        }
        if (!StringUtils.INSTANCE.isNullOrEmpty(notificationDetails.getLargeIcon())) {
            // validate the large icon resource
            if (notificationDetails.getLargeIconBitmapSource() == BitmapSource.Drawable) {
                if (!isValidDrawableResource(registrar.context(), notificationDetails.getLargeIcon(), result, INVALID_LARGE_ICON_ERROR_CODE)) {
                    return null;
                }
            }
        }
        if (notificationDetails.getStyle() == NotificationStyle.BigPicture) {
            // validate the big picture resources
            BigPictureStyleInformation bigPictureStyleInformation = (BigPictureStyleInformation) notificationDetails.getStyleInformation();
            if (!StringUtils.INSTANCE.isNullOrEmpty(bigPictureStyleInformation.getLargeIcon())) {
                if (bigPictureStyleInformation.getLargeIconBitmapSource() == BitmapSource.Drawable && !isValidDrawableResource(registrar.context(), bigPictureStyleInformation.getLargeIcon(), result, INVALID_LARGE_ICON_ERROR_CODE)) {
                    return null;
                }
            }
            if (bigPictureStyleInformation.getBigPictureBitmapSource() == BitmapSource.Drawable && !isValidDrawableResource(registrar.context(), bigPictureStyleInformation.getBigPicture(), result, INVALID_BIG_PICTURE_ERROR_CODE)) {
                return null;
            }
        }
        if (!StringUtils.INSTANCE.isNullOrEmpty(notificationDetails.getSound())) {
            int soundResourceId = registrar.context().getResources().getIdentifier(notificationDetails.getSound(), "raw", registrar.context().getPackageName());
            if (soundResourceId == 0) {
                result.error(INVALID_SOUND_ERROR_CODE, INVALID_RAW_RESOURCE_ERROR_MESSAGE, null);
            }
        }

        return notificationDetails;
    }

    private void cancelNotification(Integer id) {
        Context context = registrar.context();
        Intent intent = new Intent(context, ScheduledNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager = getAlarmManager(context);
        alarmManager.cancel(pendingIntent);
        NotificationManagerCompat notificationManager = getNotificationManager(context);
        notificationManager.cancel(id);
        removeNotificationFromCache(id, context);
    }

    private void cancelAllNotifications(Result result) {
        Context context = registrar.context();
        NotificationManagerCompat notificationManager = getNotificationManager(context);
        notificationManager.cancelAll();
        ArrayList<NotificationDetails> scheduledNotifications = loadScheduledNotifications(context);
        if (scheduledNotifications == null || scheduledNotifications.isEmpty()) {
            result.success(null);
            return;
        }

        Intent intent = new Intent(context, ScheduledNotificationReceiver.class);
        for (NotificationDetails scheduledNotification :
                scheduledNotifications) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, scheduledNotification.getId(), intent, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager alarmManager = getAlarmManager(context);
            alarmManager.cancel(pendingIntent);
        }

        saveScheduledNotifications(context, new ArrayList<NotificationDetails>());
        result.success(null);
    }

    public static void showNotification(Context context, NotificationDetails notificationDetails) {
        Notification notification = createNotification(context, notificationDetails);
        NotificationManagerCompat notificationManagerCompat = getNotificationManager(context);
        notificationManagerCompat.notify(notificationDetails.getId(), notification);
        /*SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        if(sharedPreferences.contains(ON_NOTIFICATION_CALLBACK_DISPATCHER)) {
            long callbackHandle = sharedPreferences.getLong(ON_NOTIFICATION_CALLBACK_DISPATCHER, 0);
            HashMap<String, Object> callbackArgs = new HashMap<>();
            callbackArgs.put(CALLBACK_DISPATCHER, callbackHandle);
            callbackArgs.put(NotificationDetails.ID, notificationDetails.id);
            callbackArgs.put(NotificationDetails.TITLE, notificationDetails.title);
            callbackArgs.put(NotificationDetails.BODY, notificationDetails.body);
            callbackArgs.put(PAYLOAD, notificationDetails.payload);
            Intent intent = new Intent(context, NotificationService.class);
            intent.setAction(ON_NOTIFICATION_ACTION);
            intent.putExtra(ON_NOTIFICATION_ARGS, callbackArgs);
            NotificationService.enqueueWork(context, intent);
        }*/
    }

    private static NotificationManagerCompat getNotificationManager(Context context) {
        return NotificationManagerCompat.from(context);
    }

    @Override
    public boolean onNewIntent(Intent intent) {

        boolean res = sendMessageFromIntent("onResume", intent);

        if (res && registrar.activity() != null) {
            registrar.activity().setIntent(intent);
        }
        return res;

    }

    // BroadcastReceiver implementation.
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action == null) {
            return;
        }

        if (action.equals(FlutterFirebaseMessagingService.ACTION_TOKEN)) {
            String token = intent.getStringExtra(FlutterFirebaseMessagingService.EXTRA_TOKEN);
            channel.invokeMethod("onToken", token);
        } else if (action.equals(FlutterFirebaseMessagingService.ACTION_REMOTE_MESSAGE)) {
//            RemoteMessage message =
//                    intent.getParcelableExtra(FlutterFirebaseMessagingService.EXTRA_REMOTE_MESSAGE);
//            Map<String, Object> content = parseRemoteMessage(message);
//            channel.invokeMethod("onMessage", content);
        }
    }


    @NonNull
    private Map<String, Object> parseRemoteMessage(RemoteMessage message) {
        Map<String, Object> content = new HashMap<>();
        content.put("data", message.getData());

        RemoteMessage.Notification notification = message.getNotification();

        Map<String, Object> notificationMap = new HashMap<>();

        String title = notification != null ? notification.getTitle() : null;
        notificationMap.put("title", title);

        String body = notification != null ? notification.getBody() : null;
        notificationMap.put("body", body);

        content.put("notification", notificationMap);
        return content;
    }

    /**
     * @return true if intent contained a message to send.
     */
    private boolean sendMessageFromIntent(String method, Intent intent) {

        if (CLICK_ACTION_VALUE.equals(intent.getAction())
                || CLICK_ACTION_VALUE.equals(intent.getStringExtra("click_action"))) {
            Bundle extras = intent.getExtras();
            RemoteMessage payload = extras.getParcelable(PAYLOAD);

            if (payload == null) {
                return false;
            }

            channel.invokeMethod(method, payload.getData());
            return true;
        } else if (CLICK_ACTION_VALUE.equals(intent.getAction())) {
            String payload = intent.getStringExtra(PAYLOAD);
            channel.invokeMethod(method, payload);
            return true;
        }
        return false;
    }
}
