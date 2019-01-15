package org.infobip.mobile.messaging.mobile.appinstance;

import org.infobip.mobile.messaging.Installation;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;

public interface InstallationActionListener {
    void onSuccess(Installation installation);

    void onError(MobileMessagingError error);
}