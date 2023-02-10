package org.infobip.mobile.messaging.chat.core;

/**
 * Interface for Views with lifecycle (Activity, Fragment) that manage WebView
 */
public interface InAppChatWebViewManager {

    void onPageStarted();
    void onPageFinished();
    void setControlsEnabled(boolean enabled);
    void onJSError(String message);
    void setControlsVisibility(boolean isVisible);
    void openAttachmentPreview(String url, String type, String caption);
    void setLanguage(String language);
    void onWidgetViewChanged(InAppChatWidgetView widgetView);
    void sendContextualMetaData(String data, InAppChatMultiThreadFlag multiThreadFlag);
}
