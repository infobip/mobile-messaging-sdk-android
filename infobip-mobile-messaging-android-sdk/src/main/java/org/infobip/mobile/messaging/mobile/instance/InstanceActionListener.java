package org.infobip.mobile.messaging.mobile.instance;

public interface InstanceActionListener {
    void onSuccess(boolean isPrimary);

    void onError(Throwable error);
}