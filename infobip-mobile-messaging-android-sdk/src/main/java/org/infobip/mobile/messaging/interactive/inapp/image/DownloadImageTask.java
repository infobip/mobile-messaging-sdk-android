package org.infobip.mobile.messaging.interactive.inapp.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import org.infobip.mobile.messaging.logging.MobileMessagingLogger;

import java.io.InputStream;
import java.net.URL;

/**
 * @author sslavin
 * @since 12/04/2018.
 */
public abstract class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {

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
            } finally {
                attempt++;
            }
        } while (attempt <= MAX_DOWNLOAD_ATTEMPTS);
        return null;
    }
}