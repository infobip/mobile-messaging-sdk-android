package org.infobip.mobile.messaging.chat.core;

/**
 * Declaration of interaction of inappchat-widget.html script with mobile-side.
 * All methods intended for widget invocation on mobile (for client-side) will be put here.
 */
interface InAppChatMobile {

    /**
     * Show/Hide input field
     *
     * @param isVisible
     */
    void setControlsVisibility(boolean isVisible);

    /**
     * Open attachment preview in new webView window
     * @param url link to cdn
     * @param type IMAGE, VIDEO, DOCUMENT (pdf file)
     * @param caption name of preview file
     */
    void openAttachmentPreview(String url, String type, String caption);

    /**
     * Provides Livechat widget current view name.
     * @param view name of current widget destination
     */
    void onViewChanged(String view);

    /**
     * Provides Livechat widget api call error result.
     * @param method name of widget api function that caused error
     * @param errorPayload error payload
     */
    void onWidgetApiError(String method, String errorPayload);

    /**
     * Provides Livechat widget api call success result.
     * @param method name of widget api function that returned result
     * @param successPayload result payload
     */
    void onWidgetApiSuccess(String method, String successPayload);
}
