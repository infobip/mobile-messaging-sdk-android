/*
 * InAppChatEvent.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat.core;

public enum InAppChatEvent {
    /**
     * It is triggered when In-app chat widget configuration is synced.
     */
    CHAT_CONFIGURATION_SYNCED("org.infobip.mobile.messaging.chat.CHAT_CONFIGURATION_SYNCED"),

    /**
     * It is triggered when the number of unread messages is changed.
     * <p>
     * Contains unread messages count.
     * <pre>
     * {@code
     * int unreadMessagesCount = intent.getIntExtra(BroadcastParameter.EXTRA_UNREAD_CHAT_MESSAGES_COUNT);
     * }
     * </pre>
     */
    UNREAD_MESSAGES_COUNTER_UPDATED("org.infobip.mobile.messaging.chat.UNREAD_MESSAGES_COUNTER_UPDATED"),

    /**
     * It is triggered when the view in the InAppChat is changed.
     * <p>
     * Contains current InAppChat view name.
     * <pre>
     * {@code
     * String viewName = intent.getStringExtra(BroadcastParameter.EXTRA_CHAT_VIEW);
     * LivechatWidgetView livechatWidgetView = LivechatWidgetView.valueOf(viewName)
     * }
     * </pre>
     */
    CHAT_VIEW_CHANGED("org.infobip.mobile.messaging.chat.CHAT_VIEW_CHANGED"),

    /**
     * It is triggered when livechat registration id is updated.
     * <p>
     * Contains livechat registration id.
     * <pre>
     * {@code
     * String livechatRegistrationId = intent.getStringExtra(BroadcastParameter.EXTRA_LIVECHAT_REGISTRATION_ID);
     * }
     * </pre>
     */
    LIVECHAT_REGISTRATION_ID_UPDATED("org.infobip.mobile.messaging.chat.LIVECHAT_REGISTRATION_ID_UPDATED"),

    /**
     * It is triggered when chat availability is updated.
     * <p>
     * Contains chat availability flag.
     * <pre>
     * {@code
     * boolean isChatAvailable = intent.getBooleanExtra(BroadcastParameter.EXTRA_IS_CHAT_AVAILABLE);
     * }
     * </pre>
     */
    IN_APP_CHAT_AVAILABILITY_UPDATED("org.infobip.mobile.messaging.chat.CHAT_AVAILABILITY_UPDATED");
    private final String key;

    InAppChatEvent(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
