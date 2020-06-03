package org.infobip.mobile.messaging.mobileapi.user;

public interface DepersonalizeActionListener {
    void onUserInitiatedDepersonalizeCompleted();

    void onUserInitiatedDepersonalizeFailed(Throwable error);
}