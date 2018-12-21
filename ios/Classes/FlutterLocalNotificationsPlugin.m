#import "FlutterLocalNotificationsPlugin.h"
#import "NotificationTime.h"
#import "NotificationDetails.h"
#import "FlutterLocalNotificationsPlugin.h"

#import "Firebase/Firebase.h"

static bool appResumingFromBackground;
#if defined(__IPHONE_10_0) && __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_10_0

@interface FlutterLocalNotificationsPlugin () <FIRMessagingDelegate>
@end

#endif

@implementation FlutterLocalNotificationsPlugin

FlutterMethodChannel *channel;
// FlutterMethodChannel* callbackChannel;
NSString *const INITIALIZE_METHOD = @"initialize";
NSString *const INITIALIZED_HEADLESS_SERVICE_METHOD = @"initializedHeadlessService";
NSString *const SHOW_METHOD = @"show";
NSString *const SCHEDULE_METHOD = @"schedule";
NSString *const PERIODICALLY_SHOW_METHOD = @"periodicallyShow";
NSString *const SHOW_DAILY_AT_TIME_METHOD = @"showDailyAtTime";
NSString *const SHOW_WEEKLY_AT_DAY_AND_TIME_METHOD = @"showWeeklyAtDayAndTime";
NSString *const CANCEL_METHOD = @"cancel";
NSString *const CANCEL_ALL_METHOD = @"cancelAll";
NSString *const GET_NOTIFICATION_APP_LAUNCH_DETAILS_METHOD = @"getNotificationAppLaunchDetails";
NSString *const CHANNEL = @"dexterous.com/flutter/local_notifications";
NSString *const CALLBACK_CHANNEL = @"dexterous.com/flutter/local_notifications_background";
NSString *const ON_NOTIFICATION_METHOD = @"onNotification";
NSString *const DAY = @"day";

NSString *const REQUEST_SOUND_PERMISSION = @"requestSoundPermission";
NSString *const REQUEST_ALERT_PERMISSION = @"requestAlertPermission";
NSString *const REQUEST_BADGE_PERMISSION = @"requestBadgePermission";
NSString *const DEFAULT_PRESENT_ALERT = @"defaultPresentAlert";
NSString *const DEFAULT_PRESENT_SOUND = @"defaultPresentSound";
NSString *const DEFAULT_PRESENT_BADGE = @"defaultPresentBadge";
NSString *const CALLBACK_DISPATCHER = @"callbackDispatcher";
NSString *const ON_NOTIFICATION_CALLBACK_DISPATCHER = @"onNotificationCallbackDispatcher";
NSString *const PLATFORM_SPECIFICS = @"platformSpecifics";
NSString *const ID = @"id";
NSString *const TITLE = @"title";
NSString *const BODY = @"body";
NSString *const SOUND = @"sound";
NSString *const PRESENT_ALERT = @"presentAlert";
NSString *const PRESENT_SOUND = @"presentSound";
NSString *const PRESENT_BADGE = @"presentBadge";
NSString *const MILLISECONDS_SINCE_EPOCH = @"millisecondsSinceEpoch";
NSString *const REPEAT_INTERVAL = @"repeatInterval";
NSString *const REPEAT_TIME = @"repeatTime";
NSString *const HOUR = @"hour";
NSString *const MINUTE = @"minute";
NSString *const SECOND = @"second";

NSString *const NOTIFICATION_ID = @"NotificationId";
NSString *const PAYLOAD = @"payload";
NSString *const NOTIFICATION_LAUNCHED_APP = @"notificationLaunchedApp";
NSString *launchPayload;
NSDictionary *_launchNotification;
bool displayAlert;
bool playSound;
bool updateBadge;
bool initialized;
bool launchingAppFromNotification;
FlutterHeadlessDartRunner *headlessRunner;
NSUserDefaults *persistentState;
NSObject <FlutterPluginRegistrar> *_registrar;

+ (bool)resumingFromBackground {
    return appResumingFromBackground;
}

UILocalNotification *launchNotification;

typedef NS_ENUM(NSInteger, RepeatInterval) {
    EveryMinute,
    Hourly,
    Daily,
    Weekly
};


