package org.infobip.mobile.messaging.cloud;

import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.util.StringUtils;

/**
 * @author sslavin
 * @since 03/09/2018.
 */
public abstract class RegistrationTokenHandler {

    protected final MobileMessagingCore mobileMessagingCore;

    public RegistrationTokenHandler(MobileMessagingCore mobileMessagingCore) {
        this.mobileMessagingCore = mobileMessagingCore;
    }

    public abstract void handleNewToken(String senderId, String token);
    public abstract void cleanupToken(String senderId);
    public abstract void acquireNewToken(String senderId);

    protected void sendRegistrationToServer(String token) {
        if (StringUtils.isBlank(token)) {
            return;
        }

        String registrationId = mobileMessagingCore.getPushRegistrationId();
        boolean saveNeeded = null == registrationId ||
                null == mobileMessagingCore.getCloudToken() ||
                !token.equals(mobileMessagingCore.getCloudToken()) ||
                !mobileMessagingCore.isRegistrationIdReported() ||
                mobileMessagingCore.isPushServiceTypeChanged();

        if (saveNeeded) {
            mobileMessagingCore.setCloudToken(token);
        }

        mobileMessagingCore.sync();
    }
}
