/*
 * MobileMessagingChatTestCase.kt
 * Mobile Messaging SDK
 *
 * Copyright (c) 2016-2025 Infobip Limited
 * Licensed under the Apache License, Version 2.0
 */
package org.infobip.mobile.messaging.chat

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseOptions
import io.mockk.every
import io.mockk.mockk
import org.infobip.mobile.messaging.MobileMessaging
import org.infobip.mobile.messaging.MobileMessagingCore
import org.infobip.mobile.messaging.MobileMessagingProperty
import org.infobip.mobile.messaging.api.appinstance.MobileApiAppInstance
import org.infobip.mobile.messaging.chat.core.InAppChatBroadcaster
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty
import org.infobip.mobile.messaging.chat.properties.PropertyHelper
import org.infobip.mobile.messaging.cloud.firebase.FirebaseAppProvider
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import org.infobip.mobile.messaging.mobileapi.MobileApiResourceProvider
import org.infobip.mobile.messaging.platform.Broadcaster
import org.infobip.mobile.messaging.platform.Time
import org.infobip.mobile.messaging.platform.TimeProvider
import org.infobip.mobile.messaging.util.PreferenceHelper
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
abstract class MobileMessagingChatTestCase() {

    protected class TestTimeProvider : TimeProvider {
        var delta: Long = 0
        var overwritten = false
        fun forward(time: Long, unit: TimeUnit) {
            delta += unit.toMillis(time)
        }

        fun backward(time: Long, unit: TimeUnit) {
            delta -= unit.toMillis(time)
        }

        fun reset() {
            overwritten = false
            delta = 0
        }

        fun set(time: Long) {
            overwritten = true
            delta = time
        }

        override fun now(): Long {
            return if (overwritten) {
                delta
            } else {
                System.currentTimeMillis() + delta
            }
        }
    }

    protected val messageBroadcaster: Broadcaster = mockk()
    protected val inAppChatBroadcaster: InAppChatBroadcaster = mockk()
    protected val mobileApiResourceProvider: MobileApiResourceProvider = mockk()
    protected val mobileApiAppInstance: MobileApiAppInstance = mockk()
    protected lateinit var propertyHelper: PropertyHelper
    protected lateinit var time: TestTimeProvider
    protected lateinit var firebaseAppProvider: FirebaseAppProvider
    protected lateinit var mobileMessaging: MobileMessaging
    protected lateinit var mobileMessagingCore: MobileMessagingCore

    protected val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    @Before
    open fun setUp() {
        PreferenceHelper.getDefaultMMSharedPreferences(context).edit().clear().commit()

//        PreferenceHelper.saveString(context, MobileMessagingProperty.API_URI, "http://127.0.0.1:" + debugServer.listeningPort + "/")
        PreferenceHelper.saveString(context, MobileMessagingProperty.APPLICATION_CODE, "TestApplicationCode")
        PreferenceHelper.saveString(context, MobileMessagingProperty.INFOBIP_REGISTRATION_ID, "TestDeviceInstanceId")
        PreferenceHelper.saveString(context, MobileMessagingProperty.UNIVERSAL_INSTALLATION_ID, "UniversalInstallationId")
        PreferenceHelper.saveString(context, MobileMessagingProperty.CLOUD_TOKEN, "TestRegistrationId")
        PreferenceHelper.saveBoolean(context, MobileMessagingProperty.CLOUD_TOKEN_REPORTED, true)

        PreferenceHelper.saveString(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ID.key , "IN_APP_CHAT_WIDGET_ID")
        PreferenceHelper.saveString(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_TITLE.key , "IN_APP_CHAT_WIDGET_TITLE")
        PreferenceHelper.saveString(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_PRIMARY_COLOR.key , "IN_APP_CHAT_WIDGET_PRIMARY_COLOR")
        PreferenceHelper.saveString(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_BACKGROUND_COLOR.key , "IN_APP_CHAT_WIDGET_BACKGROUND_COLOR")
        PreferenceHelper.saveString(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_PRIMARY_TEXT_COLOR.key , "IN_APP_CHAT_WIDGET_PRIMARY_TEXT_COLOR")
        PreferenceHelper.saveBoolean(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_MULTITHREAD.key , true)
        PreferenceHelper.saveBoolean(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_MULTICHANNEL_CONVERSATION.key , true)
        PreferenceHelper.saveBoolean(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_CALLS_ENABLED.key , true)
        PreferenceHelper.saveStringSet(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_THEMES.key , setOf("default", "dark"))
        PreferenceHelper.saveBoolean(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ATTACHMENT_ENABLED.key , true)
        PreferenceHelper.saveLong(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ATTACHMENT_MAX_SIZE.key , 10L)
        PreferenceHelper.saveStringSet(context, MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ATTACHMENT_ALLOWED_EXTENSIONS.key , setOf("jpg", "png"))
        PreferenceHelper.saveBoolean(context, MobileMessagingChatProperty.IN_APP_CHAT_ACTIVATED.key , true)
        PreferenceHelper.saveString(context, MobileMessagingChatProperty.IN_APP_CHAT_LANGUAGE.key , "IN_APP_CHAT_LANGUAGE")
        PreferenceHelper.saveInt(context, MobileMessagingChatProperty.UNREAD_CHAT_MESSAGES_COUNT.key , 0)
        propertyHelper = PropertyHelper(context)

        MobileMessagingLogger.enforce()

        time = TestTimeProvider()
        Time.reset(time)

        every { mobileApiResourceProvider.getMobileApiAppInstance(any()) } returns mobileApiAppInstance

        firebaseAppProvider = FirebaseAppProvider(context)
        val firebaseOptions = FirebaseOptions.Builder()
            .setProjectId("project_id")
            .setApiKey("api_key")
            .setApplicationId("application_id")
            .build()
        firebaseAppProvider.setFirebaseOptions(firebaseOptions)

        mobileMessagingCore = MobileMessagingTestable.create(context, messageBroadcaster, mobileApiResourceProvider, firebaseAppProvider)
        mobileMessaging = mobileMessagingCore
    }

    @After
    open fun tearDown() {
        time.reset()
    }


}