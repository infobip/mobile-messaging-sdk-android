package com.infobip.webrtc.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import com.infobip.webrtc.Injector
import com.infobip.webrtc.TAG
import com.infobip.webrtc.ui.model.ListenType
import org.infobip.mobile.messaging.util.ResourceLoader

/**
 * [InfobipRtcUi] is an easy to use library, that allows you to connect to [Infobip RTC](https://github.com/infobip/infobip-rtc-android) by just building library.
 *
 * This assumes, though, that the initial setups of [Mobile Push Notifications](https://github.com/infobip/mobile-messaging-sdk-android/blob/master/README.md) and the [Infobip RTC](https://www.infobip.com/docs/voice-and-video/webrtc#set-up-web-and-in-app-calls) are set in both, your account and your mobile app profile.
 * [InfobipRtcUi] takes care of everything: the registration of your device (in order to receive and trigger calls), the handle of the calls themselves, and offers you a powerful user interface with all the features your customer may need: ability to capture video through both, front and back camera, option to mute and use the speaker, ability to capture and share the screen of your mobile device, option to minimise the call UI in a picture-on-picture mode, and more.
 * [InfobipRtcUi] also allows you to take control in any step of the flow, if you wish or need so, you can become a delegate for the FirebaseMessagingService or use your own custom user interface to handle the calls, it is up to you.
 */
interface InfobipRtcUi {
    class Builder(private val context: Context) {
        private var enableCalls = false

        /**
         * Defines Infobip <a href="https://portal.infobip.com/apps/webrtc/application/create" target="_blank">`WebRTC application ID`</a> to use.
         *
         * @param appId <a href="https://portal.infobip.com/apps/webrtc/application/create" target="_blank">`WebRTC application ID`</a>
         * It is mandatory parameter. Can be provided also as string resource, builder provided ID has precedent over ID provided in resources.
         * ```
         * <resources>
         *    <string name="infobip_webrtc_application_id">WEBRTC APPLICATION ID</string>
         *    ...
         * </resources>
         * ```
         * @return [InfobipRtcUi.Builder]
         */
        fun applicationId(appId: String) = apply {
            Injector.cache.applicationId = appId
        }

        /**
         * Defines custom activity class to handle call UI.
         *
         * @param clazz custom activity class handling call UI
         * @return [InfobipRtcUi.Builder]
         */
        fun customActivity(clazz: Class<out Activity>) = apply {
            Injector.cache.activityClass = clazz
        }

        /**
         * Enables calls immediately SDK is created.
         *
         * @param successListener callback triggered when incoming calls are subscribed
         * @param errorListener callback triggered when incoming calls subscribe action failed
         * @return [InfobipRtcUi.Builder]
         */
        @JvmOverloads
        fun enableInAppCalls(
                successListener: SuccessListener? = null,
                errorListener: ErrorListener? = null
        ) = apply {
            this.enableCalls = true
            Injector.cache.inAppCallsEnabled = true
            Injector.enableInAppCallsSuccess = successListener
            Injector.enableInAppCallsError = errorListener
        }

        /**
         * Set whether incoming call should declined in case of missing notification permission. Default value is true.
         *
         * @param decline true if call is automatically declined, false otherwise
         * @return [InfobipRtcUi.Builder]
         */
        fun autoDeclineOnMissingNotificationPermission(decline: Boolean) = apply {
            Injector.cache.autoDeclineOnMissingNotificationPermission = decline
        }

        /**
         * Creates [InfobipRtcUi] SDK instance.
         *
         * @return [InfobipRtcUi] instance
         */
        fun build(): InfobipRtcUi {
            return getInstance(context).also { sdk ->
                if (this.enableCalls) {
                    sdk.enableInAppCalls(
                        successListener = Injector.enableInAppCallsSuccess ?: SuccessListener { Log.d(TAG, "InAppCalls enabled.") },
                        errorListener = Injector.enableInAppCallsError ?: ErrorListener { Log.d(TAG, "Failed to enabled InAppCalls.", it) }
                    )
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

            if (Injector.cache.applicationId.isBlank()) {
                getWebRtcApplicationIdFromResources(context)?.let {
                    Injector.cache.applicationId = it
                }
            }
            validateMobileMessagingApplicationCode()
            validateWebRtcApplicationId()
            return sdk
        }

        private fun getWebRtcApplicationIdFromResources(context: Context): String? {
            val resource = ResourceLoader.loadResourceByName(context, "string", "infobip_webrtc_application_id")
            return if (resource > 0) {
                context.resources.getString(resource)
            } else null
        }

        private fun validateMobileMessagingApplicationCode() {
            if (Injector.appCodeDelegate.getApplicationCode().isNullOrBlank())
                throw IllegalArgumentException("Application code is not provided to MobileMessaging library, make sure it is available in resources or Mobile Messaging SDK builder.")
        }

        private fun validateWebRtcApplicationId() {
            if (Injector.cache.applicationId.isBlank())
                throw IllegalArgumentException("Application ID is not provided to InfobipRtcUi library, make sure it is available in resources or InfobipRtcUi SDK builder.")
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
     * Enables incoming calls subscription for InAppChat.
     *
     * @param successListener callback triggered when incoming calls are subscribed
     * @param errorListener callback triggered when incoming calls subscribe action failed
     */
    fun enableInAppCalls(
        successListener: SuccessListener? = null,
        errorListener: ErrorListener? = null
    )

    /**
     * Enables incoming calls for custom identity and listenType.
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
}