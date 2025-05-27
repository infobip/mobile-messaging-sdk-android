package org.infobip.mobile.messaging.chat.mobileapi

import android.os.Handler
import android.os.Looper
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.verify
import org.infobip.mobile.messaging.MobileMessagingProperty
import org.infobip.mobile.messaging.api.appinstance.LivechatContactInformation
import org.infobip.mobile.messaging.api.appinstance.LivechatDestination
import org.infobip.mobile.messaging.api.support.ApiIOException
import org.infobip.mobile.messaging.chat.MobileMessagingChatTestCase
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty
import org.infobip.mobile.messaging.util.PreferenceHelper
import org.junit.Test
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
        clearMocks(mobileApiAppInstance)
    }

    @Test
    fun shouldSkipCheckWhenCallAreDisabled() {
        lcRegIdChecker.sync("widgetId", "pushRegistrationId", false)
        verify(exactly = 0) { mobileApiAppInstance.getLivechatContactInformation(any()) }
        verify(exactly = 0) { inAppChatBroadcaster.livechatRegistrationIdUpdated(any()) }
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
        every { mobileApiAppInstance.getLivechatContactInformation(any()) } returns lcContactInfoResponse
        every { inAppChatBroadcaster.livechatRegistrationIdUpdated(any()) } just Runs

        Handler(Looper.getMainLooper()).postDelayed({
            lcRegIdChecker.sync("widgetId", "pushRegistrationId", true)
            lcRegIdChecker.sync("widgetId", "pushRegistrationId", true)
            lcRegIdChecker.sync("widgetId", "pushRegistrationId", true)

            verify { mobileApiAppInstance.getLivechatContactInformation(any()) }
            verify { inAppChatBroadcaster.livechatRegistrationIdUpdated(any()) }
        }, 500)

    }

    @Test
    fun shouldNotBroadcastLivechatRegistrationIdWhenNoWidgetPresent() {
        val widgetId = "widgetId"
        val pushRegistrationId = "pushRegistrationId"

        val lcContactInfoResponse = LivechatContactInformation(null)
        every { mobileApiAppInstance.getLivechatContactInformation(any()) } returns lcContactInfoResponse
        every { inAppChatBroadcaster.livechatRegistrationIdUpdated(any()) } just Runs

        lcRegIdChecker.sync(widgetId, pushRegistrationId, true)

        verify { mobileApiAppInstance.getLivechatContactInformation(pushRegistrationId) }
        verify(exactly = 0) { inAppChatBroadcaster.livechatRegistrationIdUpdated(any()) }
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
        every { mobileApiAppInstance.getLivechatContactInformation(any()) } returns lcContactInfoResponse
        every { inAppChatBroadcaster.livechatRegistrationIdUpdated(any()) } just Runs

        lcRegIdChecker.sync(widgetId, pushRegistrationId, true)

        verify { mobileApiAppInstance.getLivechatContactInformation(pushRegistrationId) }
        verify { inAppChatBroadcaster.livechatRegistrationIdUpdated(lcRegistrationId) }
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
        every { mobileApiAppInstance.getLivechatContactInformation(any()) } returns lcContactInfoResponse
        every { inAppChatBroadcaster.livechatRegistrationIdUpdated(any()) } just Runs

        lcRegIdChecker.sync(widgetId, pushRegistrationId, true)

        verify { mobileApiAppInstance.getLivechatContactInformation(pushRegistrationId) }
        verify { inAppChatBroadcaster.livechatRegistrationIdUpdated(lcRegistrationId) }
    }

    @Test
    fun shouldNotBroadcastLivechatRegistrationIdWhenRequestFails() {
        every { mobileApiAppInstance.getLivechatContactInformation(any()) } throws ApiIOException("error code", "error msg")

        lcRegIdChecker.sync("widgetId", "pushRegistrationId", true)

        verify { mobileApiAppInstance.getLivechatContactInformation(any()) }
        verify(exactly = 0) { inAppChatBroadcaster.livechatRegistrationIdUpdated(any()) }
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

        every { mobileApiAppInstance.getLivechatContactInformation(any()) } returns lcContactInfoResponse
        every { inAppChatBroadcaster.livechatRegistrationIdUpdated(any()) } just Runs

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

        verify { mobileApiAppInstance.getLivechatContactInformation(any()) }
        verify { inAppChatBroadcaster.livechatRegistrationIdUpdated(lcRegistrationId) }
    }


}