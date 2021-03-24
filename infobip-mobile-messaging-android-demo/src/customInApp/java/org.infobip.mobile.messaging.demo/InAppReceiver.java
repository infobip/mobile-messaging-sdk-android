package org.infobip.mobile.messaging.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.interactive.MobileInteractive;

public class InAppReceiver extends BroadcastReceiver {
    final static String TAG = "InAppReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            Log.d(TAG, "Can't get extras from Intent");
            return;
        }

        Message message = Message.createFrom(intent.getExtras());
        if (message == null) {
            Log.d(TAG, "Can't get message from Intent");
            return;
        }
        MobileInteractive.getInstance(context).displayInAppDialogFor(message);
    }
}
