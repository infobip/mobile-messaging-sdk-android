package org.infobip.mobile.messaging.tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.mockito.Mockito;
import org.mockito.internal.verification.Times;
import org.mockito.internal.verification.VerificationOverTimeImpl;
import org.mockito.verification.VerificationMode;
import org.mockito.verification.VerificationWrapper;

import java.lang.reflect.Field;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @author sslavin
 * @since 02/03/2017.
 */

public class BroadcastReceiverMockito extends BroadcastReceiver {

    private BroadcastReceiver innerReceiverMock;
    private Semaphore semaphore;

    private BroadcastReceiverMockito() {
        innerReceiverMock = Mockito.mock(BroadcastReceiver.class);
        semaphore = new Semaphore(0);
    }

    public static BroadcastReceiver mock() {
        return new BroadcastReceiverMockito();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        innerReceiverMock.onReceive(context, intent);
        semaphore.release();
    }

    public static BroadcastReceiver verify(BroadcastReceiver mock, VerificationMode verificationMode) throws InterruptedException {
        BroadcastReceiverMockito rm = (BroadcastReceiverMockito) mock;
        long waitTimeout = getTimeout(verificationMode);
        if (waitTimeout >= 0) {
            rm.semaphore.tryAcquire(waitTimeout, TimeUnit.MILLISECONDS);
        } else if (!isNever(verificationMode)) {
            rm.semaphore.acquire();
        }
        return Mockito.verify(rm.innerReceiverMock, getNextVerificationMode(verificationMode));
    }

    static long getTimeout(VerificationMode mode) {
        try {
            Field f = VerificationWrapper.class.getDeclaredField("wrappedVerification");
            f.setAccessible(true);
            VerificationOverTimeImpl verification = (VerificationOverTimeImpl) f.get(mode);
            return verification.getDuration();
        } catch (Exception ignored) {
            return -1;
        }
    }

    static boolean isNever(VerificationMode mode) {
        return getTimes(mode) == 0;
    }

    private static int getTimes(VerificationMode mode) {
        try {
            Field f = Times.class.getDeclaredField("wantedCount");
            f.setAccessible(true);
            return (int) f.get(mode);
        } catch (Exception ignored) {
            return -1;
        }
    }

    static VerificationMode getNextVerificationMode(VerificationMode mode) {
        try {
            Field f = VerificationWrapper.class.getDeclaredField("wrappedVerification");
            f.setAccessible(true);
            VerificationOverTimeImpl verification = (VerificationOverTimeImpl) f.get(mode);
            return verification.getDelegate() != null ? verification.getDelegate() : mode;
        } catch (Exception ignored) {
            return mode;
        }
    }
}