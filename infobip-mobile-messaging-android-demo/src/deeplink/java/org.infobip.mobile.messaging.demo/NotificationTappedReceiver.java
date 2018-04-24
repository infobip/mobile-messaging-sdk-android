package org.infobip.mobile.messaging.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.infobip.mobile.messaging.Message;
import org.json.JSONObject;

import static org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper.messageToBundle;

/**
 * @author sslavin
 * @since 13/11/2017.
 */

public class NotificationTappedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Message message = Message.createFrom(intent.getExtras());
        JSONObject customPayload = message.getCustomPayload();
        String deepLink = customPayload != null ? customPayload.optString("deeplink") : "";

        if (!deepLink.isEmpty()) {
            Intent deepLinkIntent = new Intent(Intent.ACTION_VIEW);
            deepLinkIntent.setData(Uri.parse(deepLink));
            deepLinkIntent.putExtras(messageToBundle(message));
            context.startActivity(deepLinkIntent);
        }
    }
}
