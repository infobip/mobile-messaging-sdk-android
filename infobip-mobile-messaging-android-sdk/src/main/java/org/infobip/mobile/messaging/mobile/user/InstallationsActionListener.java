package org.infobip.mobile.messaging.mobile.user;

import org.infobip.mobile.messaging.UserData;
import org.infobip.mobile.messaging.mobile.MobileMessagingError;

import java.util.List;

public interface InstallationsActionListener {
    void onSuccess(List<UserData.Installation> installations);

    void onError(MobileMessagingError error);
}