+ (void)registerWithRegistrar:(NSObject <FlutterPluginRegistrar> *)registrar {
    channel = [FlutterMethodChannel
            methodChannelWithName:CHANNEL
                  binaryMessenger:[registrar messenger]];
    persistentState = [NSUserDefaults standardUserDefaults];
    FlutterLocalNotificationsPlugin *instance = [[FlutterLocalNotificationsPlugin alloc] initWithChannel:channel];
    headlessRunner = [[FlutterHeadlessDartRunner alloc] init];
    // callbackChannel = [FlutterMethodChannel methodChannelWithName:CALLBACK_CHANNEL binaryMessenger:headlessRunner];
//    if (@available(iOS 10.0, *)) {
//        UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
//        center.delegate = instance;
//    }
//    if (![FIRApp defaultApp]) {
//    [FIRApp configure];
//    [FIRMessaging messaging].delegate = self;
//    }
    [registrar addApplicationDelegate:instance];
    [registrar addMethodCallDelegate:instance channel:channel];
    _registrar = registrar;
}

- (instancetype)initWithChannel:(FlutterMethodChannel *)channel {
    self = [super init];

    if (self) {
        channel = channel;
        appResumingFromBackground = NO;
//        if (![FIRApp defaultApp]) {
        [FIRApp configure];
//        }
        [FIRMessaging messaging].delegate = self;
    }
    return self;
}

- (void)initialize:(FlutterMethodCall *_Nonnull)call result:(FlutterResult _Nonnull)result {
    appResumingFromBackground = false;
    NSDictionary *arguments = [call arguments];
    if (arguments[DEFAULT_PRESENT_ALERT] != [NSNull null]) {
        displayAlert = [[arguments objectForKey:DEFAULT_PRESENT_ALERT] boolValue];
    }
    if (arguments[DEFAULT_PRESENT_SOUND] != [NSNull null]) {
        playSound = [[arguments objectForKey:DEFAULT_PRESENT_SOUND] boolValue];
    }
    if (arguments[DEFAULT_PRESENT_BADGE] != [NSNull null]) {
        updateBadge = [[arguments objectForKey:DEFAULT_PRESENT_BADGE] boolValue];
    }
    bool requestedSoundPermission = false;
    bool requestedAlertPermission = false;
    bool requestedBadgePermission = false;
    if (arguments[REQUEST_SOUND_PERMISSION] != [NSNull null]) {
        requestedSoundPermission = [arguments[REQUEST_SOUND_PERMISSION] boolValue];
    }
    if (arguments[REQUEST_ALERT_PERMISSION] != [NSNull null]) {
        requestedAlertPermission = [arguments[REQUEST_ALERT_PERMISSION] boolValue];
    }
    if (arguments[REQUEST_BADGE_PERMISSION] != [NSNull null]) {
        requestedBadgePermission = [arguments[REQUEST_BADGE_PERMISSION] boolValue];
    }
    /*if (call.arguments[ON_NOTIFICATION_CALLBACK_DISPATCHER] != [NSNull null]) {
        [self startHeadlessService:[call.arguments[CALLBACK_DISPATCHER] longValue]];
        [self setCallbackDispatcherHandle:[call.arguments[ON_NOTIFICATION_CALLBACK_DISPATCHER] longValue] key:ON_NOTIFICATION_CALLBACK_DISPATCHER];
    } else {
        [persistentState removeObjectForKey:ON_NOTIFICATION_CALLBACK_DISPATCHER];
    }*/
    if (@available(iOS 10.0, *)) {
        UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
        UNAuthorizationOptions authorizationOptions = 0;
        if (requestedSoundPermission) {
            authorizationOptions += UNAuthorizationOptionSound;
        }
        if (requestedAlertPermission) {
            authorizationOptions += UNAuthorizationOptionAlert;
        }
        if (requestedBadgePermission) {
            authorizationOptions += UNAuthorizationOptionBadge;
        }
        [center requestAuthorizationWithOptions:(authorizationOptions) completionHandler:^(BOOL granted, NSError *_Nullable error) {
            if (launchPayload != nil) {
                [FlutterLocalNotificationsPlugin handleSelectNotification:launchPayload];
            }
            result(@(granted));
        }];
    } else {
        UIUserNotificationType notificationTypes = 0;
        if (requestedSoundPermission) {
            notificationTypes |= UIUserNotificationTypeSound;
        }
        if (requestedAlertPermission) {
            notificationTypes |= UIUserNotificationTypeAlert;
        }
        if (requestedBadgePermission) {
            notificationTypes |= UIUserNotificationTypeBadge;
        }
        UIUserNotificationSettings *settings = [UIUserNotificationSettings settingsForTypes:notificationTypes categories:nil];
        [[UIApplication sharedApplication] registerUserNotificationSettings:settings];
        if (launchNotification != nil) {
            NSString *payload = launchNotification.userInfo[PAYLOAD];
            [channel invokeMethod:@"selectNotification" arguments:payload];
        }
        result(@YES);
    }
    initialized = true;
}

