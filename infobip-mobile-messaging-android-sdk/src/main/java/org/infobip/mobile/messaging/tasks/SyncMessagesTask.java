package org.infobip.mobile.messaging.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.messages.v3.SyncMessagesBody;
import org.infobip.mobile.messaging.api.messages.v3.SyncMessagesResponse;

/**
 * @author pandric
 * @since 09/09/16.
 */
public class SyncMessagesTask extends AsyncTask<Object, Void, SyncMessagesResult> {

    private final Context context;

    public SyncMessagesTask(Context context) {
        this.context = context;
    }

    @Override
    protected SyncMessagesResult doInBackground(Object... notUsed) {
        MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);
        try {
            String deviceApplicationInstanceId = mobileMessagingCore.getDeviceApplicationInstanceId();
            String[] messageIds = mobileMessagingCore.getSyncMessagesIds();
            String[] unreportedMessageIds = mobileMessagingCore.getUnreportedMessageIds();

            SyncMessagesBody syncMessagesBody = new SyncMessagesBody(messageIds, unreportedMessageIds);
            SyncMessagesResponse syncMessagesResponse = MobileApiResourceProvider.INSTANCE.getMobileApiSyncMessages(context).syncMessages(deviceApplicationInstanceId, syncMessagesBody);
            mobileMessagingCore.removeUnreportedMessageIds(unreportedMessageIds);
            return new SyncMessagesResult(syncMessagesResponse);

        } catch (Exception e) {
            mobileMessagingCore.setLastHttpException(e);
            Log.e(MobileMessaging.TAG, "Error syncing messages!", e);
            cancel(true);

            return new SyncMessagesResult(e);
        }
    }
}
