package org.infobip.mobile.messaging.mobile.data;

public interface LogoutActionListener {
    void onUserInitiatedLogoutCompleted();

    void onUserInitiatedLogoutFailed(Throwable error);
}