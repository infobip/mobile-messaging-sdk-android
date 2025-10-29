/*
 * DownloadImageTask.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.interactive.inapp.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.security.NetworkSecurityPolicy;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.common.MMAsyncTask;

import java.io.InputStream;
import java.net.URL;

/**
 * @author sslavin
 * @since 12/04/2018.
 */
public abstract class DownloadImageTask extends MMAsyncTask<String, Void, Bitmap> {

    private final static int MAX_DOWNLOAD_ATTEMPTS = 3;

    @Override
    protected Bitmap doInBackground(String... URL) {
        String imageUrl = URL[0];
        return downloadWithRetries(imageUrl);
    }

    private Bitmap downloadWithRetries(String imageUrl) {
        int attempt = 0;
        do {
            try {
                InputStream input = new URL(imageUrl).openStream();
                return BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                MobileMessagingLogger.e("Cannot download picture: " + e.getMessage());
                // If exception is caused by http, skip retries.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted() && imageUrl.substring(0, 5).toLowerCase().startsWith("http:")) {
                        MobileMessagingLogger.w("HTTP not permitted, use https or override usesClearTextTraffic on the application level.");
                        break;
                    }
                }
            } finally {
                attempt++;
            }
        } while (attempt <= MAX_DOWNLOAD_ATTEMPTS);
        return null;
    }
}