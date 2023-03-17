package org.infobip.mobile.messaging.chat.view

import android.content.*
import android.net.ConnectivityManager
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import org.infobip.mobile.messaging.*
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.api.support.http.client.DefaultApiClient
import org.infobip.mobile.messaging.chat.InAppChat
import org.infobip.mobile.messaging.chat.InAppChatErrors
import org.infobip.mobile.messaging.chat.InAppChatImpl
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.attachments.InAppChatMobileAttachment
import org.infobip.mobile.messaging.chat.core.*
import org.infobip.mobile.messaging.chat.databinding.IbViewChatBinding
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty
import org.infobip.mobile.messaging.chat.properties.PropertyHelper
import org.infobip.mobile.messaging.chat.utils.*
import org.infobip.mobile.messaging.chat.view.styles.InAppChatStyle
import org.infobip.mobile.messaging.chat.view.styles.InAppChatStyle.Companion.applyWidgetConfig
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import org.infobip.mobile.messaging.mobileapi.InternalSdkError
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError
import org.infobip.mobile.messaging.util.StringUtils
import java.util.*

class InAppChatView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attributes, defStyle, defStyleRes) {

    /**
     * [InAppChatView] events listener propagates events coming from Livechat Widget
     */
    interface EventsListener {
        fun onChatLoaded(controlsEnabled: Boolean)
        fun onChatControlsVisibilityChanged(isVisible: Boolean)
        fun onAttachmentPreviewOpened(url: String?, type: String?, caption: String?)
        fun onChatViewChanged(widgetView: InAppChatWidgetView)
        fun onChatWidgetInfoUpdated(widgetInfo: WidgetInfo)
    }

    /**
     * [InAppChatView] errors handler allows you to define custom way to process [InAppChatView] errors.
     */
    interface ErrorsHandler {
        fun handlerError(error: String)
        fun handlerWidgetError(error: String)
        fun handlerNoInternetConnectionError()
    }

    companion object {
        private const val CHAT_NOT_AVAILABLE_ANIM_DURATION_MILLIS = 500L
        private const val CHAT_SERVICE_ERROR = "12"
        private const val CHAT_WIDGET_NOT_FOUND = "24"
    }

    private val binding = IbViewChatBinding.inflate(LayoutInflater.from(context), this)
    private var style = InAppChatStyle(context, attributes)
    private val inAppChat = InAppChat.getInstance(context)
    private val inAppChatClient: InAppChatClient = InAppChatClientImpl(binding.ibLcWebView)
    private val mmCore: MobileMessagingCore = MobileMessagingCore.getInstance(context)
    private val localizationUtils = LocalizationUtils.getInstance(context)
    private var isChatLoaded: Boolean = false
    private var widgetInfo: WidgetInfo? = null
    private var lastControlsVisibility: Boolean? = null

    /**
     * [InAppChatView] event listener allows you to listen to Livechat widget events.
     */
    var eventsListener: EventsListener? = null

    /**
     * Returns true if chat is synchronized and multithread feature is enabled, otherwise returns false
     */
    val isMultiThread: Boolean
        get() = widgetInfo?.isMultiThread() ?: false

    val defaultErrorsHandler: ErrorsHandler = object : ErrorsHandler {

        override fun handlerError(error: String) {
            MobileMessagingLogger.e("InAppChatView", "Unhandled error $error")
        }

        override fun handlerWidgetError(error: String) {
            Snackbar.make(
                binding.root,
                localizationUtils.getString(R.string.ib_chat_error, error),
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.ib_chat_ok) {}
                .show()
        }

        override fun handlerNoInternetConnectionError() {
            showNoInternetConnectionView()
        }
    }

    /**
     * Allows you to set custom [InAppChatView.ErrorsHandler] handler to process [InAppChatView] errors on your own.
     */
    var errorsHandler: ErrorsHandler = defaultErrorsHandler

    init {
        applyStyle(style)
    }

    //region Public
    /**
     * Initialize [InAppChatView] with enclosing android component [Lifecycle].
     * @param lifecycle lifecycle of android Activity or Fragment
     */
    fun init(lifecycle: Lifecycle) {
        inAppChat.activate()
        binding.ibLcWebView.setup(inAppChatWebViewManager)
        lifecycle.addObserver(lifecycleObserver)
        updateWidgetInfo()
    }

