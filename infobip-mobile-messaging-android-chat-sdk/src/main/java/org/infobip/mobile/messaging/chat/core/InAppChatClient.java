package org.infobip.mobile.messaging.chat.core;

import org.infobip.mobile.messaging.chat.attachments.InAppChatMobileAttachment;

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
    void sendInputDraft(String draft);


    /**
     * Set language of widget
     *
     * @param language in locale format e.g.: en-US
     */
    void setLanguage(String language);


    /**
     * Send contextual metadata of conversation and a InAppChatMultiThreadFlag flag
     *
     * @param data
     * @param multitThreadFlag
     */
    void sendContextualData(String data, InAppChatMultiThreadFlag multiThreadFlag);
}
