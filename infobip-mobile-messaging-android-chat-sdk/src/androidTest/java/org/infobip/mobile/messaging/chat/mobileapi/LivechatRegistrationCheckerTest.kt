package org.infobip.mobile.messaging.chat.mobileapi

import android.os.Handler
import android.os.Looper
import org.infobip.mobile.messaging.MobileMessagingProperty
import org.infobip.mobile.messaging.api.appinstance.LivechatContactInformation
import org.infobip.mobile.messaging.api.appinstance.LivechatDestination
import org.infobip.mobile.messaging.api.support.ApiIOException
import org.infobip.mobile.messaging.chat.MobileMessagingChatTestCase
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty
import org.infobip.mobile.messaging.util.PreferenceHelper
import org.junit.Test
import org.mockito.Mockito.after
import org.mockito.Mockito.any
import org.mockito.Mockito.anyString
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.concurrent.Executor

class LivechatRegistrationCheckerTest : MobileMessagingChatTestCase() {

    lateinit var lcRegIdChecker: LivechatRegistrationChecker
    private val executor = Executor { command -> command.run() }

    override fun setUp() {
        super.setUp()
        lcRegIdChecker = LivechatRegistrationChecker(
            context,
            mobileMessagingCore,
            propertyHelper,
            inAppChatBroadcaster,
            mobileApiAppInstance,
            executor
        )
    }

    override fun tearDown() {
        super.tearDown()
        reset(mobileApiAppInstance)
    }

    @Test
    fun shouldSkipCheckWhenCallAreDisabled() {
        lcRegIdChecker.sync("widgetId", "pushRegistrationId", false)
        verify(mobileApiAppInstance, after(300).times(0)).getLivechatContactInformation(any())
        verify(inAppChatBroadcaster, after(300).times(0)).livechatRegistrationIdUpdated(any())
    }

    @Test
    fun shouldSkipCheckWhenAnotherCheckIsInProgress() {
        val lcContactInfoResponse = LivechatContactInformation(
                arrayOf(
                    LivechatDestination(
                        "widgetId",
                        "lcRegistrationId"
                    )
                )
        )
        `when`(mobileApiAppInstance.getLivechatContactInformation(any())).thenReturn(lcContactInfoResponse)
        doNothing().`when`(inAppChatBroadcaster).livechatRegistrationIdUpdated(anyString())

        Handler(Looper.getMainLooper()).postDelayed({
            lcRegIdChecker.sync("widgetId", "pushRegistrationId", true)
            lcRegIdChecker.sync("widgetId", "pushRegistrationId", true)
            lcRegIdChecker.sync("widgetId", "pushRegistrationId", true)

            verify(mobileApiAppInstance, after(300).times(1)).getLivechatContactInformation(any())
            verify(inAppChatBroadcaster, after(300).times(1)).livechatRegistrationIdUpdated(any())
        }, 500)

    }

    @Test
    fun shouldNotBroadcastLivechatRegistrationIdWhenNoWidgetPresent() {
        val widgetId = "widgetId"
        val pushRegistrationId = "pushRegistrationId"

        val lcContactInfoResponse = LivechatContactInformation(null)
        `when`(mobileApiAppInstance.getLivechatContactInformation(any())).thenReturn(lcContactInfoResponse)
        doNothing().`when`(inAppChatBroadcaster).livechatRegistrationIdUpdated(anyString())

        lcRegIdChecker.sync(widgetId, pushRegistrationId, true)

        verify(mobileApiAppInstance, after(300).times(1)).getLivechatContactInformation(pushRegistrationId)
        verify(inAppChatBroadcaster, after(300).times(0)).livechatRegistrationIdUpdated(anyString())
    }

