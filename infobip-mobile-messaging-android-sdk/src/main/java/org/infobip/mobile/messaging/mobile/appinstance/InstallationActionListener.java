package org.infobip.mobile.messaging.mobile.appinstance;

import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;

import java.util.List;

public interface InstallationActionListener {
    void onSuccess(List<UserData.Installation> installation);

    void onError(MobileMessagingError error);
}