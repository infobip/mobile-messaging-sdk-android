# Infobip RTC Calls and UI

**Find out more about Voice/Video Calls and Infobip RTC features in the [Infobip docs](https://www.infobip.com/docs/voice-and-video).**

- [Intro](#intro)
- [Requirements](#requirements)
- [Permissions](#permissions)
- [Quick-start guide](#quick-start-guide)
- [Customizing the calls UI](#customizing-the-calls-ui)
- [Firebase Messaging Service delegation](#firebase-messaging-service-delegation)


# Intro

InfobipRtcUi is an easy-to-use library that allows you to connect to the [Infobip RTC](https://github.com/infobip/infobip-rtc-android) by building a library.

This guide assumes that you have already set up your account and your mobile app profile with [Mobile Push Notifications](https://github.com/infobip/mobile-messaging-sdk-android/blob/master/README.md) and the [Infobip RTC](https://www.infobip.com/docs/voice-and-video/webrtc#set-up-web-and-in-app-calls).

InfobipRtcUi takes care of everything: the registration of your device to receive and trigger calls and handling of the calls themselves. It also offers you a powerful user interface with every feature your customer may need:

- the ability to capture video through both, front and back camera,
- an option to mute and use the speaker,
- the ability to capture and share a screen of a mobile device,
- an option to minimize the call UI in a picture-on-picture mode, and more.

InfobipRtcUi also allows you to take control at any step of the flow, if you wish or need so. How you handle the calls is up to you. You can become a delegate for the [FirebaseMessagingService](https://firebase.google.com/docs/reference/android/com/google/firebase/messaging/FirebaseMessagingService) or use your own custom user interface.

Currently, InfobipRtcUi is oriented to calls with [Infobip Conversation agents](https://www.infobip.com/docs/api/customer-engagement/conversations-api). P2P calls and channel-specific call types are not yet supported, though you can still manually integrate them with the [Infobip RTC](https://github.com/infobip/infobip-rtc-android).

# Requirements

- Android Studio
- Supported API Levels: 21 (Android 5.0 - Lollipop) - 33 (Android 13.0 - Tiramisu)
- <a href="https://developer.android.com/jetpack/androidx/migrate" target="_blank">AndroidX</a>
- InfobipRtcUi library source and target compatibility set to Java 8.
- <a href="https://www.infobip.com/signup" target="_blank">Infobip account</a>

# Permissions

InfobipRtcUi library declares the following as [`dangerous`](https://developer.android.com/guide/topics/permissions/overview#runtime) permissions:
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

# Quick-start guide

1. Include the `InfobipRtcUi` dependency

    ```gradle
    //get latest release version https://github.com/infobip/mobile-messaging-sdk-android/releases 
    implementation ('com.infobip:infobip-rtc-ui:8.5.1') {
        transitive = true
    }
    ```

2. Set up the [Mobile Messaging SDK](https://github.com/infobip/mobile-messaging-sdk-android#quick-start-guide).

   > ### Note:
   > Obtain a Firebase configuration file (`google-services.json`) as described in [documentation](https://github.com/infobip/mobile-messaging-sdk-android/wiki/Firebase-Cloud-Messaging) and move
   > your config file into the module (app-level) root directory of your app.

3. Create your [WebRTC application](https://www.infobip.com/docs/voice-and-video/webrtc#configure-application-getting-started), define an application type, and set up push notifications. You can create the application in your [Infobip account](https://portal.infobip.com/apps/webrtc/).


4. Add Infobip <a href="https://portal.infobip.com/apps/webrtc/application/create" target="_blank">`WebRTC application ID`</a> obtained in the step 3 to `values/strings.xml`.

   ```groovy
   <resources>
      <string name="infobip_webrtc_application_id">WEBRTC APPLICATION ID</string>
      ...
   </resources>
    ```

5. Build an InfobipRtcUi SDK:

   Kotlin

    ```kotlin
    class App: android.app.Application() {
        override fun onCreate() {
            super.onCreate()
                
            InfobipRtcUi.Builder(getApplication())
                 .applicationId("WebRTC application ID") //optional step, builder provided ID has precedent over ID provided in resources
                 .enableInAppCalls() //optional step, enables calls immediately 
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
    
            new InfobipRtcUi.Builder(getApplication())
                 .applicationId("WebRTC application ID") //optional step, builder provided ID has precedent over ID provided in resources
                 .enableInAppCalls() //optional step, enables calls immediately 
                 .build();
        }
    }
    ```

    </p>
    </details>

   > ### Note:
   > `enableInAppCalls()` registers you for incoming calls. You can skip this option in the builder and register for incoming calls later using an `InfobipRtcUi` instance.

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
</style>
```

- Use a completely new user interface of your own.

  Kotlin

    ```kotlin
        InfobipRtcUi.Builder(getApplication())
            .customActivity(YourActivity.class)
            .build()
    ```

    <details><summary>expand to see Java code</summary>
    <p>

    ```java
        new InfobipRtcUi.Builder(getApplication())
            .customActivity(YourActivity.class)
            .build();
    ```

    </p>
    </details>

  > ### Note:
  > An incoming call push notification will be handled by the `InfobipRtcUi` SDK. To build your own UI, visit the [InfobipRTC wiki page](https://github.com/infobip/infobip-rtc-android/wiki) for all options.

# Firebase Messaging Service delegation

Let's assume that you use an FCM to process push from native backend and you don't want to migrate all code to use Infobip. There's still some use case where you want to send notifications directly to Firebase. If this is the case, you have your own service that extends FirebaseMessagingService. Infobip SDK also extends the same service and if your service is registered in the manifest, Infobip's service won't be used for message handling (won't be able to receive calls).

To solve this issue you need to extend `IncomingCallService` and put your current Firebase implementation there. Mobile Messaging SDK message and token handling is covered by `IncomingCallService`.
<mark>Don't forget to register your service</mark> in `Manifest.xml`.

Kotlin

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