    /**
     * Set the language of the Livechat Widget
     * @param locale locale's language is used by Livechat Widget and native parts
     */
    fun setLanguage(locale: Locale) {
        MobileMessagingLogger.d("InAppChatView", "setLanguage($locale)")
        PropertyHelper(context).saveString(
            MobileMessagingChatProperty.IN_APP_CHAT_LANGUAGE,
            locale.toString()
        )
        inAppChatClient.setLanguage(locale.language) //LC widget uses only language
        localizationUtils.setLanguage(locale) //native parts use language and country code
        updateWidgetInfo()
    }

    /**
     * Set contextual data of the Livechat Widget
     *
     * @param data                   contextual data in the form of JSON string
     * @param allMultiThreadStrategy multithread strategy flag, true -> ALL, false -> ACTIVE
     */
    fun sendContextualMetaData(data: String, allMultiThreadStrategy: Boolean) {
        val flag =
            if (allMultiThreadStrategy) InAppChatMultiThreadFlag.ALL else InAppChatMultiThreadFlag.ACTIVE
        inAppChatClient.sendContextualData(data, flag)
    }

    /**
     * Navigates Livechat widget from thread detail back to thread's list destination in multithread widget. It does nothing if widget is not multithread.
     */
    fun showThreadList() = inAppChatClient.showThreadList()

    /**
     * Sends draft message to be show in chat to peer's chat.
     * @param draft message
     */
    fun sendInputDraft(draft: String) = inAppChatClient.sendInputDraft(draft)

    /**
     * Sends message to the chat with optional [InAppChatMobileAttachment].
     * @param message message to be send
     * @param attachment to create attachment use [InAppChatMobileAttachment]'s constructor where you provide attachment's mimeType, base64 and filename
     */
    @JvmOverloads
    fun sendChatMessage(message: String?, attachment: InAppChatMobileAttachment? = null) {
        val msg = CommonUtils.escapeJsonString(message)
        if (attachment != null) {
            inAppChatClient.sendChatMessage(msg, attachment)
        } else {
            inAppChatClient.sendChatMessage(msg)
        }
    }
    //endregion

