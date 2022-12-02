package com.infobip.webrtc.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import com.infobip.webrtc.Injector
import com.infobip.webrtc.TAG
import com.infobip.webrtc.ui.model.ListenType
import org.infobip.mobile.messaging.util.ResourceLoader

interface InfobipRtcUi {
    class Builder(private val context: Context) {
        private var enableCalls = false

        fun applicationId(appId: String) = apply {
            Injector.cache.applicationId = appId
        }

        fun customActivity(clazz: Class<out Activity>) = apply {
            Injector.cache.activityClass = clazz
        }

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

    fun disableCalls(
        successListener: SuccessListener? = null,
        errorListener: ErrorListener? = null
    )

    fun enableInAppCalls(
        successListener: SuccessListener? = null,
        errorListener: ErrorListener? = null
    )

    fun enableCalls(
        identity: String,
        listenType: ListenType = ListenType.PUSH,
        successListener: SuccessListener? = null,
        errorListener: ErrorListener? = null
    )
}