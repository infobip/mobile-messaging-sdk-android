#Mobile Messaging SDK for Android

Mobile Messaging SDK easily enables push notification channel in your mobile application.
It gives your application access to the features of [Infobip IP Messaging Platform](https://portal.infobip.com/push/).

##Supported versions
We support Android 4.0 "Ice Cream Sandwich" and later!

##Quick start guide

1. Create new application in Android Studio

    * You can find more info on this link http://developer.android.com/training/basics/firstapp/creating-project.html
    
2. Add Infobip maven repo to the root build.gradle file

    ```groovy
    allprojects {
        repositories {
            ...
            maven {
                url  "http://dl.bintray.com/infobipmobile/maven"
            }
        }
    }
    ```

3. Add dependencies to app/build.gradle

    ```groovy
    dependencies {
        ...
        compile 'org.infobip.mobile.messaging.api:infobip-mobile-messaging-android-sdk:0.9.2@aar'
        compile 'org.infobip.mobile.messaging.api:infobip-mobile-messaging-api-java:0.9.2'
        compile 'com.google.android.gms:play-services:8.4.0'
    }
    ```

4. Add permissions and services to AndroidManifest.xml

    ```xml
    <?xml version="1.0" encoding="utf-8"?>
    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
              package="<your-package-name>">
     
        <uses-permission android:name="android.permission.INTERNET" />
        <uses-permission android:name="android.permission.WAKE_LOCK" />
        <uses-permission android:name="android.permission.GET_ACCOUNTS" />
        <uses-permission android:name="com.google.android.c2dm.permission.RECEIVE" />
     
        <permission android:name="<your-package-name>.permission.C2D_MESSAGE" android:protectionLevel="signature" />
        <uses-permission android:name="<your-package-name>.permission.C2D_MESSAGE" />
     
        <!-- Needed for push notifications that contain VIBRATE flag. Optional, but recommended. -->
        <uses-permission android:name="android.permission.VIBRATE" />
        <application
                android:allowBackup="true"
                android:icon="@mipmap/ic_launcher"
                android:label="@string/app_name"
                android:supportsRtl="true"
                android:theme="@style/AppTheme">
     
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
            ...
        </application>
    </manifest>
    ```

5. Add GCM Sender ID and Infobip Application Code to values/strings.xml resource file
    ```groovy
    <resources>
        <string name="google_app_id">YOUR GCM SENDER</string>
        <string name="infobip_application_code">YOUR APPLICATION CODE</string>
        ...
    </resources>
    ```
    * You can generate the GCM Sender ID And Server API Key here: https://developers.google.com/mobile/add?platform=android&cntapi=gcm
    * You can generate the Application Code by creating the application here: https://portal.ioinfobip.com/push/applications
    * You need to enter your Application Code when you create the application!

6. Add code to MainActivity#onCreate

    ```java
    import android.os.Bundle;
    import android.support.design.widget.FloatingActionButton;
    import android.support.design.widget.Snackbar;
    import android.support.v7.app.AppCompatActivity;
    import android.support.v7.widget.Toolbar;
    import android.view.View;
    import android.view.Menu;
    import android.view.MenuItem;
    import org.infobip.mobile.messaging.MobileMessaging;
     
    public class MainActivity extends AppCompatActivity {
     
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            
            ...
     
            new MobileMessaging.Builder(this).build();
        }
         
        ...
    }
    ```
    
##Events

###Message received

Library generates intents on the following events as described in [Event](src/main/java/org/infobip/mobile/messaging/Event.java):

* __Message received__ - is triggered when message is received.
* __Registration acquired__ - is triggered when GCM registration token is received.
* __Registration created__ - is triggered when GCM registration token successfully stored on the registration server.
* __API communication error__ - is triggered on every error returned by API.
* __Delivery reports sent__ - is triggered when message delivery is reported.
