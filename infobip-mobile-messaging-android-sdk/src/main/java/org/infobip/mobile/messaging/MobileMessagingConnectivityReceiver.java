package org.infobip.mobile.messaging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.VisibleForTesting;

/**
 * @author tjuric
 * @since 31/08/2017.
 */
public class MobileMessagingConnectivityReceiver extends BroadcastReceiver {

    private MobileMessagingCore mobileMessagingCore;

    public MobileMessagingConnectivityReceiver() {
    }

    @VisibleForTesting
    public MobileMessagingConnectivityReceiver(MobileMessagingCore mobileMessagingCore) {
        this.mobileMessagingCore = mobileMessagingCore;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            return;
        }

        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();

        Boolean internetConnectedBefore = mobileMessagingCore(context).getInternetConnected();
        boolean internetConnected =
                (networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED) ||
                (intent.hasExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY)
                && !intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false));
        mobileMessagingCore(context).setInternetConnected(internetConnected);

        if (null != internetConnectedBefore && !internetConnectedBefore && internetConnected) {
            mobileMessagingCore(context).retrySync();
        }
    }

    private MobileMessagingCore mobileMessagingCore(Context context) {
        if (mobileMessagingCore == null) {
            mobileMessagingCore = MobileMessagingCore.getInstance(context.getApplicationContext());
        }
        return mobileMessagingCore;
    }
}
