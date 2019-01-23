package org.infobip.mobile.messaging.mobile.user;

public interface DepersonalizeServerListener {
    void onServerDepersonalizeStarted();

    void onServerDepersonalizeCompleted();

    void onServerDepersonalizeFailed(Throwable error);
}