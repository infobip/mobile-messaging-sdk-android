package org.infobip.mobile.messaging.mobile.common;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.mobile.common.exceptions.BackendCommunicationException;
import org.infobip.mobile.messaging.mobile.common.exceptions.BackendCommunicationExceptionWithContent;
import org.infobip.mobile.messaging.util.PreferenceHelper;

/**
 * @author sslavin
 * @since 28/11/2017.
 */

public class RetryPolicyProvider {

    private final MRetryPolicy defaultPolicy;
    private final MRetryPolicy oneRetryPolicy;
    private final MRetryPolicy noRetryPolicy;

    public RetryPolicyProvider(Context context) {
        this.defaultPolicy = new MRetryPolicy.Builder()
                .withBackoffMultiplier(PreferenceHelper.findInt(context, MobileMessagingProperty.DEFAULT_EXP_BACKOFF_MULTIPLIER))
                .withMaxRetries(PreferenceHelper.findInt(context, MobileMessagingProperty.DEFAULT_MAX_RETRY_COUNT))
                .withRetryOn(BackendCommunicationException.class, BackendCommunicationExceptionWithContent.class)
                .build();
        this.oneRetryPolicy = new MRetryPolicy.Builder()
                .withMaxRetries(1)
                .withBackoffMultiplier(1)
                .withRetryOn(BackendCommunicationException.class)
                .build();
        this.noRetryPolicy = new MRetryPolicy.Builder()
                .withMaxRetries(0)
                .build();
    }

    public MRetryPolicy DEFAULT() {
        return defaultPolicy;
    }

    public MRetryPolicy ONE_RETRY() {
        return oneRetryPolicy;
    }

    public MRetryPolicy NO_RETRY() {
        return noRetryPolicy;
    }
}
