# Mobile Messaging SDK for Android

[![Download](https://img.shields.io/github/v/tag/infobip/mobile-messaging-sdk-android?label=maven%20central)](https://mvnrepository.com/artifact/com.infobip/infobip-mobile-messaging-android-sdk)
[![License](https://img.shields.io/github/license/infobip/mobile-messaging-sdk-android.svg?label=License)](https://github.com/infobip/mobile-messaging-sdk-android/blob/master/LICENSE)

Mobile Messaging SDK is designed and developed to easily enable push notification channel in your mobile application. In almost no time of implementation you get push notification in your application and access to the features of <a href="https://www.infobip.com/en/products/mobile-app-messaging" target="_blank">Infobip Mobile Apps Messaging</a>. The document describes library integration steps. Additional information can be found in our <a href="https://github.com/infobip/mobile-messaging-sdk-android/wiki" target="_blank">Wiki</a>.

## Requirements

- Android Studio
- Supported API Levels: 16 (Android 4.0 - Jellybean) - 33 (Android 13.0)
- <a href="https://developer.android.com/jetpack/androidx/migrate" target="_blank">AndroidX</a>

## Quick start guide

1. Make sure to <a href="https://www.infobip.com/docs/mobile-app-messaging/create-mobile-application-profile" target="_blank">setup application at Infobip portal</a>, if you haven't already.
2. Add dependencies to `app/build.gradle`
    ```groovy
    dependencies {
        ...
        implementation ('com.infobip:infobip-mobile-messaging-android-sdk:7.2.2@aar') {
            transitive = true
        }
    }
    ```
    <img src="https://github.com/infobip/mobile-messaging-sdk-android/wiki/images/QSGGradle.png?raw=true" alt="Gradle dependencies"/>
3. Add a Firebase configuration file as described in <a href="https://firebase.google.com/docs/cloud-messaging/android/client#add_a_firebase_configuration_file" target="_blank">`Firebase documentation`</a>

> ### Notice: 
> Check <a href="https://github.com/infobip/mobile-messaging-sdk-android/wiki/Applying-Firebase-configuration-in-MobileMessaging-SDK">Applying Firebase configuration in MobileMessaging SDK Guide</a> for alternatives.

4. Add Infobip <a href="https://dev.infobip.com/push-messaging/create-application" target="_blank">`Application Code`</a> obtained in step 1 to `values/strings.xml`
    ```groovy
    <resources>
        <string name="infobip_application_code">APPLICATION CODE</string>
        ...
    </resources>
    ```
    <img src="https://github.com/infobip/mobile-messaging-sdk-android/wiki/images/QSGStrings.png?raw=true" alt="String resources"/>
4. Add code to `MainActivity#onCreate`

    ```java
    public class MainActivity extends AppCompatActivity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            new MobileMessaging
                    .Builder(getApplication())
                    .build();
        }
    }
    ```
    <img src="https://github.com/infobip/mobile-messaging-sdk-android/wiki/images/QSGActivity.png?raw=true" alt="String resources"/>

> ### Notice:
> Since Android 13+ we are using default notification permission request dialog.
> It is possible to trigger permission request later, to learn how please follow: <a href="https://github.com/infobip/mobile-messaging-sdk-android/wiki/Android-13-Notification-Permission-Handling" target="_blank">Android 13 notification permission handling</a>.


<br>
<p align="center"><b>NEXT STEPS: <a href="https://github.com/infobip/mobile-messaging-sdk-android/wiki/User-profile">User profile</a></b></p>
<br>

> ### Notes
> 1. All required manifest components are merged to application manifest automatically by manifest merger. Please include <a href="https://github.com/infobip/mobile-messaging-sdk-android/wiki/Android-Manifest-components#push-notifications" target="_blank">push-related components</a> to manifest manually if manifest merger was disabled.
> 2. MobileMessaging library has geofencing service disabled by default. In order to opt-in the service, follow <a href="https://github.com/infobip/mobile-messaging-sdk-android/wiki/Geofencing-API" target="_blank">this guide</a>.
> 3. Keep in mind that some proprietary android versions may restrict network traffic for your app. It may in turn affect delivery of push notifications.

<br>

| If you have any questions or suggestions, feel free to send an email to support@infobip.com or create an <a href="https://github.com/infobip/mobile-messaging-sdk-android/issues" target="_blank">issue</a>. |
|---|




