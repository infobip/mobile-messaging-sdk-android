# Infobip RTC Calls and UI

**Find out more about Voice/Video Calls and Infobip RTC features in the [Infobip docs](https://www.infobip.com/docs/voice-and-video).**

- [Intro](#intro)
- [Requirements](#requirements)
- [Permissions](#permissions)
- [Migration guide](#migration-guide)
- [Quick start guide](#quick-start-guide)
- [Customizing the calls UI](#customizing-the-calls-ui)
- [Changing localization](#changing-localization)
- [Firebase Messaging Service delegation](#firebase-messaging-service-delegation)


# Intro

InfobipRtcUi is an easy-to-use library that allows you to connect to the [Infobip RTC](https://github.com/infobip/infobip-rtc-android) by building a library.

This guide assumes that you have already set up your account and your mobile app profile with [Mobile Push Notifications](https://github.com/infobip/mobile-messaging-sdk-android/blob/master/README.md) and the [Infobip RTC](https://www.infobip.com/docs/voice-and-video/webrtc#getstartedwith-rtc-sdk).

InfobipRtcUi takes care of everything: the registration of your device to receive and trigger calls and handling of the calls themselves. It also offers you a powerful user interface with every feature your customer may need:

- the ability to capture video through both, front and back camera,
- an option to mute and use the speaker,
- the ability to capture and share a screen of a mobile device,
- an option to minimize the call UI in a picture-on-picture mode, and more.

InfobipRtcUi also allows you to take control at any step of the flow, if you wish or need so. How you handle the calls is up to you. You can become a delegate for the [FirebaseMessagingService](https://firebase.google.com/docs/reference/android/com/google/firebase/messaging/FirebaseMessagingService) or use your own custom user interface.

# Requirements

- Android Studio
- Supported API Levels: 21 (Android 5.0 - Lollipop) - 33 (Android 13.0 - Tiramisu)
- <a href="https://developer.android.com/jetpack/androidx/migrate" target="_blank">AndroidX</a>
- InfobipRtcUi library source and target compatibility is set to Java 8.
- <a href="https://www.infobip.com/signup" target="_blank">Infobip account</a>

# Permissions

InfobipRtcUi library declares the following [`dangerous`](https://developer.android.com/guide/topics/permissions/overview#runtime) permissions:
- [`RECORD_AUDIO`](https://developer.android.com/reference/android/Manifest.permission#RECORD_AUDIO)
- [`CAMERA`](https://developer.android.com/reference/android/Manifest.permission#CAMERA)
- [`POST_NOTIFICATIONS`](https://developer.android.com/reference/android/Manifest.permission#POST_NOTIFICATIONS)

Runtime check and request for dangerous permissions is handled by library UI components.

Below, is the list of `normal` permissions declared in the library:
- [`WAKE_LOCK`](https://developer.android.com/reference/android/Manifest.permission#WAKE_LOCK)
- [`FOREGROUND_SERVICE`](https://developer.android.com/reference/android/Manifest.permission#FOREGROUND_SERVICE)
- [`VIBRATE`](https://developer.android.com/reference/android/Manifest.permission#VIBRATE)
- [`USE_FULL_SCREEN_INTENT`](https://developer.android.com/reference/android/Manifest.permission#USE_FULL_SCREEN_INTENT)
- [`FOREGROUND_SERVICE`](https://developer.android.com/reference/android/Manifest.permission#FOREGROUND_SERVICE)

# Migration guide

Following the major release of [Infobip WebRTC SDK 2.0](https://github.com/infobip/infobip-rtc-android), content and setup exclusive of [Infobip WebRTC SDK 1.x will be deprecated](https://www.infobip.com/docs/voice-and-video/webrtc#set-up-web-and-in-app-calls) on 31/10/2023. If you were using InfobipRtcUi previously (version 9.X or older), please update to the latest version and read carefully the [Quick Start Guide](#quick-start-guide) below. You will need a new setup and change function to enable calls. Things to consider:
* Portal UI for handling WebRTC has been replaced with REST API calls.
* The previous WebRTC application you used must be replaced with two new separate models: [CPaaS X Application](https://www.infobip.com/docs/cpaas-x/application-and-entity-management) and WebRTC Push Configuration.
* The WebRTC `applicationId` you used before to enable calls in mobile needs to be replaced with a new WebRTC Push `configrationId`.
* If you want to use InfobipRtcUi and [InAppChat](https://github.com/infobip/mobile-messaging-sdk-android/wiki/In%E2%80%90app-chat) together (meaning, you want to receive calls from Infobip Conversation's agents), you need to use the new `withInAppChatCalls()` builder function, instead of the previous `enableInAppCalls()`.

You can find complete list of changes in [migration guide](https://github.com/infobip/mobile-messaging-sdk-android/wiki/Migration-guide#migration-from-8x-to-9x).

# Quick-start guide

1. Include the `InfobipRtcUi` dependency

    ```gradle
    //get latest release version https://github.com/infobip/mobile-messaging-sdk-android/releases 
    implementation ('com.infobip:infobip-rtc-ui:1.11.2') {
        transitive = true
    }
    ```

2. Set up the [Mobile Messaging SDK](https://github.com/infobip/mobile-messaging-sdk-android#quick-start-guide).

   > ### Note:
   > Obtain a Firebase configuration file (`google-services.json`) as described in [documentation](https://github.com/infobip/mobile-messaging-sdk-android/wiki/Firebase-Cloud-Messaging) and move
   > your config file into the module (app-level) root directory of your app.

3. Create your CPaaS X Application and WebRTC Push Configuration resource as described in a [guide](https://www.infobip.com/docs/voice-and-video/webrtc#declare-a-webrtc-application-getstartedwith-rtc-sdk). You can create CPaaS X Application in our [REST API](https://www.infobip.com/docs/api/platform/application-entity/create-application). Then create WebRTC Push Configuration with the [REST API](https://www.infobip.com/docs/api/channels/webrtc-calls/webrtc/save-push-configuration) with `applicationId` of CPaaS X Application from previous request and your [FCM server key](https://github.com/infobip/mobile-messaging-sdk-android/wiki/Firebase-Cloud-Messaging).

4. Provide WebRTC Push Configuration id obtained in the step 3 to InfobipRtcUi. There are 2 options how to do it:
    - Introduce new string variable in resources file `values/strings.xml`.
        ```groovy
        <resources>
             <string name="infobip_webrtc_configuration_id">WEBRTC CONFIGURATION ID</string>
           ...
        </resources>
        ```
    - Provides it in runtime using builder function showed in next step.

5. Build an InfobipRtcUi SDK:

    ```kotlin
    class App: android.app.Application() {
        override fun onCreate() {
            super.onCreate()
                
            InfobipRtcUi.Builder(this)
                 .withConfigurationId("WebRTC Configuration ID") //optional step, builder provided ID has precedent over ID provided in resources
                 .withInAppChatCalls() //optional step, enables InAppChat calls  
                 .build()
        }
    }
    ```

    <details><summary>expand to see Java code</summary>
    <p>

    ```java
    public class Application extends android.app.Application {

        @Override
        public void onCreate() {
            super.onCreate();
    
            new InfobipRtcUi.Builder(this)
                 .withConfigurationId("WebRTC Configuration ID") //optional step, builder provided ID has precedent over ID provided in resources
                 .withInAppChatCalls() //optional step, enables InAppChat calls  
                 .build();
        }
    }
    ```

    </p>
    </details>


6. Register for incoming calls based on your use case:
    - If you use InfobipRtcUi together with [InAppChat](https://github.com/infobip/mobile-messaging-sdk-android/wiki/In%E2%80%90app-chat), there is prepared function you need use:

        ```kotlin
        InfobipRtcUi.getInstance(context).enableInAppChatCalls(
            successListener = {},
            errorListener = { throwable -> }
        )
        ```

        <details><summary>expand to see Java code</summary>
        <p>

        ```java
        InfobipRtcUi.getInstance(context).enableInAppChatCalls(
            () -> {},
            (throwable) -> {}
        );
        ```
        </p>
        </details>

      > ### Note:
      > There is builder function `withInAppChatCalls(successListener, errorListener)` which also registers you for incoming calls, where you don't need to use `InfobipRtcUi` instance.
    - If you plan to use InfobipRtcUi where you can define call identity and listen type on your own, there is prepared function you can use:

        ```kotlin
        InfobipRtcUi.getInstance(context).enableCalls(
            identity = "customIdentity",
            listenType = ListenType.PUSH,
            successListener = {},
            errorListener = { throwable -> }
        )
        ```

        <details><summary>expand to see Java code</summary>
        <p>

        ```java
        InfobipRtcUi.getInstance(context).enableCalls(
            "customIdentity",
            ListenType.PUSH,
            () -> {},
            (throwable) -> {}
        );
        ```
        </p>
        </details>

      > ### Note:
      > There is builder function `withCalls(identity, listenType, successListener, errorListener)` which also registers you for incoming calls, where you don't need to use `InfobipRtcUi` instance.
    - If you don't want to care about identity, you can left responsibility for picking unique identity on InfobipRtcUi. InfobipRtcUi will use per device, per installation unique `pushRegistrationId` as identity. There is also prepared function for such use case you can use:

        ```kotlin
        InfobipRtcUi.getInstance(context).enableCalls(
            successListener = {},
            errorListener = { throwable -> }
        )
        ```

        <details><summary>expand to see Java code</summary>
        <p>

        ```java
        InfobipRtcUi.getInstance(context).enableCalls(
            () -> {},
            (throwable) -> {}
        );
        ```
        </p>
        </details>

      > ### Note:
      > There is builder function `withCalls(successListener, errorListener)` which also registers you for incoming calls, where you don't need to use `InfobipRtcUi` instance.

# Customizing the calls UI

The UI for interacting with calls is important. For this reason, we offer several options of customization:
- Use our default UI that will work out of the box.
- Use the default `InfobipRtcUi` style in your `styles.xml` file to override colors and icons.

    ```xml
    <style name="InfobipRtcUi">
        <!-- color of text and elements in foreground -->
        <item name="rtc_ui_color_foreground">#ffffff</item>
        <!-- color of call notification -->
        <item name="rtc_ui_color_notification">#ff0000</item>
        <!-- background color of call action buttons excluding hangup and accept call -->
        <item name="rtc_ui_color_actions_background">#fff000<item>
        <!--  background color of call action buttons in active state -->
        <item name="rtc_ui_color_actions_background_checked">#ffff00</item>
        <!-- color of call action icon -->
        <item name="rtc_ui_color_actions_icon">#fffff0</item>
        <!-- color of call action icon in active state -->
        <item name="rtc_ui_color_actions_icon_checked">#0fffff</item>
         <!-- color of less prominent texts -->
        <item name="rtc_ui_color_text_secondary">#00ffff</item>
        <!-- background color of calls -->
        <item name="rtc_ui_color_background">#000fff</item>
        <!-- background color of toolbar in video call -->
        <item name="rtc_ui_color_overlay_background">#0000ff</item>
        <!-- background color of alerts during call -->
        <item name="rtc_ui_color_alert_background">#00000f</item>
        <!-- background color of accept call button, icon use rtc_ui_color_actions_icon -->
        <item name="rtc_ui_color_accept">#000000</item>
        <!-- background color of hangup call button, icon use rtc_ui_color_actions_icon -->
        <item name="rtc_ui_color_hangup">#00ff00</item>
        <!-- drawable references for icons and images -->
        <item name="rtc_ui_icon_unMute">@drawable/ic_unmute</item>
        <item name="rtc_ui_icon_screenShare">@drawable/ic_screen_share</item>
        <item name="rtc_ui_icon_avatar">@drawable/ic_user_grayscale</item>
        <item name="rtc_ui_icon_video">@drawable/ic_video</item>
        <item name="rtc_ui_icon_videoOff">@drawable/ic_video_off</item>
        <item name="rtc_ui_icon_speaker">@drawable/ic_speaker</item>
        <item name="rtc_ui_icon_speakerOff">@drawable/ic_speaker_off</item>
        <item name="rtc_ui_icon_mute">@drawable/ic_mute</item>
        <item name="rtc_ui_icon_accept">@drawable/ic_rtc_ui_30</item>
        <item name="rtc_ui_icon_flipCamera">@drawable/ic_flip_camera</item>
        <item name="rtc_ui_icon_endCall">@drawable/ic_end_call</item>
        <item name="rtc_ui_icon_collapse">@drawable/ic_collapse</item>
        <item name="rtc_ui_icon_decline">@drawable/ic_clear_large</item>
        <item name="rtc_ui_icon_callsIcon">@drawable/ic_calls</item>
        <item name="rtc_ui_icon_alertTriangle">@drawable/ic_alert_triangle</item>
        <!-- incoming call message customizations -->
        <item name="rtc_ui_incoming_call_headline">@string/incoming_call_headline</item>
        <item name="rtc_ui_incoming_call_headline_appearance">@style/TextAppearance.AppCompat.Title</item>
        <item name="rtc_ui_incoming_call_headline_text_color">@color/black</item>
        <item name="rtc_ui_incoming_call_headline_background">@drawable/incoming_message_background</item>
        <item name="rtc_ui_incoming_call_message">@string/incoming_call_message</item>
        <item name="rtc_ui_incoming_call_message_appearance">@style/TextAppearance.AppCompat.Title</item>
        <item name="rtc_ui_incoming_call_message_text_color">@color/black</item>
        <item name="rtc_ui_incoming_call_message_background">@drawable/incoming_message_background</item>
    </style>
    ```

- Use a completely new user interface of your own.

    ```kotlin
    InfobipRtcUi.Builder(context)
        .withCustomActivity(YourActivity.class)
        .build()
    ```

    <details><summary>expand to see Java code</summary>
    <p>

    ```java
    new InfobipRtcUi.Builder(context)
        .withCustomActivity(YourActivity.class)
        .build();
    ```

    </p>
    </details>

  > ### Note:
  > An incoming call push notification will be handled by the `InfobipRtcUi` SDK. To build your own UI, visit the [InfobipRTC wiki page](https://github.com/infobip/infobip-rtc-android/wiki) for all options.

#  Changing localization
Whole UI provided by InfobipRtcUi is by default localized for English locale, but it can be changed by providing your locale. Call's UI must be recreated to apply new locale.


```kotlin
InfobipRtcUi.getInstance(context).setLanguage(Locale("es", "ES"))
```


<details><summary>expand to see Java code</summary>
<p>

```java
InfobipRtcUi.getInstance(context).setLanguage(new Locale("es", "ES"))
```
</p>
</details>


# Firebase Messaging Service delegation

Let's assume that you use an FCM to process push-from-native backend and you don't want to migrate all code to use Infobip. There's still some use case where you want to send notifications directly to Firebase. If this is the case, you have your own service that extends FirebaseMessagingService. Infobip SDK also extends the same service and if your service is registered in the manifest, Infobip's service won't be used for message handling (won't be able to receive calls).

To solve this issue you need to extend `IncomingCallService` and put your current Firebase implementation there. Mobile Messaging SDK message and token handling is covered by `IncomingCallService`.
<mark>Don't forget to register your service</mark> in `Manifest.xml`.

```kotlin
class YourIncomingCallService : IncomingCallService() {
    override fun onMessageReceivedDelegate(message: RemoteMessage) {
        // process non-Infobip notifications here
        // TODO your code
    }
    override fun onNewTokenDelegate(token: String) {
        // process Firebase token here
        // TODO your code
    }
}
```

<details><summary>expand to see Java code</summary>
<p>

```java
class YourIncomingCallService extends IncomingCallService {
   @Override
   void onMessageReceivedDelegate(RemoteMessage message) {
      // process non-Infobip notifications here
      // TODO your code
   }
   @Override
   void onNewTokenDelegate(String token) {
      // process Firebase token here
      // TODO your code
   }
}
```

</p>
</details>


```xml
<service
    android:name=".YourIncomingCallService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```