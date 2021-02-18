# Mobile Messaging SDK for Android

[![Download](https://api.bintray.com/packages/infobip/maven/infobip-mobile-messaging-android-sdk/images/download.svg)](https://bintray.com/infobip/maven/infobip-mobile-messaging-android-sdk/_latestVersion)
[![License](https://img.shields.io/github/license/infobip/mobile-messaging-sdk-android.svg?label=License)](https://github.com/infobip/mobile-messaging-sdk-android/blob/master/LICENSE)

Mobile Messaging SDK is designed and developed to easily enable push notification channel in your mobile application. In almost no time of implementation you get push notification in your application and access to the features of <a href="https://www.infobip.com/en/products/mobile-app-messaging" target="_blank">Infobip Mobile Apps Messaging</a>. The document describes library integration steps. Additional information can be found in our <a href="https://github.com/infobip/mobile-messaging-sdk-android/wiki" target="_blank">Wiki</a>.

## Requirements

- Android Studio
- API Level: 14 (Android 4.0 - Ice Cream Sandwich)

## Quick start guide

1. Make sure to <a href="https://www.infobip.com/docs/mobile-app-messaging/create-mobile-application-profile" target="_blank">setup application at Infobip portal</a>, if you haven't already.
2. Add dependencies to `app/build.gradle`
    ```groovy
    dependencies {
        ...
        implementation ('org.infobip.mobile.messaging.api:infobip-mobile-messaging-android-sdk:4.3.6@aar') {
            transitive = true
        }
    }
    ```
    <img src="https://github.com/infobip/mobile-messaging-sdk-android/wiki/images/QSGGradle.png?raw=true" alt="Gradle dependencies"/>
3. Add <a href="https://www.infobip.com/docs/mobile-app-messaging/fcm-server-api-key-setup-guide" target="_blank">`Firebase Sender ID`</a> and Infobip <a href="https://dev.infobip.com/push-messaging/create-application" target="_blank">`Application Code`</a> obtained in step 1 to `values/strings.xml`
    ```groovy
    <resources>
        <string name="google_app_id">SENDER ID</string>
        <string name="infobip_application_code">APPLICATION CODE</string>
        ...
    </resources>
    ```
    <img src="https://github.com/infobip/mobile-messaging-sdk-android/wiki/images/QSGStrings.png?raw=true" alt="String resources"/>
    Do not add `google_app_id` if you're using <a href="https://developers.google.com/android/guides/google-services-plugin" target="_blank">Google Services Gradle Plugin</a> and `google-services.json`.
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