    //region Lifecycle
    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            super.onResume(owner)
            registerReceivers()
            updateErrors()
            binding.ibLcWebView.onResume()
            loadWebPage(force = !isChatLoaded)
            syncInAppChatConfigIfNeeded()
        }

        override fun onPause(owner: LifecycleOwner) {
            super.onPause(owner)
            unregisterReceivers()
            binding.ibLcWebView.onPause()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            binding.ibLcWebView.destroy()
        }
    }
    //endregion

    //region InAppChatWebViewManager
    private val inAppChatWebViewManager = object : InAppChatWebViewManager {
        override fun onPageStarted() {
            binding.ibLcSpinner.visible()
            binding.ibLcWebView.invisible()
        }

        override fun onPageFinished() {
            binding.ibLcSpinner.invisible()
            binding.ibLcWebView.visible()
            applyLanguage()
        }

        override fun setControlsEnabled(enabled: Boolean) {
            eventsListener?.onChatLoaded(enabled)
            isChatLoaded = enabled
            if (enabled)
                inAppChat.resetMessageCounter()
        }

        override fun onJSError(message: String?) {
            inAppChatErrors.insertError(InAppChatErrors.Error(InAppChatErrors.JS_ERROR, message))
            binding.ibLcSpinner.invisible()
            binding.ibLcWebView.visible()
            setControlsEnabled(false)
        }

        override fun setControlsVisibility(isVisible: Boolean) {
            if (lastControlsVisibility != isVisible) {
                eventsListener?.onChatControlsVisibilityChanged(isVisible)
            }
            lastControlsVisibility = isVisible
        }

        override fun openAttachmentPreview(url: String?, type: String?, caption: String?) {
            eventsListener?.onAttachmentPreviewOpened(url, type, caption)
        }

        override fun onWidgetViewChanged(widgetView: InAppChatWidgetView) {
            eventsListener?.onChatViewChanged(widgetView)
        }
    }
    //endregion

    //region Error handling
    //region BroadcastReceiver
    private var receiversRegistered = false
    private val broadcastEventsReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            if (action == ConnectivityManager.CONNECTIVITY_ACTION) {
                if (intent.hasExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY)) {
                    inAppChatErrors.insertError(
                        InAppChatErrors.Error(
                            InAppChatErrors.INTERNET_CONNECTION_ERROR,
                            localizationUtils.getString(R.string.ib_chat_no_connection)
                        )
                    )
                } else {
                    inAppChatErrors.removeError(InAppChatErrors.INTERNET_CONNECTION_ERROR)
                }
            } else if (action == InAppChatEvent.CHAT_CONFIGURATION_SYNCED.key) {
                if (!inAppChatErrors.removeError(InAppChatErrors.CONFIG_SYNC_ERROR)) {
                    onWidgetSynced()
                }
            } else if (action == Event.API_COMMUNICATION_ERROR.key && intent.hasExtra(
                    BroadcastParameter.EXTRA_EXCEPTION
                )
            ) {
                val mobileMessagingError =
                    intent.getSerializableExtra(BroadcastParameter.EXTRA_EXCEPTION) as MobileMessagingError?
                val errorCode = mobileMessagingError!!.code
                if (errorCode == CHAT_SERVICE_ERROR || errorCode == CHAT_WIDGET_NOT_FOUND) {
                    inAppChatErrors.insertError(
                        InAppChatErrors.Error(
                            InAppChatErrors.CONFIG_SYNC_ERROR,
                            mobileMessagingError.message
                        )
                    )
                }
            } else if (action == Event.REGISTRATION_CREATED.key) {
                syncInAppChatConfigIfNeeded()
            }
        }
    }

    private fun registerReceivers() {
        if (!receiversRegistered) {
            val intentFilter = IntentFilter()
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
            intentFilter.addAction(InAppChatEvent.CHAT_CONFIGURATION_SYNCED.key)
            intentFilter.addAction(Event.API_COMMUNICATION_ERROR.key)
            intentFilter.addAction(Event.REGISTRATION_CREATED.key)
            context.registerReceiver(broadcastEventsReceiver, intentFilter)
            receiversRegistered = true
        }
    }

    private fun unregisterReceivers() {
        if (receiversRegistered) {
            context.unregisterReceiver(broadcastEventsReceiver)
            receiversRegistered = false
        }
    }
    //endregion

    private fun updateErrors() {
        val chatWidgetConfigSyncResult = InAppChatImpl.getChatWidgetConfigSyncResult()
        if (chatWidgetConfigSyncResult != null && !chatWidgetConfigSyncResult.isSuccess) {
            val error = chatWidgetConfigSyncResult.error
            val isInternetConnectionError =
                DefaultApiClient.ErrorCode.API_IO_ERROR.value == error.code && error.type == MobileMessagingError.Type.SERVER_ERROR
            val isRegistrationPendingError =
                InternalSdkError.NO_VALID_REGISTRATION.error.code == error.code && mmCore.isRegistrationIdReported
            //connection error handled separately by broadcast receiver, sync is triggered again after registration, do not show error
            if (!isInternetConnectionError && !isRegistrationPendingError) {
                inAppChatErrors.insertError(
                    InAppChatErrors.Error(
                        InAppChatErrors.CONFIG_SYNC_ERROR,
                        error.message
                    )
                )
            }
        }
    }

    private val inAppChatErrors = InAppChatErrors { currentErrors, removedError, _ ->
        if (removedError != null) {
            //reload webView if it wasn't loaded in case when internet connection appeared
            if (InAppChatErrors.INTERNET_CONNECTION_ERROR == removedError.type && !isChatLoaded) {
                hideNoInternetConnectionView()
                loadWebPage(force = true)
            }

            //update views configuration and reload webPage in case there was config sync error
            if (InAppChatErrors.CONFIG_SYNC_ERROR == removedError.type) {
                onWidgetSynced()
            }
        }

        if (currentErrors.isEmpty()) {
            hideNoInternetConnectionView()
        } else {
            for (error in currentErrors) {
                if (InAppChatErrors.INTERNET_CONNECTION_ERROR == error.type) {
                    errorsHandler.handlerNoInternetConnectionError()
                } else if (InAppChatErrors.CONFIG_SYNC_ERROR == error.type || InAppChatErrors.JS_ERROR == error.type) {
                    errorsHandler.handlerWidgetError(error.message)
                } else {
                    errorsHandler.handlerError(error.message)
                }
            }
        }
    }

    private fun showNoInternetConnectionView(duration: Long = CHAT_NOT_AVAILABLE_ANIM_DURATION_MILLIS) =
        with(binding.ibLcConnectionError) {
            if (isInvisible) {
                this@with.visible()
                animate().translationY(height.toFloat()).duration = duration
            }
        }

    private fun hideNoInternetConnectionView(duration: Long = CHAT_NOT_AVAILABLE_ANIM_DURATION_MILLIS) =
        with(binding.ibLcConnectionError) {
            if (isVisible) {
                animate().translationY(0f).duration = duration
                postDelayed({ this@with.invisible() }, duration)
            }
        }
    //endregion

    //region Helpers
    private fun loadWebPage(force: Boolean = false) {
        binding.ibLcWebView.loadWebPage(force, widgetInfo, inAppChat.jwtProvider?.provideJwt())
    }

    private fun applyStyle(style: InAppChatStyle) = with(binding) {
        root.setBackgroundColor(style.backgroundColor)
        ibLcWebView.setBackgroundColor(style.backgroundColor)
        ibLcSpinner.setProgressTint(style.progressBarColor.toColorStateList())
        ibLcConnectionError.setBackgroundColor(style.networkConnectionLabelBackgroundColor)
        style.networkConnectionTextAppearance?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ibLcConnectionError.setTextAppearance(it)
            } else {
                ibLcConnectionError.setTextAppearance(context, it)
            }
        }
        ibLcConnectionError.setTextColor(style.networkConnectionTextColor)
        if (style.networkConnectionTextRes != null) {
            ibLcConnectionError.text = localizationUtils.getString(style.networkConnectionTextRes)
        } else if (style.networkConnectionText != null) {
            ibLcConnectionError.text = style.networkConnectionText
        }
    }

    private fun updateWidgetInfo() {
        widgetInfo = prepareWidgetInfo()
        widgetInfo?.let {
            style = style.applyWidgetConfig(context, it)
            applyStyle(style)
            eventsListener?.onChatWidgetInfoUpdated(it)
        }
    }

    private fun onWidgetSynced() {
        updateWidgetInfo()
        loadWebPage(force = true)
    }

    private fun syncInAppChatConfigIfNeeded() {
        val pushRegistrationId = MobileMessagingCore.getInstance(context).pushRegistrationId
        if (pushRegistrationId != null && widgetInfo == null) {
            (inAppChat as? MessageHandlerModule)?.performSyncActions()
        }
    }

    private fun prepareWidgetInfo(): WidgetInfo? {
        val prefs = PropertyHelper.getDefaultMMSharedPreferences(context)
        val widgetId = prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ID.key, null)
        val widgetTitle =
            prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_TITLE.key, null)
        val widgetPrimaryColor =
            prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_PRIMARY_COLOR.key, null)
        val widgetBackgroundColor = prefs.getString(
            MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_BACKGROUND_COLOR.key,
            null
        )
        val maxUploadContentSizeStr = prefs.getString(
            MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_MAX_UPLOAD_CONTENT_SIZE.key,
            null
        )
        val widgetMultiThread =
            prefs.getBoolean(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_MULTITHREAD.key, false)
        val language = prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_LANGUAGE.key, null)
        var maxUploadContentSize = InAppChatMobileAttachment.DEFAULT_MAX_UPLOAD_CONTENT_SIZE
        if (StringUtils.isNotBlank(maxUploadContentSizeStr)) {
            maxUploadContentSize = maxUploadContentSizeStr!!.toLong()
        }
        return widgetId?.let {
            WidgetInfo(
                it,
                widgetTitle,
                widgetPrimaryColor,
                widgetBackgroundColor,
                maxUploadContentSize,
                language,
                widgetMultiThread
            )
        }
    }

    private fun applyLanguage() {
        val storedLanguage: String? = widgetInfo?.getLanguage()
        val language: String = if (storedLanguage?.isNotBlank() == true) {
            storedLanguage
        } else {
            mmCore.installation.language
        }
        val locale = localizationUtils.localeFromString(language)
        setLanguage(locale)
    }
    //endregion
}