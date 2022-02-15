package org.infobip.mobile.messaging.demo;

import androidx.core.content.ContextCompat;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.NotificationSettings;
import org.infobip.mobile.messaging.storage.SQLiteMessageStore;

/**
 * @author sslavin
 * @since 08/11/2017.
 */

public class Application extends android.app.Application {

    @Override
    public void onCreate() {
        super.onCreate();

        new MobileMessaging.Builder(this)
                .withMessageStore(SQLiteMessageStore.class)
                .withDisplayNotification(new NotificationSettings.Builder(this)
                        .withMultipleNotifications()
                        .withDefaultIcon(R.drawable.ic_notification)
                        .withColor(ContextCompat.getColor(this, R.color.red))
                        .build())
                .build();
    }
}
