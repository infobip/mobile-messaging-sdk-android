package org.infobip.mobile.messaging.chat.view

import android.annotation.SuppressLint
import android.content.*
import android.net.ConnectivityManager
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import org.infobip.mobile.messaging.*
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.api.support.http.client.DefaultApiClient
import org.infobip.mobile.messaging.chat.InAppChat
import org.infobip.mobile.messaging.chat.InAppChatErrors
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.attachments.InAppChatMobileAttachment
import org.infobip.mobile.messaging.chat.core.*
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetApi
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetApiImpl
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetEventsListener
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetException
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetLanguage
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetResult
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetThread
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetThreads
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetView
import org.infobip.mobile.messaging.chat.core.widget.toInAppChatWidgetView
import org.infobip.mobile.messaging.chat.databinding.IbViewChatBinding
import org.infobip.mobile.messaging.chat.mobileapi.LivechatRegistrationChecker
import org.infobip.mobile.messaging.chat.models.ContextualData
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
     * [InAppChatView] events listener propagates chat related events
     */
    interface EventsListener : InAppChatEventsListener {
        /**
         * Attachment from chat has been interacted.
         */
        @Deprecated("Use onChatAttachmentPreviewOpened(url: String?, type: String?, caption: String?) instead")
        fun onAttachmentPreviewOpened(url: String?, type: String?, caption: String?)

        /**
         * Attachment from chat has been interacted.
         */
        fun onChatAttachmentPreviewOpened(url: String?, type: String?, caption: String?)
    }

    /**
     * [InAppChatView] errors handler allows you to define custom way to process [InAppChatView] errors.
     * You can use [DefaultInAppChatErrorHandler] to override only necessary methods.
     */
    interface ErrorsHandler : InAppChatErrorHandler

    companion object {
        private const val CHAT_NOT_AVAILABLE_ANIM_DURATION_MILLIS = 500L
        private const val CHAT_SERVICE_ERROR = "12"
        private const val CHAT_WIDGET_NOT_FOUND = "24"
        private const val TAG = "InAppChatView"
        const val MESSAGE_MAX_LENGTH = 4096
    }

    private val binding = IbViewChatBinding.inflate(LayoutInflater.from(context), this)
    private var style = StyleFactory.create(context, attributes).chatStyle()
    private val mmCore: MobileMessagingCore by lazy { MobileMessagingCore.getInstance(context) }
    private val inAppChat by lazy { InAppChat.getInstance(context) }
    private val lcRegIdChecker by lazy { LivechatRegistrationChecker(context) }
    private var inAppChatBroadcaster: InAppChatBroadcaster = InAppChatBroadcasterImpl(context)
    private val localizationUtils = LocalizationUtils.getInstance(context)
    private val propertyHelper = PropertyHelper(context)
    private var widgetInfo: WidgetInfo? = null
    private var lastControlsVisibility: Boolean? = null
    private var lifecycle: Lifecycle? = null
    private val pushRegistrationId
        get() = mmCore.pushRegistrationId
    private val coroutineScope: CoroutineScope
        get() = findViewTreeLifecycleOwner()?.lifecycleScope ?: SessionStorage.scope //when View is attached to the window, it has a lifecycle owner otherwise use session scope
    private val livechatWidgetApi: LivechatWidgetApi by lazy {
        LivechatWidgetApiImpl(
            binding.ibLcWebView,
            mmCore,
            inAppChat,
            propertyHelper,
            coroutineScope
        ).apply { eventsListener = livechatWidgetEventsListener }
    }
    private val isWidgetLoaded: Boolean
        get() = livechatWidgetApi.isWidgetLoaded

    /**
     * [InAppChatView] event listener allows you to listen to livechat widget events.
     * You can use [DefaultInAppChatViewEventsListener] to override only necessary methods.
     */
    var eventsListener: EventsListener? = null

    /**
     * Returns true if chat is synchronized and multithread feature is enabled, otherwise returns false.
     */
    val isMultiThread: Boolean
        get() = widgetInfo?.isMultiThread ?: false

    val defaultErrorsHandler: ErrorsHandler = object : ErrorsHandler {

        private fun parseWidgetError(errorJson: String): String {
            return runCatching {
                val error = LivechatWidgetException.parse(errorJson)
                val message: String? = error.message
                val code: String? = error.code?.toString()
                when {
                    message?.isNotBlank() == true && code?.isNotBlank() == true -> "$message " + localizationUtils.getString(R.string.ib_chat_error_code, code)
                    message?.isNotBlank() == true -> message
                    code?.isNotBlank() == true -> localizationUtils.getString(R.string.ib_chat_error, localizationUtils.getString(R.string.ib_chat_error_code, code))
                    else -> localizationUtils.getString(R.string.ib_chat_error, errorJson)
                }
            }.onFailure {
                MobileMessagingLogger.e("Could not parse JS error json.", it)
            }.getOrDefault(localizationUtils.getString(R.string.ib_chat_error, errorJson))
        }

        override fun handlerError(error: String) {
            MobileMessagingLogger.e(TAG, "Unhandled error $error")
        }

        override fun handlerWidgetError(error: String) {
            val message = if (CommonUtils.isJSON(error)) parseWidgetError(error) else error
            Snackbar.make(binding.root, message, Snackbar.LENGTH_INDEFINITE)
                .also {
                    runCatching {
                        var textView = it.view.findViewById<TextView>(androidx.core.R.id.text)
                        if (textView == null) {
                            textView = it.view.findViewById(com.google.android.material.R.id.snackbar_text)
                        }
                        if (textView != null) {
                            textView.maxLines = 4
                        }
                    }
                }
                .setAction(R.string.ib_chat_ok) {}
                .show()
        }

        override fun handlerNoInternetConnectionError(hasConnection: Boolean) {
            if (hasConnection) {
                hideNoInternetConnectionView()
            } else {
                showNoInternetConnectionView()
            }
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
     * Loads chat and establish connection to be able to receive real time updates - new messages.
     * Chat connection is established and stopped based on provided [Lifecycle].
     * Chat connection is active only when [Lifecycle.State] is at least [Lifecycle.State.STARTED].
     *
     * @param lifecycle lifecycle of android Activity or Fragment
     */
    fun init(lifecycle: Lifecycle) {
        this.lifecycle = lifecycle
        updateWidgetInfo()
        inAppChat.activate()
        lifecycle.addObserver(lifecycleObserver)
        loadWidget()
    }

    /**
     * Pauses chat connection.
     *
     * It is not needed to use it in most cases as chat connection is established and paused based on [Lifecycle] provided in [init].
     * Chat connection is paused when [Lifecycle.State] is below [Lifecycle.State.STARTED].
     *
     * By chat connection you can control push notifications.
     * Push notifications are active only when chat connection is not active.
     *
     * Can be used to enable chat's push notifications when [InAppChatView] is not visible.
     * Use [resumeChatConnection] to reestablish chat connection.
     *
     * To detect if chat connection is paused use [EventsListener.onChatConnectionPaused] event from [EventsListener].
     */
    fun pauseChatConnection() {
        livechatWidgetApi.pauseConnection()
        if (lifecycle?.currentState == Lifecycle.State.DESTROYED) {
            binding.ibLcWebView.destroy()
        }
    }

    /**
     * Stops chat connection.
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
     * To detect if chat connection is stopped use [EventsListener.onChatDisconnected] event from [EventsListener].
     */
    @Deprecated("Use pauseChatConnection() instead", ReplaceWith("pauseChatConnection()"))
    fun stopConnection() {
        pauseChatConnection()
    }

    /**
     * Use it to resume chat connection when you previously called [pauseChatConnection].
     *
     * It is not needed to use it in most cases as chat connection is established and stopped based on [Lifecycle] provided in [init].
     * Chat connection is active only when [Lifecycle.State] is at least [Lifecycle.State.STARTED].
     *
     * By chat connection you can control push notifications.
     * Push notifications are suppressed while the chat connection is active.
     *
     * To detect if chat connection was resumed use [EventsListener.onChatConnectionResumed] event from [EventsListener].
     */
    fun resumeChatConnection() {
        if (widgetInfo == null)
            updateWidgetInfo()
        livechatWidgetApi.resumeConnection()
    }

    /**
     * Use it to re-establish chat connection when you previously called [stopConnection].
     *
     * It is not needed to use it in most cases as chat connection is established and stopped based on [Lifecycle] provided in [init].
     * Chat connection is active only when [Lifecycle.State] is at least [Lifecycle.State.STARTED].
     *
     * By chat connection you can control push notifications.
     * Push notifications are suppressed while the chat connection is active.
     *
     * To detect if chat connection was re-established use [EventsListener.onChatReconnected] event from [EventsListener].
     */
    @Deprecated("Use resumeChatConnection() instead", ReplaceWith("resumeChatConnection()"))
    fun restartConnection() {
        resumeChatConnection()
    }

    /**
     * Sends a message with optional [InAppChatMobileAttachment].
     * @param message message to be send, max length allowed is 4096 characters
     * @param attachment to create attachment use [InAppChatMobileAttachment]'s constructor where you provide attachment's mimeType, base64 and filename
     */
    @JvmOverloads
    @Throws(IllegalArgumentException::class)
    fun sendChatMessage(message: String?, attachment: InAppChatMobileAttachment? = null) {
        val messageEscaped = message?.let(CommonUtils::escapeJsonString)
        if (message != null && message.length > MESSAGE_MAX_LENGTH) {
            throw IllegalArgumentException("Message length exceed maximal allowed length $MESSAGE_MAX_LENGTH")
        } else {
            livechatWidgetApi.sendMessage(messageEscaped, attachment)
        }
    }

    /**
     * Sends a draft message.
     * @param draft message
     */
    fun sendChatMessageDraft(draft: String) {
        livechatWidgetApi.sendDraft(draft)
    }

    /**
     * Set contextual data of the livechat widget.
     *
     * If the function is called when the chat is loaded,
     * data will be sent immediately, otherwise they will be sent to the chat once it is loaded.
     *
     * Every function invocation will overwrite the previous contextual data.
     *
     * @param data contextual data in the form of JSON string
     * @param allMultiThreadStrategy multithread strategy flag, true -> ALL, false -> ACTIVE
     * @see [InAppChatView.EventsListener.onChatLoaded] to detect if chat is loaded
     */
    @Deprecated("Use sendContextualData(data: String, flag: MultithreadStrategy) instead")
    fun sendContextualData(data: String, allMultiThreadStrategy: Boolean) {
        val flag = if (allMultiThreadStrategy) MultithreadStrategy.ALL else MultithreadStrategy.ACTIVE
        sendContextualData(data, flag)
    }

    /**
     * Set contextual data of the livechat widget.
     *
     * If the function is called when the chat is loaded,
     * data will be sent immediately, otherwise they will be sent to the chat once it is loaded.
     *
     * Every function invocation will overwrite the previous contextual data.
     *
     * @param data contextual data in the form of JSON string
     * @param flag multithread strategy [MultithreadStrategy]
     * @see [InAppChatView.EventsListener.onChatLoaded] to detect if chat is loaded
     */
    fun sendContextualData(data: String, flag: MultithreadStrategy) {
        if (isWidgetLoaded) {
            livechatWidgetApi.sendContextualData(data, flag)
        } else {
            SessionStorage.contextualData = ContextualData(data, flag)
            MobileMessagingLogger.d(TAG, "Contextual data is stored, will be sent once chat is loaded.")
        }
    }

    /**
     * Requests current threads from livechat widget.
     *
     * You can observe result by [InAppChatView.EventsListener.onChatThreadsReceived] event.
     */
    fun getThreads() {
        livechatWidgetApi.getThreads()
    }

    /**
     * Requests shown thread - active from livechat widget.
     *
     * You can observe result by [InAppChatView.EventsListener.onChatActiveThreadReceived] event.
     */
    fun getActiveThread() {
        livechatWidgetApi.getActiveThread()
    }

    /**
     * Navigates livechat widget to thread specified by provided [threadId].
     *
     * You can observe result by [InAppChatView.EventsListener.onChatThreadShown] event.
     *
     * @param threadId thread to be shown
     */
    fun showThread(threadId: String) {
        livechatWidgetApi.showThread(threadId)
    }

    /**
     * Navigates livechat widget from [LivechatWidgetView.THREAD] back to [LivechatWidgetView.THREAD_LIST] destination in multithread widget. It does nothing if widget is not multithread.
     */
    fun showThreadList() {
        livechatWidgetApi.showThreadList()
    }

    /**
     * Set an in-app chat's language
     * @param locale locale's language is used by livechat widget and in-app chat native parts
     */
    @Deprecated(
        "Use setLanguage(LivechatWidgetLanguage) instead",
        ReplaceWith("setLanguage(LivechatWidgetLanguage.findLanguageOrDefault(locale))", "org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetLanguage")
    )
    fun setLanguage(locale: Locale) {
        setLanguage(LivechatWidgetLanguage.findLanguageOrDefault(locale))
    }

    /**
     * Set an in-app chat's language
     * @param language language is used by livechat widget and in-app chat native parts
     */
    fun setLanguage(language: LivechatWidgetLanguage) {
        MobileMessagingLogger.d(TAG, "setLanguage($language)")
        propertyHelper.saveString(MobileMessagingChatProperty.IN_APP_CHAT_LANGUAGE, language.widgetCode)
        livechatWidgetApi.setLanguage(language)
        localizationUtils.setLanguage(language.locale)
        updateWidgetInfo()
    }

    /**
     * Returns current in-app chat language
     *
     * @return current in-app chat language or default [LivechatWidgetLanguage.ENGLISH]
     */
    fun getLanguage(): LivechatWidgetLanguage {
        return inAppChat.language
    }

    /**
     * Sets a livechat widget's theme.
     *
     * You can define widget themes in <a href="https://portal.infobip.com/apps/livechat/widgets">Live chat widget setup page</a> in Infobip Portal, section `Advanced customization`.
     * Please check widget <a href="https://www.infobip.com/docs/live-chat/widget-customization">documentation</a> for more details.
     *
     * Function allows to change widget theme while chat is shown - in runtime.
     * If you set widget theme before chat is initialized by [InAppChatView.init] the theme will be used once chat is loaded.
     *
     * @param widgetThemeName unique theme name, empty or blank value is ignored
     */
    fun setWidgetTheme(widgetThemeName: String) {
        SessionStorage.widgetTheme = widgetThemeName
        if (widgetThemeName.isNotBlank() && isWidgetLoaded) {
            livechatWidgetApi.setTheme(widgetThemeName)
        }
    }

    /**
     * Get current livechat widget theme.
     *
     * @return applied theme name of livechat widget
     */
    fun getWidgetTheme(): String? {
        return inAppChat.widgetTheme
    }

    override fun setForceDarkAllowed(allow: Boolean) {
        super.setForceDarkAllowed(allow)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.ibLcWebView.isForceDarkAllowed = allow
        }
    }
    //endregion

    //region Lifecycle
    private val lifecycleObserver = object : DefaultLifecycleObserver {

        override fun onCreate(owner: LifecycleOwner) {
        }

        override fun onStart(owner: LifecycleOwner) {
            //do not call widget API when chat is not loaded yet - initial loading
            if (isWidgetLoaded)
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
        }
    }
    //endregion

    //region LivechatWidgetEventsListener
    private val livechatWidgetEventsListener: LivechatWidgetEventsListener = object : LivechatWidgetEventsListener {

        override fun onPageStarted(url: String?) {}

        override fun onPageFinished(url: String?) {
            binding.ibLcSpinner.invisible()
            binding.ibLcWebView.visible()
        }

        override fun onLoadingFinished(result: LivechatWidgetResult<Boolean>) {
            val mappedResult: LivechatWidgetResult<Unit> = when (result) {
                is LivechatWidgetResult.Error -> result
                is LivechatWidgetResult.Success -> {
                    if (result.payload) {
                        LivechatWidgetResult.Success(Unit)
                    } else {
                        LivechatWidgetResult.Error("Chat has been reset and is no longer loaded.")
                    }
                }
            }

            when (mappedResult) {
                is LivechatWidgetResult.Error -> {
                    errorsHandler.handlerWidgetError(mappedResult.throwable.message ?: localizationUtils.getString(R.string.ib_chat_error, "Unknown error"))
                    binding.ibLcSpinner.invisible()
                    binding.ibLcWebView.visible()
                }

                is LivechatWidgetResult.Success -> {
                    inAppChat.resetMessageCounter()
                    setLanguage(inAppChat.language)
                    SessionStorage.contextualData?.let {
                        sendContextualData(it.data, it.allMultiThreadStrategy)
                    }
                }
            }

            eventsListener?.onChatLoaded(mappedResult.isSuccess)
            eventsListener?.onChatLoadingFinished(mappedResult)
            lcRegIdChecker.sync()
            if (eventsListener == null && mappedResult.isError) {
                MobileMessagingLogger.e(TAG, "Chat could not load:", mappedResult.errorOrNull())
            }
        }

        override fun onConnectionPaused(result: LivechatWidgetResult<Unit>) {
            eventsListener?.onChatDisconnected()
            eventsListener?.onChatConnectionPaused(result)
            if (eventsListener == null && result.isError) {
                MobileMessagingLogger.e(TAG, "Chat could not pause connection:", result.errorOrNull())
            }
        }

        override fun onConnectionResumed(result: LivechatWidgetResult<Unit>) {
            eventsListener?.onChatReconnected()
            eventsListener?.onChatConnectionResumed(result)
            if (eventsListener == null && result.isError) {
                MobileMessagingLogger.e(TAG, "Chat could not resume connection:", result.errorOrNull())
            }
        }

        override fun onMessageSent(result: LivechatWidgetResult<String?>) {
            eventsListener?.onChatMessageSent(result)
            if (eventsListener == null && result.isError) {
                MobileMessagingLogger.e(TAG, "Chat could not send message:", result.errorOrNull())
            }
        }

        override fun onDraftSent(result: LivechatWidgetResult<String?>) {
            eventsListener?.onChatDraftSent(result)
            if (eventsListener == null && result.isError) {
                MobileMessagingLogger.e(TAG, "Chat could not send draft:", result.errorOrNull())
            }
        }

        override fun onContextualDataSent(result: LivechatWidgetResult<String?>) {
            eventsListener?.onChatContextualDataSent(result)
            if (result.isSuccess) {
                SessionStorage.contextualData = null
            }
            if (eventsListener == null && result.isError) {
                MobileMessagingLogger.e(TAG, "Chat could not send contextual data:", result.errorOrNull())
            }
        }

        override fun onThreadListShown(result: LivechatWidgetResult<Unit>) {
            eventsListener?.onChatThreadListShown(result)
            if (eventsListener == null && result.isError) {
                MobileMessagingLogger.e(TAG, "Chat could not show thread list:", result.errorOrNull())
            }
        }

        override fun onLanguageChanged(result: LivechatWidgetResult<String?>) {
            eventsListener?.onChatLanguageChanged(result)
            if (eventsListener == null && result.isError) {
                MobileMessagingLogger.e(TAG, "Chat could not set language:", result.errorOrNull())
            }
        }

        override fun onThemeChanged(result: LivechatWidgetResult<String?>) {
            eventsListener?.onChatWidgetThemeChanged(result.getOrNull() ?: "")
            eventsListener?.onChatWidgetThemeChanged(result)
            if (eventsListener == null && result.isError) {
                MobileMessagingLogger.e(TAG, "Chat could not set widget theme:", result.errorOrNull())
            }
        }

        override fun onControlsVisibilityChanged(visible: Boolean) {
            if (lastControlsVisibility != isVisible) {
                eventsListener?.onChatControlsVisibilityChanged(isVisible)
            }
            lastControlsVisibility = isVisible
        }

        override fun onAttachmentPreviewOpened(url: String?, type: String?, caption: String?) {
            eventsListener?.onAttachmentPreviewOpened(url, type, caption)
            eventsListener?.onChatAttachmentPreviewOpened(url, type, caption)
        }

        override fun onWidgetViewChanged(view: LivechatWidgetView) {
            eventsListener?.onChatViewChanged(view.toInAppChatWidgetView())
            eventsListener?.onChatViewChanged(view)
            inAppChatBroadcaster.chatViewChanged(view)
        }

        override fun onRawMessageReceived(message: String?) {
            if (message?.isNotBlank() == true)
                eventsListener?.onChatRawMessageReceived(message)
        }

        override fun onThreadsReceived(result: LivechatWidgetResult<LivechatWidgetThreads>) {
            eventsListener?.onChatThreadsReceived(result)
            if (eventsListener == null && result.isError) {
                MobileMessagingLogger.e(TAG, "Chat could not obtain threads:", result.errorOrNull())
            }
        }

        override fun onActiveThreadReceived(result: LivechatWidgetResult<LivechatWidgetThread?>) {
            eventsListener?.onChatActiveThreadReceived(result)
            if (eventsListener == null && result.isError) {
                MobileMessagingLogger.e(TAG, "Chat could not obtain active thread:", result.errorOrNull())
            }
        }

        override fun onThreadShown(result: LivechatWidgetResult<LivechatWidgetThread>) {
            eventsListener?.onChatThreadShown(result)
            if (eventsListener == null && result.isError) {
                MobileMessagingLogger.e(TAG, "Chat could not show thread:", result.errorOrNull())
            }
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
                    inAppChatErrors.insertError(InAppChatErrors.Error(InAppChatErrors.INTERNET_CONNECTION_ERROR, localizationUtils.getString(R.string.ib_chat_no_connection)))
                } else {
                    inAppChatErrors.removeError(InAppChatErrors.INTERNET_CONNECTION_ERROR)
                }
            } else if (action == InAppChatEvent.CHAT_CONFIGURATION_SYNCED.key) {
                inAppChatErrors.removeError(InAppChatErrors.CONFIG_SYNC_ERROR)
                onWidgetSynced()
            } else if (action == Event.API_COMMUNICATION_ERROR.key && intent.hasExtra(BroadcastParameter.EXTRA_EXCEPTION)) {
                val mobileMessagingError = intent.getSerializableExtra(BroadcastParameter.EXTRA_EXCEPTION) as MobileMessagingError?
                val errorCode = mobileMessagingError!!.code
                if (errorCode == CHAT_SERVICE_ERROR || errorCode == CHAT_WIDGET_NOT_FOUND) {
                    inAppChatErrors.insertError(InAppChatErrors.Error(InAppChatErrors.CONFIG_SYNC_ERROR, mobileMessagingError.message))
                }
            } else if (action == Event.REGISTRATION_CREATED.key) {
                syncInAppChatConfigIfNeeded()
            }
        }
    }

    @SuppressLint("WrongConstant")
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
        val configSyncResult = SessionStorage.configSyncResult
        if (configSyncResult != null && !configSyncResult.isSuccess) {
            val error = configSyncResult.error
            val isInternetConnectionError = DefaultApiClient.ErrorCode.API_IO_ERROR.value == error.code && error.type == MobileMessagingError.Type.SERVER_ERROR
            val isPushRegIdMissing = pushRegistrationId == null
            val isRegistrationPendingError = InternalSdkError.NO_VALID_REGISTRATION.error.code == error.code && mmCore.isRegistrationIdReported

            /**
             * 1. connection error handled separately by broadcast receiver
             * 2. sync is triggered again after registration, do not show error
             * 3. ignore any error immediately after initial app installation when pushRegId is not present yet
             */
            if (isInternetConnectionError || isPushRegIdMissing || isRegistrationPendingError)
                return

            inAppChatErrors.insertError(InAppChatErrors.Error(InAppChatErrors.CONFIG_SYNC_ERROR, error.message))
        }
    }

    private val inAppChatErrors = InAppChatErrors { currentErrors, removedError, _ ->
        if (removedError != null) {
            if (InAppChatErrors.INTERNET_CONNECTION_ERROR == removedError.type) {
                errorsHandler.handlerNoInternetConnectionError(true)
                if (!isWidgetLoaded) {
                    loadWidget()
                }
            }
        }

        for (error in currentErrors) {
            if (InAppChatErrors.INTERNET_CONNECTION_ERROR == error.type) {
                errorsHandler.handlerNoInternetConnectionError(false)
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
    private fun loadWidget() {
        val pushRegId = pushRegistrationId
        val widgetId = widgetInfo?.id
        val jwt = inAppChat.widgetJwtProvider?.provideJwt()
        val domain = inAppChat.domain
        val widgetTheme = inAppChat.widgetTheme

        if (pushRegId.isNullOrBlank() || widgetId.isNullOrBlank()) {
            MobileMessagingLogger.e(TAG, "Chat loading skipped, pushRegId($pushRegId) or widgetId($widgetId) is missing.")
            return
        }

        with(binding) {
            ibLcSpinner.visible()
            ibLcWebView.invisible()
            livechatWidgetApi.loadWidget(widgetId, jwt, domain, widgetTheme)
        }
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
        if (widgetInfo == null || !isWidgetLoaded) {
            MobileMessagingLogger.d(TAG, "Widget synced")
            updateWidgetInfo()
            loadWidget()
        }
    }

    private fun syncInAppChatConfigIfNeeded() {
        if (pushRegistrationId != null && widgetInfo == null) {
            (inAppChat as? MessageHandlerModule)?.performSyncActions()
        }
    }

    private fun prepareWidgetInfo(): WidgetInfo? {
        val widgetId = propertyHelper.findString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_ID)
        return widgetId?.let {
            val widgetTitle = propertyHelper.findString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_TITLE)
            val widgetPrimaryColor = propertyHelper.findString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_PRIMARY_COLOR)
            val widgetBackgroundColor = propertyHelper.findString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_BACKGROUND_COLOR)
            val maxUploadContentSizeStr = propertyHelper.findString(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_MAX_UPLOAD_CONTENT_SIZE)
            val widgetMultiThread = propertyHelper.findBoolean(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_MULTITHREAD)
            val widgetMultichannelConversation = propertyHelper.findBoolean(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_MULTICHANNEL_CONVERSATION)
            val callsEnabled = propertyHelper.findBoolean(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_CALLS_ENABLED)
            var maxUploadContentSize = InAppChatMobileAttachment.DEFAULT_MAX_UPLOAD_CONTENT_SIZE
            if (StringUtils.isNotBlank(maxUploadContentSizeStr)) {
                maxUploadContentSize = maxUploadContentSizeStr!!.toLong()
            }
            val themeNames: Set<String>? = propertyHelper.findStringSet(MobileMessagingChatProperty.IN_APP_CHAT_WIDGET_THEMES)
            WidgetInfo(
                it,
                widgetTitle,
                widgetPrimaryColor,
                widgetBackgroundColor,
                maxUploadContentSize,
                widgetMultiThread,
                widgetMultichannelConversation,
                callsEnabled,
                themeNames?.toList()
            )
        }
    }
    //endregion
}