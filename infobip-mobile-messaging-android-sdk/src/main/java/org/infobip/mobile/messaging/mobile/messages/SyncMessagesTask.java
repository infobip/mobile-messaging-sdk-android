package org.infobip.mobile.messaging.mobile.messages;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.messages.MessageResponse;
import org.infobip.mobile.messaging.api.messages.SyncMessagesBody;
import org.infobip.mobile.messaging.api.messages.SyncMessagesResponse;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.ArrayList;

/**
 * @author pandric
 * @since 09/09/16.
 */
class SyncMessagesTask extends AsyncTask<Object, Void, SyncMessagesResult> {

    private final Context context;

    SyncMessagesTask(Context context) {
        this.context = context;
    }

    @Override
    protected SyncMessagesResult doInBackground(Object... notUsed) {
        MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);
        String deviceApplicationInstanceId = mobileMessagingCore.getDeviceApplicationInstanceId();
        if (StringUtils.isBlank(deviceApplicationInstanceId)) {
            Log.w(MobileMessaging.TAG, "Registration is not available yet");
            return new SyncMessagesResult(new SyncMessagesResponse(new ArrayList<MessageResponse>()));
        }

        try {
            String[] messageIds = mobileMessagingCore.getSyncMessagesIds();
            String[] unreportedMessageIds = mobileMessagingCore.getUnreportedMessageIds();

            SyncMessagesBody syncMessagesBody = new SyncMessagesBody(messageIds, unreportedMessageIds);
            SyncMessagesResponse syncMessagesResponse = MobileApiResourceProvider.INSTANCE.getMobileApiMessages(context).sync(deviceApplicationInstanceId, syncMessagesBody);
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
