package org.infobip.mobile.messaging.chat.view

import android.content.*
import android.net.ConnectivityManager
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
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
import org.infobip.mobile.messaging.chat.mobileapi.LivechatRegistrationChecker
import org.infobip.mobile.messaging.chat.properties.MobileMessagingChatProperty
import org.infobip.mobile.messaging.chat.properties.PropertyHelper
import org.infobip.mobile.messaging.chat.utils.*
import org.infobip.mobile.messaging.chat.view.styles.InAppChatStyle
import org.infobip.mobile.messaging.chat.view.styles.factory.StyleFactory
import org.infobip.mobile.messaging.logging.MobileMessagingLogger
import org.infobip.mobile.messaging.mobileapi.InternalSdkError
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError
import org.infobip.mobile.messaging.util.StringUtils
import org.infobip.mobile.messaging.util.SystemInformation
import java.util.*

class InAppChatView @JvmOverloads constructor(
    context: Context,
    private val attributes: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attributes, defStyle, defStyleRes) {

    /**
     * [InAppChatView] events listener propagates events coming from Livechat Widget
     */
    interface EventsListener {
        fun onChatLoaded(controlsEnabled: Boolean)
        fun onChatDisconnected()
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
        private const val TAG = "InAppChatView"
        const val MESSAGE_MAX_LENGTH = 1000
    }

    private val binding = IbViewChatBinding.inflate(LayoutInflater.from(context), this)
    private var style = StyleFactory.create(context, attributes).chatStyle()
    private val inAppChat = InAppChat.getInstance(context)
    private val inAppChatClient: InAppChatClient = InAppChatClientImpl(binding.ibLcWebView)
    private var inAppChatBroadcaster: InAppChatBroadcaster = InAppChatBroadcasterImpl(context)
    private val mmCore: MobileMessagingCore = MobileMessagingCore.getInstance(context)
    private val localizationUtils = LocalizationUtils.getInstance(context)
    private val lcRegIdChecker = LivechatRegistrationChecker(context)
    private var widgetInfo: WidgetInfo? = null
    private var lastControlsVisibility: Boolean? = null

    /**
     * Returns true if chat is loaded, otherwise returns false.
     */
    var isChatLoaded: Boolean = false
        private set

    /**
     * [InAppChatView] event listener allows you to listen to Livechat widget events.
     */
    var eventsListener: EventsListener? = null

    /**
     * Returns true if chat is synchronized and multithread feature is enabled, otherwise returns false.
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
     *
     * Chat connection is established and stopped based on provided [Lifecycle].
     * Chat connection is active only when [Lifecycle.State] is at least [Lifecycle.State.STARTED].
     *
     * @param lifecycle lifecycle of android Activity or Fragment
     */
    fun init(lifecycle: Lifecycle) {
        updateWidgetInfo()
        inAppChat.activate()
        binding.ibLcWebView.setup(inAppChatWebViewManager)
        lifecycle.addObserver(lifecycleObserver)
    }

    /**
     * Load chat. Use it to re-establish chat connection when you previously called [stopConnection].
     *
     * It is not needed to use it in most cases as chat connection is established and stopped based on [Lifecycle] provided in [init].
     * Chat connection is active only when [Lifecycle.State] is at least [Lifecycle.State.STARTED].
     *
     * By chat connection you can control push notifications.
     * Push notifications are suppressed while the chat is loaded.
     *
     * To detect if chat is loaded use [isChatLoaded] or [EventsListener.onChatLoaded] event from [EventsListener].
     */
    fun restartConnection() {
        if (widgetInfo == null)
            updateWidgetInfo()
        loadChatPage(force = !isChatLoaded)
        MobileMessagingLogger.d(TAG, "Chat connection established.")
    }

    /**
     * Load blank page, chat connection is stopped.
     *
     * It is not needed to use it in most cases as chat connection is established and stopped based on [Lifecycle] provided in [init].
     * Chat connection is stopped when [Lifecycle.State] is below [Lifecycle.State.STARTED].
     *
     * By chat connection you can control push notifications.
     * Push notifications are active only when chat connection is not active.
     *
     * Can be used to enable chat's push notifications when [InAppChatView] is not visible.
     * Use [restartConnection] to reestablish chat connection.
     *
     * To detect if chat connection is stopped use [isChatLoaded] or [EventsListener.onChatDisconnected] event from [EventsListener].
     */
    fun stopConnection() {
        binding.ibLcWebView.loadBlankPage()
        isChatLoaded = false
        eventsListener?.onChatDisconnected()
        MobileMessagingLogger.d(TAG, "Chat connection stopped.")
    }

    /**
     * Set the language of the Livechat Widget
     * @param locale locale's language is used by Livechat Widget and native parts
     */
    fun setLanguage(locale: Locale) {
        MobileMessagingLogger.d(TAG, "setLanguage($locale)")
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
     * @param message message to be send, max length allowed is 1000 characters
     * @param attachment to create attachment use [InAppChatMobileAttachment]'s constructor where you provide attachment's mimeType, base64 and filename
     */
    @JvmOverloads
    fun sendChatMessage(message: String?, attachment: InAppChatMobileAttachment? = null) {
        val messageEscaped = message?.let(CommonUtils::escapeJsonString)

        if (message != null && message.length > MESSAGE_MAX_LENGTH) {
            throw IllegalArgumentException("Message length exceed maximal allowed length $MESSAGE_MAX_LENGTH")
        } else if (attachment != null) {
            inAppChatClient.sendChatMessage(messageEscaped, attachment)
        } else {
            inAppChatClient.sendChatMessage(messageEscaped)
        }
    }
    //endregion

    //region Lifecycle
    private val lifecycleObserver = object : DefaultLifecycleObserver {

        override fun onCreate(owner: LifecycleOwner) {
        }

        override fun onStart(owner: LifecycleOwner) {
            restartConnection()
        }

        override fun onResume(owner: LifecycleOwner) {
            binding.ibLcWebView.onResume()
            registerReceivers()
            updateErrors()
            syncInAppChatConfigIfNeeded()
        }

        override fun onPause(owner: LifecycleOwner) {
            binding.ibLcWebView.onPause()
            unregisterReceivers()
        }

        override fun onStop(owner: LifecycleOwner) {
            stopConnection()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            binding.ibLcWebView.destroy()
        }
    }
    //endregion

    //region InAppChatWebViewManager
    private val inAppChatWebViewManager = object : InAppChatWebViewManager {

        override fun onPageStarted(url: String) {
        }

        override fun onPageFinished(url: String) {
            if (InAppChatWebView.BLANK_PAGE_URI == url) return
            binding.ibLcSpinner.invisible()
            binding.ibLcWebView.visible()
            applyLanguage()
        }

        override fun setControlsEnabled(enabled: Boolean) {
            eventsListener?.onChatLoaded(enabled)
            isChatLoaded = enabled
            lcRegIdChecker.sync()
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
            inAppChatBroadcaster.chatViewChanged(widgetView)
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
                inAppChatErrors.removeError(InAppChatErrors.CONFIG_SYNC_ERROR)
                onWidgetSynced()
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
            if (SystemInformation.isUpsideDownCakeOrAbove()) {
                ContextCompat.registerReceiver(context, broadcastEventsReceiver, intentFilter, ContextCompat.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(broadcastEventsReceiver, intentFilter)
            }
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
            val isPushRegIdMissing = mmCore.pushRegistrationId == null
            val isRegistrationPendingError =
                InternalSdkError.NO_VALID_REGISTRATION.error.code == error.code && mmCore.isRegistrationIdReported

            /**
             * 1. connection error handled separately by broadcast receiver
             * 2. sync is triggered again after registration, do not show error
             * 3. ignore any error immediately after initial app installation when pushRegId is not present yet
             */
            if (isInternetConnectionError || isPushRegIdMissing || isRegistrationPendingError)
                return

            inAppChatErrors.insertError(
                InAppChatErrors.Error(
                    InAppChatErrors.CONFIG_SYNC_ERROR,
                    error.message
                )
            )
        }
    }

    private val inAppChatErrors = InAppChatErrors { currentErrors, removedError, _ ->
        if (removedError != null) {
            //reload webView if it wasn't loaded in case when internet connection appeared
            if (InAppChatErrors.INTERNET_CONNECTION_ERROR == removedError.type) {
                hideNoInternetConnectionView()
                if (!isChatLoaded) {
                    loadChatPage(force = true)
                }
            }
        }

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
    private fun loadChatPage(force: Boolean = false) = with(binding) {
        if (ibLcWebView.url != InAppChatWebView.BLANK_PAGE_URI) {
            ibLcSpinner.visible()
            ibLcWebView.invisible()
        }
        ibLcWebView.loadChatPage(force, widgetInfo, inAppChat.jwtProvider?.provideJwt())
    }

    @Suppress("DEPRECATION")
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
        MobileMessagingLogger.d(TAG, "Update widget info")
        widgetInfo = prepareWidgetInfo()
        widgetInfo?.let {
            style = StyleFactory.create(context, attributes, widgetInfo).chatStyle()
            applyStyle(style)
            eventsListener?.onChatWidgetInfoUpdated(it)
        }
    }

    private fun onWidgetSynced() {
        if (widgetInfo == null || !isChatLoaded) {
            MobileMessagingLogger.d(TAG, "Widget synced")
            updateWidgetInfo()
            loadChatPage(force = true)
        }
    }

    private fun syncInAppChatConfigIfNeeded() {
        val pushRegistrationId = mmCore.pushRegistrationId
        if (pushRegistrationId != null && widgetInfo == null) {
            (inAppChat as? MessageHandlerModule)?.performSyncActions()
        }
    }

    private fun prepareWidgetInfo(): WidgetInfo? {
        val prefs = PropertyHelper.getDefaultMMSharedPreferences(context)
        val widgetId = prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ID.key, null)
        val widgetTitle = prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_TITLE.key, null)
        val widgetPrimaryColor = prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_PRIMARY_COLOR.key, null)
        val widgetBackgroundColor = prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_BACKGROUND_COLOR.key, null)
        val maxUploadContentSizeStr = prefs.getString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_MAX_UPLOAD_CONTENT_SIZE.key, null)
        val widgetMultiThread = prefs.getBoolean(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_MULTITHREAD.key, false)
        val callsAvailable = prefs.getBoolean(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_CALLS_AVAILABLE.key, true)
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
                widgetMultiThread,
                callsAvailable
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