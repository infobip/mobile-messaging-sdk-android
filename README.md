# Mobile Messaging SDK for Android

[![Download](https://api.bintray.com/packages/infobip/maven/infobip-mobile-messaging-android-sdk/images/download.svg)](https://bintray.com/infobip/maven/infobip-mobile-messaging-android-sdk/_latestVersion)
[![License](https://img.shields.io/github/license/infobip/mobile-messaging-sdk-android.svg?label=License)](https://github.com/infobip/mobile-messaging-sdk-android/blob/master/LICENSE)

Mobile Messaging SDK is designed and developed to easily enable push notification channel in your mobile application. In almost no time of implementation you get push notification in your application and access to the features of [Infobip IP Messaging Platform](https://portal.infobip.com/push/). 
The document describes library integration steps.

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
        compile ('org.infobip.mobile.messaging.api:infobip-mobile-messaging-android-sdk:1.3.14@aar') {
            transitive = true;
        }
    }
    ```

5. Add permissions and Mobile Messaging components to AndroidManifest.xml

    ```xml
    <manifest>
    
        <!-- Existing manifest entries -->
     
        <!-- Mobile Messaging permissions -->
        
        <uses-permission android:name="android.permission.INTERNET" />
        <uses-permission android:name="android.permission.WAKE_LOCK" />

        <uses-permission android:name="${applicationId}.permission.C2D_MESSAGE" />
        <permission android:name="${applicationId}.permission.C2D_MESSAGE" android:protectionLevel="signature" />
     
        <!-- Needed for push notifications that contain VIBRATE flag. Optional, but recommended. -->
        <uses-permission android:name="android.permission.VIBRATE" />
        
        <!-- /Mobile Messaging permissions -->
        
      
        <application>
        
            <!-- Existing application entries -->
     
            <!-- Mobile Messaging components -->
            
            <receiver
                    android:name="org.infobip.mobile.messaging.gcm.MobileMessagingGcmReceiver"
                    android:exported="true"
                    android:permission="com.google.android.c2dm.permission.SEND" >
                <intent-filter>
                    <action android:name="com.google.android.c2dm.intent.RECEIVE" />
                </intent-filter>
            </receiver>
            
            <service
                    android:name="org.infobip.mobile.messaging.gcm.MobileMessagingGcmIntentService"
                    android:exported="true">
            </service>
            
            <service
                    android:name="org.infobip.mobile.messaging.gcm.MobileMessagingInstanceIDListenerService"
                    android:exported="false">
                    <intent-filter>
                        <action android:name="com.google.android.gms.iid.InstanceID"/>
                        <action android:name="com.google.android.c2dm.intent.REGISTRATION"/>
                    </intent-filter>
            </service>
            
            <!-- /Mobile Messaging components -->
            
            
        </application>
    </manifest>
    ```

6. Add GCM Sender ID, obtained in step 1, and Infobip Application Code, obtained in step 2, to values/strings.xml resource file
    ```groovy
    <resources>
        <string name="google_app_id">YOUR GCM SENDER</string>
        <string name="infobip_application_code">YOUR APPLICATION CODE</string>
        ...
    </resources>
    ```

7. Add code to MainActivity#onCreate

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
                            .withMessageStore(SharedPreferencesMessageStore.class)
                            .build();
        }
         
        ...
    }
    ```
	> ### Notice
	> MobileMessaging library has geofencing service disabled by default. In order to opt-in the service, follow [this guide](https://github.com/infobip/mobile-messaging-sdk-android/wiki/Geofencing-API).

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