- (void)showNotification:(FlutterMethodCall *_Nonnull)call result:(FlutterResult _Nonnull)result {
    NotificationDetails *notificationDetails = [[NotificationDetails alloc] init];
    notificationDetails.id = call.arguments[ID];
    notificationDetails.id = call.arguments[ID];
    notificationDetails.title = call.arguments[TITLE];
    notificationDetails.body = call.arguments[BODY];
    notificationDetails.payload = call.arguments[PAYLOAD];
    notificationDetails.presentAlert = displayAlert;
    notificationDetails.presentSound = playSound;
    notificationDetails.presentBadge = updateBadge;
    if (call.arguments[PLATFORM_SPECIFICS] != [NSNull null]) {
        NSDictionary *platformSpecifics = call.arguments[PLATFORM_SPECIFICS];

        if (platformSpecifics[PRESENT_ALERT] != [NSNull null]) {
            notificationDetails.presentAlert = [[platformSpecifics objectForKey:PRESENT_ALERT] boolValue];
        }
        if (platformSpecifics[PRESENT_SOUND] != [NSNull null]) {
            notificationDetails.presentSound = [[platformSpecifics objectForKey:PRESENT_SOUND] boolValue];
        }
        if (platformSpecifics[PRESENT_BADGE] != [NSNull null]) {
            notificationDetails.presentBadge = [[platformSpecifics objectForKey:PRESENT_BADGE] boolValue];
        }
        notificationDetails.sound = platformSpecifics[SOUND];
    }
    if ([SCHEDULE_METHOD isEqualToString:call.method]) {
        notificationDetails.secondsSinceEpoch = @([call.arguments[MILLISECONDS_SINCE_EPOCH] integerValue] / 1000);
    } else if ([PERIODICALLY_SHOW_METHOD isEqualToString:call.method] || [SHOW_DAILY_AT_TIME_METHOD isEqualToString:call.method] || [SHOW_WEEKLY_AT_DAY_AND_TIME_METHOD isEqualToString:call.method]) {
        if (call.arguments[REPEAT_TIME]) {
            NSDictionary *timeArguments = (NSDictionary *) call.arguments[REPEAT_TIME];
            notificationDetails.repeatTime = [[NotificationTime alloc] init];
            if (timeArguments[HOUR] != [NSNull null]) {
                notificationDetails.repeatTime.hour = @([timeArguments[HOUR] integerValue]);
            }
            if (timeArguments[MINUTE] != [NSNull null]) {
                notificationDetails.repeatTime.minute = @([timeArguments[MINUTE] integerValue]);
            }
            if (timeArguments[SECOND] != [NSNull null]) {
                notificationDetails.repeatTime.second = @([timeArguments[SECOND] integerValue]);
            }
        }
        if (call.arguments[DAY]) {
            notificationDetails.day = @([call.arguments[DAY] integerValue]);
        }
        notificationDetails.repeatInterval = @([call.arguments[REPEAT_INTERVAL] integerValue]);
    }
    if (@available(iOS 10.0, *)) {
        [self showUserNotification:notificationDetails];
    } else {
        [self showLocalNotification:notificationDetails];
    }
    result(nil);
}

