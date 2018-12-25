package org.infobip.mobile.messaging.mobile.user;

public interface LogoutActionListener {
    void onUserInitiatedLogoutCompleted();

    void onUserInitiatedLogoutFailed(Throwable error);
}