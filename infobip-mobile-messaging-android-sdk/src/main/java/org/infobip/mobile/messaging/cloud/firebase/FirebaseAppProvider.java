/*
 * FirebaseAppProvider.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.cloud.firebase;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

/**
 * The FirebaseAppProvider checks if [DEFAULT] {@link FirebaseApp} is already initialized automatically by <a href=https://developers.google.com/android/guides/google-services-plugin>Google Services Gradle Plugin</a>
 * or tries to initialize it from {@link FirebaseOptions}, provided using {@link MobileMessaging.Builder#withFirebaseOptions(FirebaseOptions)} method.
 * <p>
 * If your Android project has some configuration that prevents usage of Google Services Gradle Plugin,
 * you can add key/values from google-services.json to resource strings (strings.xml),
 * or initialize {@link FirebaseOptions} object and provide it to {@link MobileMessaging.Builder#withFirebaseOptions(FirebaseOptions)} method.
 * <p>
 * <a href=https://developers.google.com/android/guides/google-services-plugin>Documentation of the Google Services Gradle Plugin<a/> gives the details of how to get these values from google-services.json file.
 */
public class FirebaseAppProvider {

    private Context context;
    private FirebaseOptions firebaseOptions;

    public FirebaseAppProvider(Context context) {
        this.context = context;
    }

    public FirebaseApp getFirebaseApp() {
        FirebaseApp firebaseApp;
        try {
            //Trying to get instance of FirebaseApp with FirebaseOptions from google-services.json file or from resources
            firebaseApp = FirebaseApp.getInstance();
        } catch (Exception e) {
            //Trying to get instance of FirebaseApp with FirebaseOptions provided at runtime
            //firebaseOptions shouldn't be null in this case, or it will throw an error.
            if (firebaseOptions == null) {
                MobileMessagingLogger.e("Can't initialize FirebaseApp, provide either google-services.json, or string resources in strings.xml, or call withFirebaseOptions in MobileMessaging builder");
                throw new IllegalArgumentException("FirebaseOptions aren't provided");
            }
            firebaseApp = FirebaseApp.initializeApp(context, firebaseOptions);
        }
        return firebaseApp;
    }

    public void setFirebaseOptions(FirebaseOptions firebaseOptions) {
        this.firebaseOptions = firebaseOptions;
    }
}
