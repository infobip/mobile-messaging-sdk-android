package org.infobip.mobile.messaging.mobile.messages;

import android.content.Context;
import android.os.AsyncTask;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.api.messages.MessageResponse;
import org.infobip.mobile.messaging.api.messages.SyncMessagesBody;
import org.infobip.mobile.messaging.api.messages.SyncMessagesResponse;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.ArrayList;

/**
 * @author pandric
 * @since 09/09/16.
 */
class SyncMessagesTask extends AsyncTask<Object, Void, SyncMessagesResult> {

    private final Context context;
    private final Broadcaster broadcaster;

    SyncMessagesTask(Context context, Broadcaster broadcaster) {
        this.context = context;
        this.broadcaster = broadcaster;
    }

    @Override
    protected SyncMessagesResult doInBackground(Object... notUsed) {
        MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);
        if (StringUtils.isBlank(mobileMessagingCore.getDeviceApplicationInstanceId())) {
            MobileMessagingLogger.w("Registration is not available yet");
            return new SyncMessagesResult(new SyncMessagesResponse(new ArrayList<MessageResponse>()));
        }

        String[] messageIds = mobileMessagingCore.getSyncMessagesIds();
        String[] unreportedMessageIds = mobileMessagingCore.getAndRemoveUnreportedMessageIds();

        try {
            SyncMessagesBody syncMessagesBody = new SyncMessagesBody(messageIds, unreportedMessageIds);
            SyncMessagesResponse syncMessagesResponse = MobileApiResourceProvider.INSTANCE.getMobileApiMessages(context).sync(syncMessagesBody);
            broadcaster.deliveryReported(messageIds);
            return new SyncMessagesResult(syncMessagesResponse);

        } catch (Exception e) {
            mobileMessagingCore.addUnreportedMessageIds(unreportedMessageIds);
            mobileMessagingCore.setLastHttpException(e);
            MobileMessagingLogger.e("Error syncing messages!", e);
            cancel(true);

            return new SyncMessagesResult(e);
        }
    }
}
