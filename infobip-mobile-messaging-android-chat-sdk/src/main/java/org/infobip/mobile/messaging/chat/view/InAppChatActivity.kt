package org.infobip.mobile.messaging.chat.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.infobip.mobile.messaging.BroadcastParameter
import org.infobip.mobile.messaging.Message
import org.infobip.mobile.messaging.OpenLivechatAction
import org.infobip.mobile.messaging.api.chat.WidgetInfo
import org.infobip.mobile.messaging.chat.InAppChat
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.core.InAppChatException
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetMessage
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetResult
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetThread
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetThreads
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetView
import org.infobip.mobile.messaging.chat.models.MessagePayload
import org.infobip.mobile.messaging.chat.utils.applyInAppChatLanguage
import org.infobip.mobile.messaging.chat.utils.applyWindowInsets
import org.infobip.mobile.messaging.chat.utils.setStatusBarColor
import org.infobip.mobile.messaging.chat.utils.setSystemBarIconsColor
import org.infobip.mobile.messaging.chat.view.InAppChatFragment.ErrorsHandler
import org.infobip.mobile.messaging.chat.view.InAppChatFragment.EventsListener
import org.infobip.mobile.messaging.chat.view.InAppChatThemeResolver.getChatViewTheme
import org.infobip.mobile.messaging.chat.view.styles.factory.StyleFactory
import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper
import org.infobip.mobile.messaging.logging.MobileMessagingLogger

class InAppChatActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "InAppChatActivity"
        private val IN_APP_CHAT_FRAGMENT_TAG: String = InAppChatFragment::class.java.name

        @JvmOverloads
        @JvmStatic
        fun startIntent(
            context: Context,
            intentFlags: Int = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            message: Message? = null,
        ): Intent {
            return Intent(context, InAppChatActivity::class.java).apply {
                addFlags(intentFlags)
                if (message != null) {
                    putExtra(BroadcastParameter.EXTRA_MESSAGE, MessageBundleMapper.messageToBundle(message))
                }
            }
        }
    }

    private var fragment: InAppChatFragment? = null

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.applyInAppChatLanguage())
    }

    @SuppressLint("CommitTransaction")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getChatViewTheme(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ib_activity_chat)
        applyWindowInsets()
        val fragment = getInAppChatFragment()
        fragment.eventsListener = getEventsListener()
        fragment.errorsHandler = getErrorsHandler() ?: fragment.defaultErrorsHandler
        runCatching {
            supportFragmentManager.beginTransaction()
                .replace(R.id.ib_in_app_chat_fragment, fragment, IN_APP_CHAT_FRAGMENT_TAG)
                .commit()
        }.onFailure {
            MobileMessagingLogger.e(TAG, "Failed to show InAppChatFragment.", it)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun getEventsListener(): EventsListener {
        val activityEventsListener = InAppChat.getInstance(this).inAppChatScreen().eventsListener
        return object : EventsListener {

            override fun onChatAttachmentPreviewOpened(url: String?, type: String?, caption: String?): Boolean {
                return (activityEventsListener as? EventsListener)?.onChatAttachmentPreviewOpened(url, type, caption) ?: false
            }

            override fun onExitChatPressed() {
                (activityEventsListener as? EventsListener)?.onExitChatPressed()
                this@InAppChatActivity.finish()
            }

            override fun onChatLoadingFinished(result: LivechatWidgetResult<Unit>) {
                activityEventsListener?.onChatLoadingFinished(result)
                if (result.isSuccess) {
                    handleIntent(this@InAppChatActivity.intent)
                }
            }

            override fun onChatConnectionPaused(result: LivechatWidgetResult<Unit>) {
                activityEventsListener?.onChatConnectionPaused(result)
            }

            override fun onChatConnectionResumed(result: LivechatWidgetResult<Unit>) {
                activityEventsListener?.onChatConnectionResumed(result)
            }

            override fun onChatSent(result: LivechatWidgetResult<LivechatWidgetMessage?>) {
                activityEventsListener?.onChatSent(result)
            }

            override fun onChatContextualDataSent(result: LivechatWidgetResult<String?>) {
                activityEventsListener?.onChatContextualDataSent(result)
            }

            override fun onChatThreadCreated(result: LivechatWidgetResult<LivechatWidgetMessage?>) {
                activityEventsListener?.onChatThreadCreated(result)
            }

            override fun onChatThreadsReceived(result: LivechatWidgetResult<LivechatWidgetThreads>) {
                activityEventsListener?.onChatThreadsReceived(result)
            }

            override fun onChatActiveThreadReceived(result: LivechatWidgetResult<LivechatWidgetThread?>) {
                activityEventsListener?.onChatActiveThreadReceived(result)
            }

            override fun onChatThreadShown(result: LivechatWidgetResult<LivechatWidgetThread>) {
                activityEventsListener?.onChatThreadShown(result)
            }

            override fun onChatThreadListShown(result: LivechatWidgetResult<Unit>) {
                activityEventsListener?.onChatThreadListShown(result)
            }

            override fun onChatLanguageChanged(result: LivechatWidgetResult<String?>) {
                activityEventsListener?.onChatLanguageChanged(result)
            }

            override fun onChatWidgetThemeChanged(result: LivechatWidgetResult<String?>) {
                activityEventsListener?.onChatWidgetThemeChanged(result)
            }

            override fun onChatControlsVisibilityChanged(isVisible: Boolean) {
                activityEventsListener?.onChatControlsVisibilityChanged(isVisible)
            }

            override fun onChatViewChanged(widgetView: LivechatWidgetView) {
                activityEventsListener?.onChatViewChanged(widgetView)
            }

            override fun onChatWidgetInfoUpdated(widgetInfo: WidgetInfo) {
                activityEventsListener?.onChatWidgetInfoUpdated(widgetInfo)
                val style = StyleFactory.create(this@InAppChatActivity, widgetInfo = widgetInfo).chatToolbarStyle()
                setStatusBarColor(style.statusBarBackgroundColor)
                setSystemBarIconsColor(style.lightStatusBarIcons)
            }

            override fun onChatRawMessageReceived(rawMessage: String) {
                activityEventsListener?.onChatRawMessageReceived(rawMessage)
            }
        }
    }

    private fun getErrorsHandler(): ErrorsHandler? {
        return InAppChat.getInstance(this).inAppChatScreen().errorHandler?.let { activityErrorHandler ->
            object : ErrorsHandler {
                override fun handlerError(error: String) {
                    activityErrorHandler.handlerError(error)
                }

                override fun handlerWidgetError(error: String) {
                    activityErrorHandler.handlerWidgetError(error)
                }

                override fun handlerNoInternetConnectionError(hasConnection: Boolean) {
                    activityErrorHandler.handlerNoInternetConnectionError(hasConnection)
                }

                override fun handleError(exception: InAppChatException): Boolean {
                    return activityErrorHandler.handleError(exception)
                }
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        runCatching {
            intent?.extractLivechatAction()?.let { action ->
                val fragment = getInAppChatFragment()
                val message = action.keyword
                when {
                    message?.isNotBlank() == true && fragment.isMultiThread -> fragment.createThread(MessagePayload.Basic(message = message))
                    message?.isNotBlank() == true && !fragment.isMultiThread -> fragment.send(MessagePayload.Basic(message = message))
                    fragment.isMultiThread -> fragment.openNewThread()
                }
                intent.removeExtra(BroadcastParameter.EXTRA_MESSAGE)
            }
        }.onFailure {
            MobileMessagingLogger.e(TAG, "Failed to send initial livechat message.", it)
        }
    }

    private fun Intent?.extractLivechatAction(): OpenLivechatAction? {
        return this?.getBundleExtra(BroadcastParameter.EXTRA_MESSAGE)?.let { bundle ->
            OpenLivechatAction.parseFrom(Message.createFrom(bundle))
        }
    }

    private fun getInAppChatFragment(): InAppChatFragment {
        return fragment
            ?: (supportFragmentManager.findFragmentByTag(IN_APP_CHAT_FRAGMENT_TAG) as? InAppChatFragment)
            ?: InAppChatFragment()
                .also { fragment = it }
    }

}