- (void)cancelNotification:(FlutterMethodCall *_Nonnull)call result:(FlutterResult _Nonnull)result {
    NSNumber *id = (NSNumber *) call.arguments;
    if (@available(iOS 10.0, *)) {
        UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
        NSArray *idsToRemove = [[NSArray alloc] initWithObjects:[id stringValue], nil];
        [center removePendingNotificationRequestsWithIdentifiers:idsToRemove];
        [center removeDeliveredNotificationsWithIdentifiers:idsToRemove];
    } else {
        NSArray *notifications = [UIApplication sharedApplication].scheduledLocalNotifications;
        for (int i = 0; i < [notifications count]; i++) {
            UILocalNotification *localNotification = [notifications objectAtIndex:i];
            NSNumber *userInfoNotificationId = localNotification.userInfo[NOTIFICATION_ID];
            if ([userInfoNotificationId longValue] == [id longValue]) {
                [[UIApplication sharedApplication] cancelLocalNotification:localNotification];
                break;
            }
        }
    }
    result(nil);
}

- (void)cancelAllNotifications:(FlutterResult _Nonnull)result {
    if (@available(iOS 10.0, *)) {
        UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
        [center removeAllPendingNotificationRequests];
        [center removeAllDeliveredNotifications];
    } else {
        [[UIApplication sharedApplication] cancelAllLocalNotifications];
    }
    result(nil);
}

- (void)handleMethodCall:(FlutterMethodCall *)call result:(FlutterResult)result {
    NSString *method = call.method;
    if ([INITIALIZE_METHOD isEqualToString:call.method]) {
        [self initialize:call result:result];
    } else if ([SHOW_METHOD isEqualToString:call.method] || [SCHEDULE_METHOD isEqualToString:call.method] || [PERIODICALLY_SHOW_METHOD isEqualToString:call.method] || [SHOW_DAILY_AT_TIME_METHOD isEqualToString:call.method]
            || [SHOW_WEEKLY_AT_DAY_AND_TIME_METHOD isEqualToString:call.method]) {
        [self showNotification:call result:result];
    } else if ([CANCEL_METHOD isEqualToString:call.method]) {
        [self cancelNotification:call result:result];
    } else if ([CANCEL_ALL_METHOD isEqualToString:call.method]) {
        [self cancelAllNotifications:result];
    } else if ([GET_NOTIFICATION_APP_LAUNCH_DETAILS_METHOD isEqualToString:call.method]) {
        NSString *payload;
        if (launchNotification != nil) {
            payload = launchNotification.userInfo[PAYLOAD];
        } else {
            payload = launchPayload;
        }
        NSDictionary *notificationAppLaunchDetails = [NSDictionary dictionaryWithObjectsAndKeys:[NSNumber numberWithBool:launchingAppFromNotification], NOTIFICATION_LAUNCHED_APP, payload, PAYLOAD, nil];
        result(notificationAppLaunchDetails);
    } else if ([INITIALIZED_HEADLESS_SERVICE_METHOD isEqualToString:call.method]) {
        result(nil);
    } else if ([@"requestNotificationPermissions" isEqualToString:method]) {
        UIUserNotificationType notificationTypes = 0;
        NSDictionary *arguments = call.arguments;
        if ([arguments[@"sound"] boolValue]) {
            notificationTypes |= UIUserNotificationTypeSound;
        }
        if ([arguments[@"alert"] boolValue]) {
            notificationTypes |= UIUserNotificationTypeAlert;
        }
        if ([arguments[@"badge"] boolValue]) {
            notificationTypes |= UIUserNotificationTypeBadge;
        }
        UIUserNotificationSettings *settings =
                [UIUserNotificationSettings settingsForTypes:notificationTypes categories:nil];
        [[UIApplication sharedApplication] registerUserNotificationSettings:settings];
        result(nil);
    } else if ([@"configure" isEqualToString:method]) {
        [[UIApplication sharedApplication] registerForRemoteNotifications];
        if (_launchNotification != nil) {
            [channel invokeMethod:@"onLaunch" arguments:_launchNotification];
        }
        result(nil);
    } else if ([@"subscribeToTopic" isEqualToString:method]) {
        NSString *topic = call.arguments;
        [[FIRMessaging messaging] subscribeToTopic:topic];
        result(nil);
    } else if ([@"unsubscribeFromTopic" isEqualToString:method]) {
        NSString *topic = call.arguments;
        [[FIRMessaging messaging] unsubscribeFromTopic:topic];
        result(nil);
    } else if ([@"getToken" isEqualToString:method]) {
        [[FIRInstanceID instanceID]
                instanceIDWithHandler:^(FIRInstanceIDResult *_Nullable instanceIDResult,
                        NSError *_Nullable error) {
                    if (error != nil) {
                        NSLog(@"getToken, error fetching instanceID: %@", error);
                        result(nil);
                    } else {
                        result(instanceIDResult.token);
                    }
                }];
    } else if ([@"deleteInstanceID" isEqualToString:method]) {
        [[FIRInstanceID instanceID] deleteIDWithHandler:^void(NSError *_Nullable error) {
            if (error.code != 0) {
                NSLog(@"deleteInstanceID, error: %@", error);
                result([NSNumber numberWithBool:NO]);
            } else {
                [[UIApplication sharedApplication] unregisterForRemoteNotifications];
                result([NSNumber numberWithBool:YES]);
            }
        }];
    } else if ([@"autoInitEnabled" isEqualToString:method]) {
        BOOL *value = [[FIRMessaging messaging] isAutoInitEnabled];
        result([NSNumber numberWithBool:value]);
    } else if ([@"setAutoInitEnabled" isEqualToString:method]) {
        NSNumber *value = call.arguments;
        [FIRMessaging messaging].autoInitEnabled = value.boolValue;
        result(nil);
    } else {
        result(FlutterMethodNotImplemented);
    }
}

