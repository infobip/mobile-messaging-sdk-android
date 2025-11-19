/*
 * MobileMessagingTestCase.java
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.inbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import android.annotation.SuppressLint;
import android.content.Context;

import com.google.firebase.FirebaseOptions;

import org.infobip.mobile.messaging.Message;
import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.MobileMessagingCore;
import org.infobip.mobile.messaging.MobileMessagingProperty;
import org.infobip.mobile.messaging.android.MobileMessagingBaseTestCase;
import org.infobip.mobile.messaging.api.appinstance.MobileApiAppInstance;
import org.infobip.mobile.messaging.api.messages.MessageResponse;
import org.infobip.mobile.messaging.api.messages.MobileApiMessages;
import org.infobip.mobile.messaging.cloud.firebase.FirebaseAppProvider;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.MobileApiResourceProvider;
import org.infobip.mobile.messaging.notification.NotificationHandler;
import org.infobip.mobile.messaging.platform.Broadcaster;
import org.infobip.mobile.messaging.platform.Time;
import org.infobip.mobile.messaging.platform.TimeProvider;
import org.infobip.mobile.messaging.util.PreferenceHelper;
import org.junit.After;
import org.junit.Before;

import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class MobileMessagingTestCase extends MobileMessagingBaseTestCase {

    protected MobileInboxBroadcaster inboxBroadcaster;
    protected Broadcaster coreBroadcaster;
    protected TestTimeProvider time;
    protected MobileMessagingCore mobileMessagingCore;
    protected MobileMessaging mobileMessaging;
    protected NotificationHandler notificationHandler;

    protected MobileApiResourceProvider mobileApiResourceProvider;
    protected MobileApiMessages mobileApiMessages;
    protected MobileApiAppInstance mobileApiAppInstance;
    protected FirebaseAppProvider firebaseAppProvider;

    protected static class TestTimeProvider implements TimeProvider {

        long delta = 0;
        boolean overwritten = false;

        public void forward(long time, TimeUnit unit) {
            delta += unit.toMillis(time);
        }

        public void backward(long time, TimeUnit unit) {
            delta -= unit.toMillis(time);
        }

        public void reset() {
            overwritten = false;
            delta = 0;
        }

        public void set(long time) {
            overwritten = true;
            delta = time;
        }

        @Override
        public long now() {
            if (overwritten) {
                return delta;
            } else {
                return System.currentTimeMillis() + delta;
            }
        }
    }

    @SuppressLint("ApplySharedPref")
    @Before
    public void setUp() throws Exception {
        super.setUp();

        PreferenceHelper.getDefaultMMSharedPreferences(context).edit().clear().commit();

//        PreferenceHelper.saveString(context, MobileMessagingProperty.API_URI, "http://127.0.0.1:" + debugServer.getListeningPort() + "/");
        PreferenceHelper.saveString(context, MobileMessagingProperty.APPLICATION_CODE, "TestApplicationCode");
        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "TestDeviceInstanceId");
        PreferenceHelper.saveString(context, MobileMessagingProperty.CLOUD_TOKEN, "TestRegistrationId");
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED, true);

        MobileMessagingLogger.enforce();

        time = new TestTimeProvider();
        Time.reset(time);

        notificationHandler = mock(NotificationHandler.class);
        coreBroadcaster = mock(Broadcaster.class);
        mobileApiMessages = mock(MobileApiMessages.class);
        mobileApiAppInstance = mock(MobileApiAppInstance.class);
        mobileApiResourceProvider = mock(MobileApiResourceProvider.class);
        given(mobileApiResourceProvider.getMobileApiMessages(any(Context.class))).willReturn(mobileApiMessages);
        given(mobileApiResourceProvider.getMobileApiAppInstance(any(Context.class))).willReturn(mobileApiAppInstance);

        firebaseAppProvider = new FirebaseAppProvider(context);
        FirebaseOptions firebaseOptions = new FirebaseOptions.Builder().setProjectId("project_id").setApiKey("api_key").setApplicationId("application_id").build();
        firebaseAppProvider.setFirebaseOptions(firebaseOptions);

        mobileMessagingCore = MobileMessagingTestable.create(context, coreBroadcaster, mobileApiResourceProvider, firebaseAppProvider);
        mobileMessaging = mobileMessagingCore;

        inboxBroadcaster = mock(MobileInboxBroadcaster.class);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();

        time.reset();
    }

    /**
     * Generates messages with provided id
     *
     * @param messageId message id for a message
     * @return new message
     */
    protected static InboxMessage createMessage(String messageId) {
        return createMessage(messageId, "defaultTopic", false);
    }

    /**
     * Generates messages with provided ids and inbox data object
     *
     * @param messageId message id for a message
     * @param topic     inbox topic for a message
     * @param seen      message seen status
     * @return new message
     */
    protected static InboxMessage createMessage(String messageId, String topic, boolean seen) {
        Message message = new Message();
        message.setMessageId(messageId);
        message.setSentTimestamp(0);
        message.setBody("some text");

        return InboxMessage.createFrom(message, topic, seen);
    }

    protected static Inbox createInbox(int countTotal, int countUnread, List<InboxMessage> messages) {
        Inbox inbox = new Inbox();
        inbox.setCountTotal(countTotal);
        inbox.setCountUnread(countUnread);
        inbox.setMessages(messages);
        return inbox;
    }

    /**
     * Creates MessageResponse with provided topic for testing
     *
     * @param topic inbox topic of a message
     * @return new MessageResponse with topic in internalData
     */
    protected static MessageResponse createMessageResponse(String topic) {
        return createMessageResponse("messageId", topic, false);
    }

    /**
     * Creates MessageResponse with provided messageId and topic for testing
     *
     * @param messageId message id for a message
     * @param topic     inbox topic of a message
     * @return new MessageResponse with topic in internalData
     */
    protected static MessageResponse createMessageResponse(String messageId, String topic, Boolean seen) {
        String internalData = InboxDataMapper.inboxDataToInternalData(topic, seen);

        return new MessageResponse(
                messageId,        // messageId
                "Test Title",     // title
                "Test Body",      // body
                null,            // sound
                null,            // vibrate
                null,            // silent
                null,            // category
                null,            // customPayload
                internalData     // internalData
        );
    }
}
