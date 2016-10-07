package org.infobip.mobile.messaging.mobile.version;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.version.LatestReleaseResponse;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;

/**
 * @author sslavin
 * @since 04/10/2016.
 */

public class VersionCheckTask extends AsyncTask<Void, Void, VersionCheckResult> {
    private final Context context;

    VersionCheckTask(Context context) {
        this.context = context;
    }

    @Override
    protected VersionCheckResult doInBackground(Void... notUsed) {

        try {
            LatestReleaseResponse response = MobileApiResourceProvider.INSTANCE.getMobileApiVersion(context).getLatestRelease();
            return new VersionCheckResult(response);
        } catch (Exception e) {
            MobileMessagingCore.getInstance(context).setLastHttpException(e);
            Log.e(MobileMessaging.TAG, "Error checking latest release!", e);
            cancel(true);

            return new VersionCheckResult(e);
        }
    }
}