/*- (void)startHeadlessService:(int64_t)handle {
    [self setCallbackDispatcherHandle:handle key:CALLBACK_DISPATCHER];
    FlutterCallbackInformation *info = [FlutterCallbackCache lookupCallbackInformation:handle];
    NSAssert(info != nil, @"failed to find callback");
    NSString *entrypoint = info.callbackName;
    NSString *uri = info.callbackLibraryPath;
    [headlessRunner runWithEntrypointAndLibraryUri:entrypoint libraryUri:uri];
    [_registrar addMethodCallDelegate:self channel:callbackChannel];
}

- (void)setCallbackDispatcherHandle:(int64_t)handle key:(NSString *)handleKey {
    [persistentState setObject:[NSNumber numberWithLongLong:handle]
                         forKey:handleKey];
}

- (int64_t)getCallbackDispatcherHandle:(NSString *) handleKey {
    id handle = [persistentState objectForKey:handleKey];
    if (handle == nil) {
        return 0;
    }
    return [handle longLongValue];
}*/

- (NSDictionary *)buildUserDict:(NSNumber *)id title:(NSString *)title presentAlert:(bool)presentAlert presentSound:(bool)presentSound presentBadge:(bool)presentBadge payload:(NSString *)payload {
    NSDictionary *userDict = [NSDictionary dictionaryWithObjectsAndKeys:id, NOTIFICATION_ID, title, TITLE, [NSNumber numberWithBool:presentAlert], PRESENT_ALERT, [NSNumber numberWithBool:presentSound], PRESENT_SOUND, [NSNumber numberWithBool:presentBadge], PRESENT_BADGE, payload, PAYLOAD, nil];
    return userDict;
}

