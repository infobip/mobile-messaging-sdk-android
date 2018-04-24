package org.infobip.mobile.messaging.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;

import org.infobip.mobile.messaging.Message;
import org.json.JSONObject;

/**
 * @author sslavin
 * @since 09/11/2017.
 */

public class MainActivity extends AppCompatActivity {

    public static class NotificationTappedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Message message = Message.createFrom(intent.getExtras());
            JSONObject customPayload = message.getCustomPayload();

            String url = customPayload != null ? customPayload.optString("url") : "";
            if (!TextUtils.isEmpty(url)) {
                Intent webViewIntent = new Intent(context, WebViewActivity.class);
                webViewIntent.putExtra(WebViewActivity.EXTRA_URL, url);
                webViewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(webViewIntent);
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(this.<Toolbar>findViewById(R.id.toolbar));
    }
}
