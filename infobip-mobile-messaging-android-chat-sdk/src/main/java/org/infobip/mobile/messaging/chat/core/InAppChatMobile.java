package org.infobip.mobile.messaging.chat.core;

/**
 * Declaration of interaction of inappchat-widget.html script with mobile-side.
 * All methods intended for widget invocation on mobile (for client-side) will be put here.
 */
interface InAppChatMobile {

    /**
     * Enables/disables control elements (such as text input view, send button etc).
     * Will be applied after widget initialization.
     *
     * @param isEnabled set to true to enable control elements or to false otherwise
     */
    void setControlsEnabled(boolean isEnabled);

    /**
     * Provides widget (client-side) errors.
     *
     * @param errorMessage description of an error
     */
    void onError(String errorMessage);

    /**
     * Open attachment preview in new webView window
     * @param url link to cdn
     * @param type IMAGE, VIDEO, DOCUMENT (pdf file)
     * @param caption name of preview file
     */
    void openAttachmentPreview(String url, String type, String caption);
}
