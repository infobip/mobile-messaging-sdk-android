# Mobile Messaging SDK for Android

[![Download](https://api.bintray.com/packages/infobip/maven/infobip-mobile-messaging-android-sdk/images/download.svg)](https://bintray.com/infobip/maven/infobip-mobile-messaging-android-sdk/_latestVersion)
[![License](https://img.shields.io/github/license/infobip/mobile-messaging-sdk-android.svg?label=License)](https://github.com/infobip/mobile-messaging-sdk-android/blob/master/LICENSE)

Mobile Messaging SDK is designed and developed to easily enable push notification channel in your mobile application. In almost no time of implementation you get push notification in your application and access to the features of [Infobip IP Messaging Platform](https://portal.infobip.com/push/). The document describes library integration steps. Additional information can be found in our [wiki](https://github.com/infobip/mobile-messaging-sdk-android/wiki).

## Requirements

- Android Studio
- API Level: 14 (Android 4.0 - Ice Cream Sandwich)

## Quick start guide

This guide is designed to get you up and running with Mobile Messaging SDK integrated into your Android application.

1. Prepare your [Cloud Messaging credentials](https://github.com/infobip/mobile-messaging-sdk-android/wiki/Firebase-Cloud-Messaging) to get Sender ID and Server API Key.
2. Prepare your Infobip account (https://portal.infobip.com/push/) to get your Application Code:
    1. [Create new application](https://dev.infobip.com/v1/docs/push-introduction-create-app) on Infobip Push portal.
    2. Navigate to your Application where you will get the Application Code.
    3. Mark the "Available on Android" checkbox.
    4. Insert previously obtained GCM Server Key (Server API Key).

    <center><img src="https://github.com/infobip/mobile-messaging-sdk-android/wiki/images/GCMAppSetup.png" alt="CUP Settings"/></center>
3. Create new application in Android Studio
    * You can find more info on this link http://developer.android.com/training/basics/firstapp/creating-project.html
    
4. Add dependencies to app/build.gradle

    ```groovy
    dependencies {
        ...
        compile ('org.infobip.mobile.messaging.api:infobip-mobile-messaging-android-sdk:1.8.5@aar') {
            transitive = true;
        }
    }
    ```

5. Add GCM Sender ID, obtained in step 1, and Infobip Application Code, obtained in step 2, to values/strings.xml resource file
    ```groovy
    <resources>
        <string name="google_app_id">YOUR GCM SENDER</string>
        <string name="infobip_application_code">YOUR APPLICATION CODE</string>
        ...
    </resources>
    ```

6. Add code to MainActivity#onCreate

    ```java
    import android.os.Bundle;
    import android.support.v7.app.AppCompatActivity;
    import org.infobip.mobile.messaging.MobileMessaging;
     
    public class MainActivity extends AppCompatActivity {
     
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            
            ...
     
            new MobileMessaging.Builder(getApplication())
                            .build();
        }
         
        ...
    }
    ```
    
> ### Notice
> 1. All required manifest components are merged to application manifest automatically by manifest merger. Please include [push-related components](https://github.com/infobip/mobile-messaging-sdk-android/wiki/Android-Manifest-components#push-notifications) to manifest manually if manifest merger was disabled.
> 2. MobileMessaging library has geofencing service disabled by default. In order to opt-in the service, follow [this guide](https://github.com/infobip/mobile-messaging-sdk-android/wiki/Geofencing-API).
> 3. Keep in mind that some proprietary android versions may restrict network traffic for your app. It may in turn affect delivery of push notifications.

## Mobile Messaging APIs

### Events

Library generates intents on the following events as described in [Event](infobip-mobile-messaging-android-sdk/src/main/java/org/infobip/mobile/messaging/Event.java):

* __Message received__ - is triggered when message is received.
* __Messages sent__ - is triggered when messages are sent.
* __Registration acquired__ - is triggered when GCM registration token is received.
* __Registration created__ - is triggered when GCM registration token successfully stored on the registration server.
* __API communication error__ - is triggered on every error returned by API.
* __API validation error__ - is triggerred when there is an error during validation of input parameters (e.g.: invalid MSISDN).
* __Delivery reports sent__ - is triggered when message delivery is reported.
* __User data synced__ - is triggered when user data is successfully saved on the registration server.

### Linking MSISDN

It is recommended that you link the Telephone number (in [MSISDN](https://en.wikipedia.org/wiki/MSISDN) format).
It will give an additional opportunity to target your application users and orchestrate your campaigns with [OMNI Messaging service](https://dev.infobip.com/docs/omni-introduction) including SMS fallback feature. 

```java
UserData userData = new UserData();
userData.setMsisdn("385911234567");
MobileMessaging.getInstance(context).syncUserData(userData);
```


