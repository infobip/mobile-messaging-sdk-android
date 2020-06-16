package org.infobip.mobile.messaging.chat.core;

/**
 * Interface for Views with lifecycle (Activity, Fragment) that manage WebView
 */
public interface InAppChatWebViewManager {

    void onPageStarted();
    void onPageFinished();
    void setControlsEnabled(boolean enabled);
    void onJSError();
}
