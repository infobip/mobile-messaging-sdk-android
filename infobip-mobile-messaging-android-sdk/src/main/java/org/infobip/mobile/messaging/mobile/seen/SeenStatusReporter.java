package org.infobip.mobile.messaging.mobile.seen;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.api.messages.SeenMessages;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobile.BatchReporter;
import org.infobip.mobile.messaging.mobile.InternalSdkError;
import org.infobip.mobile.messaging.mobile.MobileApiResourceProvider;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;
import org.infobip.mobile.messaging.mobile.common.MAsyncTask;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.stats.MobileMessagingStats;
import org.infobip.mobile.messaging.stats.MobileMessagingStatsError;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.concurrent.Executor;

/**
 * @author sslavin
 * @since 25.04.2016.
 */
public class SeenStatusReporter {

    private final Context context;
    private final MobileMessagingCore mobileMessagingCore;
    private final MobileMessagingStats stats;
    private final Executor executor;
    private final Broadcaster broadcaster;
    private BatchReporter batchReporter;

    public SeenStatusReporter(Context context, MobileMessagingCore mobileMessagingCore, MobileMessagingStats stats, Executor executor, Broadcaster broadcaster) {
        this.context = context;
        this.mobileMessagingCore = mobileMessagingCore;
        this.stats = stats;
        this.executor = executor;
        this.broadcaster = broadcaster;
    }

    public void sync() {
        String[] unreportedSeenMessageIds = mobileMessagingCore.getUnreportedSeenMessageIds();
        if (unreportedSeenMessageIds.length == 0) {
            return;
        }

        batchReporter().put(new Runnable() {
            @Override
            public void run() {
                new MAsyncTask<Void, String[]>() {
                    @Override
                    public String[] run(Void[] voids) {

                        if (StringUtils.isBlank(mobileMessagingCore.getDeviceApplicationInstanceId())) {
                            MobileMessagingLogger.w("Can't report seen status without valid registration");
                            throw InternalSdkError.NO_VALID_REGISTRATION.getException();
                        }

                        String messageIDs[] = mobileMessagingCore.getUnreportedSeenMessageIds();
                        if (messageIDs.length == 0) {
                            return messageIDs;
                        }

                        SeenMessages seenMessages = SeenMessagesMapper.fromMessageIds(messageIDs);
                        MobileMessagingLogger.v("SEEN >>>", seenMessages);
                        MobileApiResourceProvider.INSTANCE.getMobileApiMessages(context).reportSeen(seenMessages);
                        MobileMessagingLogger.v("SEEN <<<");
                        mobileMessagingCore.removeUnreportedSeenMessageIds(messageIDs);
                        return messageIDs;
                    }

                    @Override
                    public void after(String[] messageIds) {
                        broadcaster.seenStatusReported(messageIds);
                    }

                    @Override
                    public void error(Throwable error) {
                        mobileMessagingCore.setLastHttpException(error);

                        MobileMessagingLogger.e("Error reporting seen status!");
                        stats.reportError(MobileMessagingStatsError.SEEN_REPORTING_ERROR);
                        broadcaster.error(MobileMessagingError.createFrom(error));
                    }
                }
                .execute(executor);
            }
        });
    }

    private BatchReporter batchReporter() {
        if (batchReporter == null) {
            batchReporter = new BatchReporter(context);
        }
        return batchReporter;
    }
}
