package org.infobip.mobile.messaging.mobile.user;

public interface LogoutServerListener {
    void onServerLogoutStarted();

    void onServerLogoutCompleted();

    void onServerLogoutFailed(Throwable error);
}