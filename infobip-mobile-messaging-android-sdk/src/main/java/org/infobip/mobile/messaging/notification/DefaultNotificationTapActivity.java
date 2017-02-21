package org.infobip.mobile.messaging.notification;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.dal.bundle.BundleMessageMapper;

/**
 * Created by akadochnikov on 20/02/2017.
 */

public class DefaultNotificationTapActivity extends Activity {

    @Override
    protected void onResume() {
        finish();

        Intent intent = getIntent();
        Intent originalIntent = intent.getParcelableExtra(MobileMessagingProperty.EXTRA_NOTIFICATION_CALLBACK_ACTIVITY_INTENT.getKey());
        Message message = BundleMessageMapper.fromBundle(originalIntent.getBundleExtra(MobileMessagingProperty.EXTRA_MESSAGE.getKey()));
        if (message != null && message.getMessageId() != null) {
            MobileMessaging.getInstance(this).setMessagesSeen(message.getMessageId());
        }

        super.onResume();

        if (originalIntent != null) {
            startActivity(originalIntent);
        }
    }
}
