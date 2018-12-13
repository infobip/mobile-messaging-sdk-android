package org.infobip.mobile.messaging.mobile.appinstance;

public interface InstallationActionListener {
    void onSuccess(Installation installation);

    void onError(Throwable error);
}