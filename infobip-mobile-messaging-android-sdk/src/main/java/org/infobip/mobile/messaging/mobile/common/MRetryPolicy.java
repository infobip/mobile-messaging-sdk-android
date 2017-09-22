package org.infobip.mobile.messaging.mobile.common;

import android.support.annotation.NonNull;

import org.infobip.mobile.messaging.mobile.common.exceptions.BackendCommunicationException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author sslavin
 * @since 24/07/2017.
 */

public class MRetryPolicy {

    private final Set<Class<? extends Throwable>> errorsToRetryOn = new HashSet<>();

    private int maxRetries = 1;
    private int backoffMultiplier = 0;
    private MRetryPolicy() {
        errorsToRetryOn.add(BackendCommunicationException.class);
    }

    public interface ErrorCheck {

        boolean shouldRetry(@NonNull Throwable error);
    }
    boolean shouldRetry(Throwable error, int attemptsDone) {
        for (Class cls : errorsToRetryOn) {
            if (cls.isInstance(error) && attemptsDone < maxRetries) {
                return true;
            }
        }
        return false;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public Set<Class<? extends Throwable>> getErrorsToRetryOn() {
        return errorsToRetryOn;
    }

    public static class Builder {

        private MRetryPolicy retryPolicy = new MRetryPolicy();

        public Builder withMaxRetries(int maxRetries) {
            retryPolicy.maxRetries = maxRetries;
            return this;
        }

        public Builder withBackoffMultiplier(int backoffMultiplier) {
            retryPolicy.backoffMultiplier = backoffMultiplier;
            return this;
        }

        @SafeVarargs
        public final Builder withRetryOn(Class<? extends Throwable>... errorsTypes) {
            retryPolicy.errorsToRetryOn.clear();
            retryPolicy.errorsToRetryOn.addAll(Arrays.asList(errorsTypes));
            return this;
        }

        public MRetryPolicy build() {
            MRetryPolicy policy = retryPolicy;
            retryPolicy = new MRetryPolicy();
            return policy;
        }
    }
}
