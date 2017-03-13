package org.infobip.mobile.messaging.mobile.synchronizer;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.UserData;

public interface Synchronizer<T> {
    void synchronize();

    void synchronize(final MobileMessaging.ResultListener<T> listener);

    void updatePushRegistrationStatus(final Boolean enabled);

    Task getTask();
}