    @Test
    fun shouldBroadcastLivechatRegistrationIdWhenSingleWidgetPresent() {
        val widgetId = "widgetId"
        val pushRegistrationId = "pushRegistrationId"
        val lcRegistrationId = "lcRegistrationId"

        val lcContactInfoResponse = LivechatContactInformation(
            arrayOf(
                LivechatDestination(
                    "widgetId",
                    lcRegistrationId
                )
            )
        )
        `when`(mobileApiAppInstance.getLivechatContactInformation(any())).thenReturn(lcContactInfoResponse)
        doNothing().`when`(inAppChatBroadcaster).livechatRegistrationIdUpdated(anyString())

        lcRegIdChecker.sync(widgetId, pushRegistrationId, true)

        verify(mobileApiAppInstance, after(300).times(1)).getLivechatContactInformation(pushRegistrationId)
        verify(inAppChatBroadcaster, after(300).times(1)).livechatRegistrationIdUpdated(lcRegistrationId)
    }

    @Test
    fun shouldBroadcastCorrectLivechatRegistrationIdWhenMoreWidgetsPresent() {
        val widgetId = "widgetId2"
        val pushRegistrationId = "pushRegistrationId"
        val lcRegistrationId = "lcRegistrationId2"

        val lcContactInfoResponse = LivechatContactInformation(
            arrayOf(
                LivechatDestination(
                    "widgetId1",
                    "lcRegistrationId1"
                ),
                LivechatDestination(
                    "widgetId2",
                    "lcRegistrationId2"
                ),
                LivechatDestination(
                    "widgetId3",
                    "lcRegistrationId3"
                )
            )
        )
        `when`(mobileApiAppInstance.getLivechatContactInformation(any())).thenReturn(lcContactInfoResponse)
        doNothing().`when`(inAppChatBroadcaster).livechatRegistrationIdUpdated(anyString())

        lcRegIdChecker.sync(widgetId, pushRegistrationId, true)

        verify(mobileApiAppInstance, after(300).times(1)).getLivechatContactInformation(pushRegistrationId)
        verify(inAppChatBroadcaster, after(300).times(1)).livechatRegistrationIdUpdated(lcRegistrationId)
    }

    @Test
    fun shouldNotBroadcastLivechatRegistrationIdWhenRequestFails() {
        doThrow(ApiIOException("error code", "error msg")).`when`(mobileApiAppInstance)
            .getLivechatContactInformation(anyString())

        lcRegIdChecker.sync("widgetId", "pushRegistrationId", true)

        verify(mobileApiAppInstance, after(300).times(1)).getLivechatContactInformation(any())
        verify(inAppChatBroadcaster, after(300).times(0)).livechatRegistrationIdUpdated(anyString())
    }

    @Test
    fun shouldUseArgumentsFromFallback() {
        val widgetId = "widgetId4"
        val pushRegistrationId = "pushRegistrationId"
        val lcRegistrationId = "lcRegistrationId4"

        val lcContactInfoResponse = LivechatContactInformation(
            arrayOf(
                LivechatDestination(
                    "widgetId1",
                    "lcRegistrationId1"
                ),
                LivechatDestination(
                    "widgetId2",
                    "lcRegistrationId2"
                ),
                LivechatDestination(
                    "widgetId3",
                    "lcRegistrationId3"
                ),
                LivechatDestination(
                    widgetId,
                    lcRegistrationId
                )
            )
        )
        `when`(mobileApiAppInstance.getLivechatContactInformation(any())).thenReturn(lcContactInfoResponse)
        doNothing().`when`(inAppChatBroadcaster).livechatRegistrationIdUpdated(anyString())

        PreferenceHelper.saveBoolean(
            context,
            MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_CALLS_ENABLED.key,
            true
        )
        PreferenceHelper.saveString(
            context,
            MobileMessagingProperty.INFOBIP_REGISTRATION_ID,
            "pushRegistrationId"
        )
        PreferenceHelper.saveString(
            context,
            MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ID.key,
            widgetId
        )

        lcRegIdChecker.sync()

        verify(mobileApiAppInstance, after(300).times(1)).getLivechatContactInformation(pushRegistrationId)
        verify(inAppChatBroadcaster, after(300).times(1)).livechatRegistrationIdUpdated(lcRegistrationId)
    }


}