package org.infobip.mobile.messaging.mobile.data;

public interface LogoutServerListener {
    void onServerLogoutStarted();

    void onServerLogoutCompleted();

    void onServerLogoutFailed(Throwable error);
}