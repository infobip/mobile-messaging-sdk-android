package org.infobip.mobile.messaging.cloud;

import androidx.annotation.NonNull;

import org.infobip.mobile.messaging.MobileMessagingCore;

/**
 * @author sslavin
 * @since 03/09/2018.
 */
public abstract class RegistrationTokenHandler {

    protected final MobileMessagingCore mobileMessagingCore;

    public RegistrationTokenHandler(MobileMessagingCore mobileMessagingCore) {
        this.mobileMessagingCore = mobileMessagingCore;
    }

    public abstract void handleNewToken(String token);
    public abstract void cleanupToken();
    public abstract void acquireNewToken();

    protected void sendRegistrationToServer(@NonNull String token) {
        String registrationId = mobileMessagingCore.getPushRegistrationId();
        boolean saveNeeded = null == registrationId ||
                null == mobileMessagingCore.getCloudToken() ||
                !token.equals(mobileMessagingCore.getCloudToken()) ||
                !mobileMessagingCore.isRegistrationIdReported() ||
                mobileMessagingCore.isPushServiceTypeChanged();

        if (saveNeeded) {
            mobileMessagingCore.setCloudToken(token);
            mobileMessagingCore.sync();
        } else {
            mobileMessagingCore.lazySync();
        }
    }
}
