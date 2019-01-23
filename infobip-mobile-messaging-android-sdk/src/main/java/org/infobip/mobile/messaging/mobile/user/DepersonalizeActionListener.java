package org.infobip.mobile.messaging.mobile.user;

public interface DepersonalizeActionListener {
    void onUserInitiatedDepersonalizeCompleted();

    void onUserInitiatedDepersonalizeFailed(Throwable error);
}