/*
 * RegistrationTokenHandler.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.cloud;

import org.infobip.mobile.messaging.MobileMessagingCore;

import androidx.annotation.NonNull;

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
    public abstract void reissueToken();

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
