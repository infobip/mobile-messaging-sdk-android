package org.infobip.mobile.messaging.mobileapi.user;

public interface DepersonalizeServerListener {
    void onServerDepersonalizeStarted();

    void onServerDepersonalizeCompleted();

    void onServerDepersonalizeFailed(Throwable error);
}