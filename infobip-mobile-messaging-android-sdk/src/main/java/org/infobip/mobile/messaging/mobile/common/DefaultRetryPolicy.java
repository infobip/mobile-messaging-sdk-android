package org.infobip.mobile.messaging.mobile.common;

import android.content.Context;

import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.mobile.common.exceptions.BackendCommunicationException;
import org.infobip.mobile.messaging.mobile.common.exceptions.BackendCommunicationExceptionWithContent;
import org.infobip.mobile.messaging.util.PreferenceHelper;

/**
 * @author sslavin
 * @since 24/07/2017.
 */

public class DefaultRetryPolicy {

    public static MRetryPolicy create(Context context) {
        return new MRetryPolicy.Builder()
                .withBackoffMultiplier(PreferenceHelper.findInt(context, MobileMessagingProperty.DEFAULT_EXP_BACKOFF_MULTIPLIER))
                .withMaxRetries(PreferenceHelper.findInt(context, MobileMessagingProperty.DEFAULT_MAX_RETRY_COUNT))
                .withRetryOn(BackendCommunicationException.class, BackendCommunicationExceptionWithContent.class)
                .build();
    }
}