- (void)showUserNotification:(NotificationDetails *)notificationDetails NS_AVAILABLE_IOS(10.0) {
    UNMutableNotificationContent *content = [[UNMutableNotificationContent alloc] init];
    UNNotificationTrigger *trigger;
    content.title = notificationDetails.title;
    content.body = notificationDetails.body;
    if (notificationDetails.presentSound) {
        if (!notificationDetails.sound || [notificationDetails.sound isKindOfClass:[NSNull class]]) {
            content.sound = UNNotificationSound.defaultSound;
        } else {
            content.sound = [UNNotificationSound soundNamed:notificationDetails.sound];
        }
    }
    content.userInfo = [self buildUserDict:notificationDetails.id title:notificationDetails.title presentAlert:notificationDetails.presentAlert presentSound:notificationDetails.presentSound presentBadge:notificationDetails.presentBadge payload:notificationDetails.payload];
    if (notificationDetails.secondsSinceEpoch == nil) {
        NSTimeInterval timeInterval = 0.1;
        Boolean repeats = NO;
        if (notificationDetails.repeatInterval != nil) {
            switch ([notificationDetails.repeatInterval integerValue]) {
                case EveryMinute:
                    timeInterval = 60;
                    break;
                case Hourly:
                    timeInterval = 60 * 60;
                    break;
                case Daily:
                    timeInterval = 60 * 60 * 24;
                    break;
                case Weekly:
                    timeInterval = 60 * 60 * 24 * 7;
                    break;
            }
            repeats = YES;
        }
        if (notificationDetails.repeatTime != nil) {
            NSCalendar *calendar = [[NSCalendar alloc] initWithCalendarIdentifier:NSCalendarIdentifierGregorian];
            NSDateComponents *dateComponents = [[NSDateComponents alloc] init];
            [dateComponents setCalendar:calendar];
            if (notificationDetails.day != nil) {
                [dateComponents setWeekday:[notificationDetails.day integerValue]];
            }
            [dateComponents setHour:[notificationDetails.repeatTime.hour integerValue]];
            [dateComponents setMinute:[notificationDetails.repeatTime.minute integerValue]];
            [dateComponents setSecond:[notificationDetails.repeatTime.second integerValue]];
            trigger = [UNCalendarNotificationTrigger triggerWithDateMatchingComponents:dateComponents repeats:repeats];
        } else {
            trigger = [UNTimeIntervalNotificationTrigger triggerWithTimeInterval:timeInterval
                                                                         repeats:repeats];
        }
    } else {
        NSDate *date = [NSDate dateWithTimeIntervalSince1970:[notificationDetails.secondsSinceEpoch integerValue]];
        NSCalendar *calendar = [NSCalendar currentCalendar];
        NSDateComponents *dateComponents = [calendar components:(NSCalendarUnitYear |
                NSCalendarUnitMonth |
                NSCalendarUnitDay |
                NSCalendarUnitHour |
                NSCalendarUnitMinute |
                NSCalendarUnitSecond)                  fromDate:date];
        trigger = [UNCalendarNotificationTrigger triggerWithDateMatchingComponents:dateComponents repeats:false];
    }
    UNNotificationRequest *notificationRequest = [UNNotificationRequest
            requestWithIdentifier:[notificationDetails.id stringValue] content:content trigger:trigger];
    UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
    [center addNotificationRequest:notificationRequest withCompletionHandler:^(NSError *_Nullable error) {
        if (error != nil) {
            NSLog(@"Unable to Add Notification Request");
        }
    }];

}

- (void)showLocalNotification:(NotificationDetails *)notificationDetails {
    UILocalNotification *notification = [[UILocalNotification alloc] init];
    notification.alertBody = notificationDetails.body;
    if (@available(iOS 8.2, *)) {
        notification.alertTitle = notificationDetails.title;
    }

    if (notificationDetails.presentSound) {
        if (!notificationDetails.sound || [notificationDetails.sound isKindOfClass:[NSNull class]]) {
            notification.soundName = UILocalNotificationDefaultSoundName;
        } else {
            notification.soundName = notificationDetails.sound;
        }
    }

    notification.userInfo = [self buildUserDict:notificationDetails.id title:notificationDetails.title presentAlert:notificationDetails.presentAlert presentSound:notificationDetails.presentSound presentBadge:notificationDetails.presentBadge payload:notificationDetails.payload];
    if (notificationDetails.secondsSinceEpoch == nil) {
        if (notificationDetails.repeatInterval != nil) {
            NSTimeInterval timeInterval = 0;

            switch ([notificationDetails.repeatInterval integerValue]) {
                case EveryMinute:
                    timeInterval = 60;
                    notification.repeatInterval = NSCalendarUnitMinute;
                    break;
                case Hourly:
                    timeInterval = 60 * 60;
                    notification.repeatInterval = NSCalendarUnitHour;
                    break;
                case Daily:
                    timeInterval = 60 * 60 * 24;
                    notification.repeatInterval = NSCalendarUnitDay;
                    break;
                case Weekly:
                    timeInterval = 60 * 60 * 24 * 7;
                    notification.repeatInterval = NSCalendarUnitWeekOfYear;
                    break;
            }
            if (notificationDetails.repeatTime != nil) {
                NSDate *now = [NSDate date];
                NSCalendar *calendar = [[NSCalendar alloc] initWithCalendarIdentifier:NSCalendarIdentifierGregorian];
                NSDateComponents *dateComponents = [calendar components:NSCalendarUnitYear | NSCalendarUnitMonth | NSCalendarUnitDay fromDate:now];
                [dateComponents setHour:[notificationDetails.repeatTime.hour integerValue]];
                [dateComponents setMinute:[notificationDetails.repeatTime.minute integerValue]];
                [dateComponents setSecond:[notificationDetails.repeatTime.second integerValue]];
                if (notificationDetails.day != nil) {
                    [dateComponents setWeekday:[notificationDetails.day integerValue]];
                }
                notification.fireDate = [calendar dateFromComponents:dateComponents];
            } else {
                notification.fireDate = [NSDate dateWithTimeIntervalSinceNow:timeInterval];
            }
            [[UIApplication sharedApplication] scheduleLocalNotification:notification];
            return;
        }
        [[UIApplication sharedApplication] presentLocalNotificationNow:notification];
    } else {
        notification.fireDate = [NSDate dateWithTimeIntervalSince1970:[notificationDetails.secondsSinceEpoch integerValue]];
        [[UIApplication sharedApplication] scheduleLocalNotification:notification];
    }
}


