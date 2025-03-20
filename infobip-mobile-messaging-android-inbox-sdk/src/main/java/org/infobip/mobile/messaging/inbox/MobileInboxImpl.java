package org.infobip.mobile.messaging.inbox;

import static org.infobip.mobile.messaging.api.support.util.StringUtils.isBlank;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MessageHandlerModule;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.MobileApiResourceProvider;
import org.infobip.mobile.messaging.platform.AndroidBroadcaster;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MobileInboxImpl extends MobileInbox implements MessageHandlerModule {
    @SuppressLint("StaticFieldLeak")
    private static MobileInboxImpl instance;

    private Context context;
    private AndroidBroadcaster coreBroadcaster;
    private MobileInboxBroadcaster mobileInboxBroadcaster;
    private MobileApiResourceProvider mobileApiResourceProvider;
    private MobileInboxSynchronizer mobileInboxSynchronizer;
    private InboxSeenStatusReporter inboxSeenStatusReporter;
    private final Set<String> seenMessageIds = Collections.synchronizedSet(new HashSet<>());

    public static MobileInboxImpl getInstance(Context context) {
        if (instance == null) {
            instance = MobileMessagingCore.getInstance(context).getMessageHandlerModule(MobileInboxImpl.class);
        }
        return instance;
    }

    public MobileInboxImpl() {
    }

    public MobileInboxImpl(Context context, AndroidBroadcaster coreBroadcaster,
                           MobileInboxBroadcaster mobileInboxBroadcaster,
                           MobileApiResourceProvider mobileApiResourceProvider,
                           MobileInboxSynchronizer mobileInboxSynchronizer,
                           InboxSeenStatusReporter inboxSeenStatusReporter) {
        this.context = context;
        this.coreBroadcaster = coreBroadcaster;
        this.mobileInboxBroadcaster = mobileInboxBroadcaster;
        this.mobileApiResourceProvider = mobileApiResourceProvider;
        this.mobileInboxSynchronizer = mobileInboxSynchronizer;
        this.inboxSeenStatusReporter = inboxSeenStatusReporter;
    }

    @Override
    public void fetchInbox(@NonNull String token, @NonNull String externalUserId, MobileInboxFilterOptions filterOptions, MobileMessaging.ResultListener<Inbox> messageResultListener) {
        if (isBlank(token) || isBlank(externalUserId)) {
            MobileMessagingLogger.e("[Inbox] One or more required parameters is empty. Check token and externalUserId");
            return;
        }
        mobileInboxSynchronizer().fetchInbox(token, externalUserId, filterOptions, messageResultListener);
    }

    @Override
    public void fetchInbox(@NonNull String externalUserId, MobileInboxFilterOptions filterOptions, MobileMessaging.ResultListener<Inbox> messageResultListener) {
        if (isBlank(externalUserId)) {
            MobileMessagingLogger.e("[Inbox] externalUserId was empty");
            return;
        }
        mobileInboxSynchronizer().fetchInbox(null, externalUserId, filterOptions, messageResultListener);
    }

    @Override
    public void setSeen(@NonNull String externalUserId, @NonNull String[] messageIDs, MobileMessaging.ResultListener<String[]> listener) {
        if (isBlank(externalUserId)) {
            MobileMessagingLogger.e("[Inbox] externalUserId was empty");
            return;
        }
        if (messageIDs.length == 0) {
            MobileMessagingLogger.w("[Inbox] No messages to report");
            return;
        }

        Set<String> newMessageIds = new HashSet<>();
        for (String messageId : messageIDs) {
            if (!seenMessageIds.contains(messageId)) {
                newMessageIds.add(messageId);
            }
        }

        if (newMessageIds.isEmpty()) {
            MobileMessagingLogger.w("[Inbox] All message IDs have already been reported as seen");
            return;
        }

        seenMessageIds.addAll(newMessageIds);

        String[] newMessageIdsArray = newMessageIds.toArray(new String[0]);

        inboxSeenStatusReporter().reportSeen(listener, externalUserId, MobileMessagingCore.getInstance(context).enrichMessageIdsWithTimestamp(newMessageIdsArray));
    }

    @Override
    public void init(Context appContext) {
        this.context = appContext;
    }

    @Override
    public boolean handleMessage(Message message) {
        if (!hasInbox(message)) {
            return false;
        }
        coreBroadcaster().messageReceived(message);
        MobileMessagingLogger.d("Message with id: " + message.getMessageId() + " will be handled by Inbox MessageHandler");
        return true;
    }

    synchronized private AndroidBroadcaster coreBroadcaster() {
        if (coreBroadcaster == null) {
            coreBroadcaster = new AndroidBroadcaster(context);
        }
        return coreBroadcaster;
    }

    synchronized private MobileInboxBroadcaster mobileInboxBroadcaster() {
        if (mobileInboxBroadcaster == null) {
            mobileInboxBroadcaster = new MobileInboxBroadcasterImpl(context);
        }
        return mobileInboxBroadcaster;
    }

    synchronized private MobileApiResourceProvider mobileApiResourceProvider() {
        if (mobileApiResourceProvider == null) {
            mobileApiResourceProvider = new MobileApiResourceProvider();
        }
        return mobileApiResourceProvider;
    }

    @Override
    public boolean messageTapped(Message message) {
        return false;
    }

    @Override
    public void applicationInForeground() {
        performSyncActions();
    }

    @Override
    public void cleanup() {
        cleanupInboxData();
    }

    private void cleanupInboxData() {
        mobileApiResourceProvider = null;
        mobileInboxSynchronizer = null;
        inboxSeenStatusReporter = null;
    }

    @Override
    public void depersonalize() {
        cleanupInboxData();
    }

    @Override
    public void performSyncActions() {
        //do nothing
    }

    private static boolean hasInbox(Message message) {
        if (message == null || message.getInternalData() == null) {
            return false;
        }

        try {
            JSONObject inbox = new JSONObject(message.getInternalData());
            JSONArray topic = inbox.optJSONArray("inbox");
            return topic != null && topic.length() > 0;
        } catch (JSONException e) {
            MobileMessagingLogger.e(e.getMessage());
            return false;
        }
    }

    synchronized private MobileInboxSynchronizer mobileInboxSynchronizer() {
        if (mobileInboxSynchronizer == null) {
            mobileInboxSynchronizer = new MobileInboxSynchronizer(
                    context,
                    MobileMessagingCore.getInstance(context),
                    coreBroadcaster(),
                    mobileInboxBroadcaster(),
                    mobileApiResourceProvider().getMobileApiInbox(context)
            );
        }
        return mobileInboxSynchronizer;
    }

    synchronized private InboxSeenStatusReporter inboxSeenStatusReporter() {
        if (inboxSeenStatusReporter == null) {
            inboxSeenStatusReporter = new InboxSeenStatusReporter(
                    context,
                    MobileMessagingCore.getInstance(context),
                    coreBroadcaster(),
                    mobileInboxBroadcaster(),
                    mobileApiResourceProvider().getMobileApiInbox(context)
            );
        }
        return inboxSeenStatusReporter;
    }
}
