package org.infobip.mobile.messaging.geo.push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.infobip.mobile.messaging.Message;


public class PushMessageReceiver extends BroadcastReceiver {

    private PushMessageHandler pushMessageHandler = new PushMessageHandler();

    @Override
    public void onReceive(Context context, Intent intent) {
        Message message = Message.createFrom(intent.getExtras());
        pushMessageHandler.handleGeoMessage(context, message);
    }
}