- (void)userNotificationCenter:(UNUserNotificationCenter *)center willPresentNotification:(UNNotification *)notification withCompletionHandler:(void (^)(UNNotificationPresentationOptions))completionHandler NS_AVAILABLE_IOS(10.0) {
    UNNotificationPresentationOptions presentationOptions = 0;
    NSNumber *presentAlertValue = (NSNumber *) notification.request.content.userInfo[PRESENT_ALERT];
    NSNumber *presentSoundValue = (NSNumber *) notification.request.content.userInfo[PRESENT_SOUND];
    NSNumber *presentBadgeValue = (NSNumber *) notification.request.content.userInfo[PRESENT_BADGE];
    bool presentAlert = [presentAlertValue boolValue];
    bool presentSound = [presentSoundValue boolValue];
    bool presentBadge = [presentBadgeValue boolValue];
    if (presentAlert) {
        presentationOptions |= UNNotificationPresentationOptionAlert;
    }
    if (presentSound) {
        presentationOptions |= UNNotificationPresentationOptionSound;
    }
    if (presentBadge) {
        presentationOptions |= UNNotificationPresentationOptionBadge;
    }

    /*int64_t callback = [self getCallbackDispatcherHandle:ON_NOTIFICATION_CALLBACK_DISPATCHER];
    if (callback != 0) {
        NSDictionary *arguments = [NSDictionary dictionaryWithObjectsAndKeys:@(callback),CALLBACK_DISPATCHER, notification.request.content.userInfo[NOTIFICATION_ID], ID, notification.request.content.title, TITLE, notification.request.content.body, BODY, notification.request.content.userInfo[PAYLOAD], PAYLOAD, nil];
        [callbackChannel invokeMethod:ON_NOTIFICATION_METHOD arguments:arguments];
    }*/
    completionHandler(presentationOptions);
}

+ (void)handleSelectNotification:(NSString *)payload {
    [channel invokeMethod:@"selectNotification" arguments:payload];
}

- (void)userNotificationCenter:(UNUserNotificationCenter *)center
didReceiveNotificationResponse:(UNNotificationResponse *)response
         withCompletionHandler:(void (^)(void))completionHandler NS_AVAILABLE_IOS(10.0) {
    if ([response.actionIdentifier isEqualToString:UNNotificationDefaultActionIdentifier]) {

        NSString *payload = (NSString *) response.notification.request.content.userInfo[PAYLOAD];
        if (initialized) {
            [FlutterLocalNotificationsPlugin handleSelectNotification:payload];
        } else {
            launchPayload = payload;
            launchingAppFromNotification = true;
        }

    }
}


#if defined(__IPHONE_10_0) && __IPHONE_OS_VERSION_MAX_ALLOWED >= __IPHONE_10_0

// Receive data message on iOS 10 devices while app is in the foreground.
- (void)applicationReceivedRemoteMessage:(FIRMessagingRemoteMessage *)remoteMessage {
    NSLog(@"applicationReceivedRemoteMessage");
    [self didReceiveRemoteNotification:remoteMessage.appData];
}

#endif

- (BOOL)application:(UIApplication *)application willFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    NSLog(@"willFinishLaunchingWithOptions");
    return YES;
}

