package org.infobip.mobile.messaging.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.infobip.mobile.messaging.Message;
import org.json.JSONObject;

/**
 * @author sslavin
 * @since 27/08/2017.
 */

public class WebContentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Message message = Message.createFrom(intent.getExtras());
        JSONObject customPayload = message.getCustomPayload();
        String url = customPayload != null ? customPayload.optString("url") : "";
        if (!url.isEmpty()) {
            openWebView(context, url);
        }
    }

    private void openWebView(Context context, String url) {
        Intent webViewIntent = new Intent(context, WebViewActivity.class);
        webViewIntent.putExtra(WebViewActivity.EXTRA_URL, url);
        webViewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(webViewIntent);
    }
}
