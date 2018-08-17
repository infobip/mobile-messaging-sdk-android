package org.infobip.mobile.messaging.mobile.instance;

public interface InstanceActionListener {
    void onPrimarySetSuccess();

    void onPrimarySetError(Throwable error);
}