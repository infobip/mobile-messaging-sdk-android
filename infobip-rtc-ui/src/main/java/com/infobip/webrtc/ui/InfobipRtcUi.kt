package com.infobip.webrtc.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import com.infobip.webrtc.Injector
import com.infobip.webrtc.TAG
import com.infobip.webrtc.ui.model.InCallButton
import com.infobip.webrtc.ui.model.ListenType
import com.infobip.webrtc.ui.model.RtcUiMode
import com.infobip.webrtc.ui.view.styles.InfobipRtcUiTheme
import org.infobip.mobile.messaging.util.ResourceLoader
import java.util.Locale

/**
 * [InfobipRtcUi] is an easy to use library, that allows you to connect to [Infobip RTC](https://github.com/infobip/infobip-rtc-android) by just building library.
 *
 * This assumes, though, that the initial setups of [Mobile Push Notifications](https://github.com/infobip/mobile-messaging-sdk-android/blob/master/README.md) and the [Infobip RTC](https://www.infobip.com/docs/voice-and-video/webrtc#getstartedwith-rtc-sdk) are set in both, your account and your mobile app profile.
 * [InfobipRtcUi] takes care of everything: the registration of your device (in order to receive and trigger calls), the handle of the calls themselves, and offers you a powerful user interface with all the features your customer may need: ability to capture video through both, front and back camera, option to mute and use the speaker, ability to capture and share the screen of your mobile device, option to minimise the call UI in a picture-on-picture mode, and more.
 * [InfobipRtcUi] also allows you to take control in any step of the flow, if you wish or need so, you can become a delegate for the FirebaseMessagingService or use your own custom user interface to handle the calls, it is up to you.
 */
interface InfobipRtcUi {

    class Builder(private val context: Context) {

        /**
         * [WebRTC application concept is deprecated.](https://www.infobip.com/docs/voice-and-video/webrtc#webrtc-sdk-1-x-deprecated-)
         * You must migrate to new [push configuration approach](https://www.infobip.com/docs/voice-and-video/webrtc#set-up-web-and-in-app-calls) to make calls working.
         * This function has no effect anymore.
         *
         * @return [InfobipRtcUi.Builder]
         */
        @Deprecated(
            "WebRTC application concept is deprecated. Use push configuration id instead",
            ReplaceWith("withConfigurationId()")
        )
        fun applicationId(appId: String) = this

        /**
         * Defines Infobip [WebRTC configuration ID](https://www.infobip.com/docs/api/channels/webrtc-calls/webrtc/save-push-configuration) to use.
         *
         * @param id configuration id
         * It is mandatory parameter. Can be provided also as string resource, builder provided ID has precedent over ID provided in resources.
         * ```
         * <resources>
         *    <string name="infobip_webrtc_configuration_id">WEBRTC CONFIGURATION ID</string>
         *    ...
         * </resources>
         * ```
         */
        fun withConfigurationId(id: String) = apply {
            Injector.cache.configurationId = id
        }

        /**
         * Defines custom activity class to handle call UI.
         *
         * @param clazz custom activity class handling call UI
         * @return [InfobipRtcUi.Builder]
         */
        fun withCustomActivity(clazz: Class<out Activity>) = apply {
            Injector.cache.activityClass = clazz
        }

        /**
         * Set whether incoming call should be declined in case of missing notification permission. Default value is true.
         *
         * @param decline true if call is automatically declined, false otherwise
         * @return [InfobipRtcUi.Builder]
         */
        fun withAutoDeclineOnMissingNotificationPermission(decline: Boolean) = apply {
            Injector.cache.autoDeclineOnMissingNotificationPermission = decline
        }

        /**
         * Enables incoming calls for InAppChat. Calls are not enabled immediately, it is waiting for InAppChat to provide livechatRegistrationId what is used as identity, listenType is PUSH.
         * If successListener is not provided, default null is used.
         * If errorListener is not provided, default null is used.
         *
         * @param successListener callback triggered when incoming calls are subscribed
         * @param errorListener callback triggered when incoming calls subscribe action failed
         * @return [InfobipRtcUi.Builder]
         */
        @JvmOverloads
        fun withInAppChatCalls(
            successListener: SuccessListener? = null,
            errorListener: ErrorListener? = null
        ): BuilderFinalStep = BuilderFinalStepImpl(
            context,
            RtcUiMode.IN_APP_CHAT.withListeners(successListener, errorListener)
        )

