package org.infobip.mobile.messaging.chat.core;

public enum InAppChatEvent {
    /**
     * It is triggered when In-app chat widget configuration is synced.
     */
    CHAT_CONFIGURATION_SYNCED("org.infobip.mobile.messaging.chat.CHAT_CONFIGURATION_SYNCED");

    private final String key;

    InAppChatEvent(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