- (void)applicationWillResignActive:(UIApplication *)application {
    NSLog(@"applicationWillResignActive");
}

- (void)applicationWillEnterForeground:(UIApplication *)application {
    NSLog(@"applicationWillEnterForeground");

}

- (void)applicationWillTerminate:(UIApplication *)application {
    NSLog(@"applicationWillTerminate");

}

- (void)application:(UIApplication *)application didReceiveLocalNotification:(UILocalNotification *)notification {
    NSLog(@"didReceiveLocalNotification");

}

- (void)messaging:(FIRMessaging *)messaging didReceiveMessage:(FIRMessagingRemoteMessage *)remoteMessage {
    NSLog(@"didReceiveMessage");

}

- (void)didReceiveRemoteNotification:(NSDictionary *)userInfo {
    NSLog(@"didReceiveRemoteNotification");
    if (appResumingFromBackground) {
        [channel invokeMethod:@"onResume" arguments:userInfo];
    } else {
        [channel invokeMethod:@"onMessage" arguments:userInfo];
    }
}

#pragma mark - AppDelegate

- (BOOL)          application:(UIApplication *)application
didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    if (launchOptions != nil) {
        _launchNotification = launchOptions[UIApplicationLaunchOptionsRemoteNotificationKey];
    }
    return YES;
}

- (void)applicationDidEnterBackground:(UIApplication *)application {
    appResumingFromBackground = true;
}

- (void)applicationDidBecomeActive:(UIApplication *)application {
    appResumingFromBackground = false;
    // Clears push notifications from the notification center, with the
    // side effect of resetting the badge count. We need to clear notifications
    // because otherwise the user could tap notifications in the notification
    // center while the app is in the foreground, and we wouldn't be able to
    // distinguish that case from the case where a message came in and the
    // user dismissed the notification center without tapping anything.
    // TODO(goderbauer): Revisit this behavior once we provide an API for managing
    // the badge number, or if we add support for running Dart in the background.
    // Setting badgeNumber to 0 is a no-op (= notifications will not be cleared)
    // if it is already 0,
    // therefore the next line is setting it to 1 first before clearing it again
    // to remove all
    // notifications.
    application.applicationIconBadgeNumber = 1;
    application.applicationIconBadgeNumber = 0;
}

- (bool)         application:(UIApplication *)application
didReceiveRemoteNotification:(NSDictionary *)userInfo
      fetchCompletionHandler:(void (^)(UIBackgroundFetchResult result))completionHandler {
    NSLog(@"didReceiveRemoteNotification");
    [self didReceiveRemoteNotification:userInfo];
    completionHandler(UIBackgroundFetchResultNoData);
    return YES;
}

- (void)                             application:(UIApplication *)application
didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken {
#ifdef DEBUG
    [[FIRMessaging messaging] setAPNSToken:deviceToken type:FIRMessagingAPNSTokenTypeSandbox];
#else
    [[FIRMessaging messaging] setAPNSToken:deviceToken type:FIRMessagingAPNSTokenTypeProd];
#endif
    [[FIRInstanceID instanceID] instanceIDWithHandler:^(FIRInstanceIDResult *_Nullable result,
            NSError *_Nullable error) {
        if (error != nil) {
            NSLog(@"Error fetching remote instance ID: %@", error);
        } else {
            NSLog(@"Remote instance ID token: %@", result.token);
            [channel invokeMethod:@"onToken" arguments:result.token];
        }
    }];
}

- (void)                application:(UIApplication *)application
didRegisterUserNotificationSettings:(UIUserNotificationSettings *)notificationSettings {
    NSDictionary *settingsDictionary = @{
            @"sound": [NSNumber numberWithBool:notificationSettings.types & UIUserNotificationTypeSound],
            @"badge": [NSNumber numberWithBool:notificationSettings.types & UIUserNotificationTypeBadge],
            @"alert": [NSNumber numberWithBool:notificationSettings.types & UIUserNotificationTypeAlert],
    };
    [channel invokeMethod:@"onIosSettingsRegistered" arguments:settingsDictionary];
}

- (void)          messaging:(nonnull FIRMessaging *)messaging
didReceiveRegistrationToken:(nonnull NSString *)fcmToken {
    [channel invokeMethod:@"onToken" arguments:fcmToken];
}

@end