        /**
         * Enables incoming calls where internally managed pushRegistrationId is used as identity, listenType is PUSH.
         * If successListener is not provided, default null is used.
         * If errorListener is not provided, default null is used.
         *
         * @param successListener callback triggered when incoming calls are subscribed
         * @param errorListener callback triggered when incoming calls subscribe action failed
         */
        @JvmOverloads
        fun withCalls(
            successListener: SuccessListener? = null,
            errorListener: ErrorListener? = null
        ): BuilderFinalStep = BuilderFinalStepImpl(
            context,
            RtcUiMode.DEFAULT.withListeners(successListener, errorListener)
        )

        /**
         * Enables incoming calls for provided identity and listenType.
         * If listenType is not provided, default ListenType.PUSH is used.
         * If successListener is not provided, default null is used.
         * If errorListener is not provided, default null is used.
         *
         * @param identity WebRTC identity
         * @param listenType WebRTC listenType
         * @param successListener callback triggered when incoming calls are subscribed
         * @param errorListener callback triggered when incoming calls subscribe action failed
         */
        @JvmOverloads
        fun withCalls(
            identity: String,
            listenType: ListenType = ListenType.PUSH,
            successListener: SuccessListener? = null,
            errorListener: ErrorListener? = null
        ): BuilderFinalStep = BuilderFinalStepImpl(
            context,
            RtcUiMode.CUSTOM.withListeners(successListener, errorListener),
            identity,
            listenType
        )

        /**
         * Creates [InfobipRtcUi] SDK instance.
         *
         * @return [InfobipRtcUi] instance
         */
        fun build(): InfobipRtcUi {
            return BuilderFinalStepImpl(context).build()
        }

    }

    interface BuilderFinalStep {

        /**
         * Creates [InfobipRtcUi] SDK instance.
         *
         * @return [InfobipRtcUi] instance
         */
        fun build(): InfobipRtcUi

    }

    private class BuilderFinalStepImpl(
        private val context: Context,
        private val rtcUiMode: RtcUiMode? = null,
        private val identity: String? = null,
        private val listenType: ListenType? = null
    ) : BuilderFinalStep {
        override fun build(): InfobipRtcUi {
            return getInstance(context).also { sdk ->
                this.rtcUiMode?.let { mode ->
                    val defaultSuccessListener =
                        SuccessListener { Log.d(TAG, "$mode calls enabled.") }
                    val defaultErrorListener =
                        ErrorListener { Log.d(TAG, "Failed to enabled $mode calls.", it) }
                    when (mode) {
                        RtcUiMode.CUSTOM -> {
                            this.identity?.let { identity ->
                                this.listenType?.let { listenType ->
                                    sdk.enableCalls(
                                        identity = identity,
                                        listenType = listenType,
                                        successListener = mode.successListener
                                            ?: defaultSuccessListener,
                                        errorListener = mode.errorListener ?: defaultErrorListener
                                    )
                                }
                            }
                        }

                        RtcUiMode.DEFAULT -> sdk.enableCalls(
                            successListener = mode.successListener ?: defaultSuccessListener,
                            errorListener = mode.errorListener ?: defaultErrorListener
                        )

                        RtcUiMode.IN_APP_CHAT -> sdk.enableInAppChatCalls(
                            successListener = mode.successListener ?: defaultSuccessListener,
                            errorListener = mode.errorListener ?: defaultErrorListener
                        )
                    }
                }
            }
        }
    }

