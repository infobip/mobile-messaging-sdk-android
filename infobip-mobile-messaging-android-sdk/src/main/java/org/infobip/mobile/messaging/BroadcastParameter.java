/*
 * BroadcastParameter.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging;

/**
 * @author pandric
 * @since 19.05.16.
 */
public final class BroadcastParameter {

    public static final String EXTRA_EXCEPTION = "org.infobip.mobile.messaging.exception";
    public static final String EXTRA_CLOUD_TOKEN = "org.infobip.mobile.messaging.gcm.token";
    public static final String EXTRA_INFOBIP_ID = "org.infobip.mobile.messaging.infobip.token";
    public static final String EXTRA_MESSAGE_IDS = "org.infobip.mobile.messaging.message.ids";
    public static final String EXTRA_MESSAGE = "org.infobip.mobile.messaging.message";
    public static final String EXTRA_MESSAGES = "org.infobip.mobile.messaging.messages";
    public static final String EXTRA_USER = "org.infobip.mobile.messaging.user";
    public static final String EXTRA_SYSTEM_DATA = "org.infobip.mobile.messaging.systemdata";
    public static final String EXTRA_PLAY_SERVICES_ERROR_CODE = "org.infobip.mobile.messaging.play.services";
    public static final String EXTRA_TAPPED_ACTION = "org.infobip.mobile.messaging.notification.action.tapped";
    public static final String EXTRA_TAPPED_CATEGORY = "org.infobip.mobile.messaging.notification.category.tapped";
    public static final String EXTRA_NOTIFICATION_ID = "org.infobip.mobile.messaging.notification.id";
    public static final String EXTRA_INSTALLATION = "org.infobip.mobile.messaging.installation";

    public static final String EXTRA_UNREAD_CHAT_MESSAGES_COUNT = "org.infobip.mobile.messaging.unread.chat.messages.count";
    public static final String EXTRA_CHAT_VIEW = "org.infobip.mobile.messaging.chat.view";
    public static final String EXTRA_LIVECHAT_REGISTRATION_ID = "org.infobip.mobile.messaging.livechat.registration.id";
    public static final String EXTRA_IS_CHAT_AVAILABLE = "org.infobip.mobile.messaging.is.chat.available";

    public static final String EXTRA_INBOX = "org.infobip.mobile.messaging.inbox";
    public static final String EXTRA_INBOX_SEEN_IDS = "org.infobip.mobile.messaging.inbox.seen.ids";

    public static final int NOTIFICATION_NOT_DISPLAYED_ID = -1;

    private BroadcastParameter() {
    }
}
