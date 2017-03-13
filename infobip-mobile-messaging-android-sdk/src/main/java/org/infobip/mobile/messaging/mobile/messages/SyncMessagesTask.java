package org.infobip.mobile.messaging.mobile.messages;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingLogger;
import org.infobip.mobile.messaging.api.messages.MessageResponse;
import org.infobip.mobile.messaging.api.messages.SyncMessagesBody;
import org.infobip.mobile.messaging.api.messages.SyncMessagesResponse;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.ArrayList;

import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_EXCEPTION;

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
            MobileMessagingLogger.w("Registration is not available yet");
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
            MobileMessagingLogger.e("Error syncing messages!", e);
            cancel(true);

            Intent syncMessagesError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
            syncMessagesError.putExtra(EXTRA_EXCEPTION, MobileMessagingError.createFrom(e));
            context.sendBroadcast(syncMessagesError);
            LocalBroadcastManager.getInstance(context).sendBroadcast(syncMessagesError);

            return new SyncMessagesResult(e);
        }
    }
}