    companion object {

        /**
         * Provides [InfobipRtcUi] SDK instance.
         *
         * @return [InfobipRtcUi] instance
         */
        @JvmStatic
        fun getInstance(context: Context): InfobipRtcUi {
            val sdk = Injector.getWebrtcUi(context)

            if (Injector.cache.configurationId.isBlank()) {
                getWebRtcPushConfigurationIdFromResources(context)?.let {
                    Injector.cache.configurationId = it
                }
            }
            validateMobileMessagingApplicationCode()
            validateWebRtcPushConfigurationId()
            return sdk
        }

        private fun getWebRtcPushConfigurationIdFromResources(context: Context): String? {
            val resource = ResourceLoader.loadResourceByName(
                context,
                "string",
                "infobip_webrtc_configuration_id"
            )
            return if (resource > 0) {
                context.resources.getString(resource)
            } else null
        }

        private fun validateMobileMessagingApplicationCode() {
            if (Injector.appCodeDelegate.getApplicationCode().isNullOrBlank())
                throw IllegalArgumentException("Application code is not provided to MobileMessaging library, make sure it is available in resources or Mobile Messaging SDK builder.")
        }

        private fun validateWebRtcPushConfigurationId() {
            if (Injector.cache.configurationId.isBlank())
                throw IllegalArgumentException("Webrtc push configuration ID is not provided to InfobipRtcUi library, make sure it is available in resources or InfobipRtcUi SDK builder.")
        }

    }

    /**
     * Disables incoming calls.
     *
     * @param successListener callback triggered when incoming calls are unsubscribed
     * @param errorListener callback triggered when incoming calls unsubscribe action failed
     */
    fun disableCalls(
        successListener: SuccessListener? = null,
        errorListener: ErrorListener? = null
    )

    /**
     * Enables incoming calls for InAppChat. Calls are not enabled immediately, it is waiting for InAppChat to provide livechatRegistrationId what is used as identity, listenType is PUSH.
     * If successListener is not provided, default null is used.
     * If errorListener is not provided, default null is used.
     *
     * @param successListener callback triggered when incoming calls are subscribed
     * @param errorListener callback triggered when incoming calls subscribe action failed
     * @return [InfobipRtcUi.Builder]
     */
    fun enableInAppChatCalls(
        successListener: SuccessListener? = null,
        errorListener: ErrorListener? = null
    )

    /**
     * Enables incoming calls where internally managed pushRegistrationId is used as identity, listenType is PUSH.
     * If successListener is not provided, default null is used.
     * If errorListener is not provided, default null is used.
     *
     * @param successListener callback triggered when incoming calls are subscribed
     * @param errorListener callback triggered when incoming calls subscribe action failed
     */
    fun enableCalls(
        successListener: SuccessListener? = null,
        errorListener: ErrorListener? = null
    )

    /**
     * Enables incoming calls for provided identity and listenType.
     * If listenType is not provided, default ListenType.PUSH is used.
     * If successListener is not provided, default null is used.
     * If errorListener is not provided, default null is used.
     *
     * @param identity WebRTC identity
     * @param listenType WebRTC listenType
     * @param successListener callback triggered when incoming calls are subscribed
     * @param errorListener callback triggered when incoming calls subscribe action failed
     */
    fun enableCalls(
        identity: String,
        listenType: ListenType = ListenType.PUSH,
        successListener: SuccessListener? = null,
        errorListener: ErrorListener? = null
    )

    /**
     * Sets the UI language. Call's UI must be recreated to apply new language.
     *
     * @param locale new locale to be set
     */
    fun setLanguage(locale: Locale)

    /**
     * Sets bottom sheet buttons in call screen. First three buttons are in visible area (+ hang-up button is fixed on first place.)
     * All another buttons are place in draggable area.
     *
     * @param buttons sorted set of in call buttons
     */
    fun setInCallButtons(buttons: List<InCallButton>)

    /**
     * Set theme, it is alternative to defining style in xml.
     *
     * Final value for every theme attribute is resolved from multiple source-by-source priority.
     * The source with the highest priority defines a final attribute value.
     * If source does not define an attribute value, there is fallback to the source with lower priority.
     *
     * Sources by priority:
     * 1. [InfobipRtcUiTheme] theme provided in runtime using this function.
     * 2. [InfobipRtcUi] style provided in xml.
     * 3. Default [InfobipRtcUi] style defined by InfobipRtcUi library
     *
     * Final value for every theme attribute is evaluated separately.
     * It means you can define [InfobipRtcUiTheme.incomingCallMessageStyle] in runtime, colors in xml and skip icons.
     * Library will use [InfobipRtcUiTheme.incomingCallMessageStyle] you defined in runtime, colors you defined in xml and default icons provided by library itself.
     *
     * @param theme data object holding all theme attributes
     */
    fun setTheme(theme: InfobipRtcUiTheme)
}