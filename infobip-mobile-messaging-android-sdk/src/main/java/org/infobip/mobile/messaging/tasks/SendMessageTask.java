package org.infobip.mobile.messaging.tasks;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.infobip.mobile.messaging.Event;
import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.messages.MoMessage;
import org.infobip.mobile.messaging.api.messages.MoMessagesBody;
import org.infobip.mobile.messaging.api.messages.MoMessagesResponse;
import org.infobip.mobile.messaging.api.support.http.serialization.JsonSerializer;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.infobip.mobile.messaging.BroadcastParameter.EXTRA_EXCEPTION;

/**
 * @author sslavin
 * @since 21/07/16.
 */
public class SendMessageTask extends AsyncTask<Message, Void, SendMessageResult>{

    private final Context context;

    public SendMessageTask(Context context) {
        this.context = context;
    }

    @Override
    protected SendMessageResult doInBackground(Message... messages) {
        MobileMessagingCore mobileMessagingCore = MobileMessagingCore.getInstance(context);

        String deviceApplicationInstanceId = mobileMessagingCore.getDeviceApplicationInstanceId();
        if (StringUtils.isBlank(deviceApplicationInstanceId)) {
            Log.e(MobileMessaging.TAG, "Can't send messages to MobileMessaging API without valid registration!");
            return new SendMessageResult(new Exception("No valid registration"));
        }

        try {
            MoMessagesBody moMessagesBody = new MoMessagesBody();
            moMessagesBody.setFrom(deviceApplicationInstanceId);

            List<MoMessage> moMessages = new ArrayList<>();
            for (Message message : messages) {
                String customPayloadString = message.getCustomPayload() != null ? message.getCustomPayload().toString() : null;
                Map<String, Object> customPayloadMap = new JsonSerializer().deserialize(customPayloadString, Map.class);
                moMessages.add(new MoMessage(message.getMessageId(), message.getDestination(), message.getBody(), customPayloadMap));
            }
            moMessagesBody.setMessages(moMessages.toArray(new MoMessage[moMessages.size()]));
            MoMessagesResponse moMessagesResponse = MobileApiResourceProvider.INSTANCE.getMobileApiMessages(context).sendMO(moMessagesBody);
            return new SendMessageResult(moMessagesResponse.getMessages());
        } catch (Exception e) {
            mobileMessagingCore.setLastHttpException(e);
            Log.e(MobileMessaging.TAG, "Error sending MO messages!", e);
            cancel(true);

            Intent sendMessagesError = new Intent(Event.API_COMMUNICATION_ERROR.getKey());
            sendMessagesError.putExtra(EXTRA_EXCEPTION, e);
            context.sendBroadcast(sendMessagesError);
            LocalBroadcastManager.getInstance(context).sendBroadcast(sendMessagesError);

            return new SendMessageResult(e);
        }
    }
}
