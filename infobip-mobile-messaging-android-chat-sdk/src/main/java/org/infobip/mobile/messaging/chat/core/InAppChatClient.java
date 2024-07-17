package org.infobip.mobile.messaging.chat.core;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.chat.attachments.InAppChatMobileAttachment;

import java.util.Locale;

/**
 * Declaration of interaction with client-side.
 */
public interface InAppChatClient {

    /**
     * Send typed message to agent from user
     *
     * @param message user message
     */
    void sendChatMessage(String message);

    /**
     * Send typed message and attachment
     * @param message user message
     * @param attachment IMAGE, VIDEO, DOCUMENT
     */
    void sendChatMessage(String message, InAppChatMobileAttachment attachment);

    /**
     * Send intermediate state of message input component
     *
     * @param draft user message draft
     */
    void sendChatMessageDraft(String draft);

    /**
     * Set language of widget
     *
     * @param locale locale contains country and language
     */
    void setLanguage(Locale locale);

    /**
     * Send contextual metadata of conversation and a InAppChatMultiThreadFlag flag
     *
     * @param data            contextual data in the form of JSON string
     * @param multiThreadFlag multithread strategy flag
     */
    void sendContextualData(String data, InAppChatMultiThreadFlag multiThreadFlag);

    /**
     * Change destination from thread to list in multiThread widget. For non multiThread widget it does nothing.
     */
    void showThreadList();

    /**
     * Close webSocket connection and be able to receive push notifications
     */
    void mobileChatPause(MobileMessaging.ResultListener<String> resultListener);

    /**
     * Resume webSocket connection
     */
    void mobileChatResume(MobileMessaging.ResultListener<String> resultListener);

    /**
     * Set widget theme. Widget themes are defined in Livechat widget in Infobip Portal, section Advanced customization.
     * @param themeName theme name to be set
     * @param resultListener action listener
     */
    void setWidgetTheme(String themeName, MobileMessaging.ResultListener<String> resultListener);
}
