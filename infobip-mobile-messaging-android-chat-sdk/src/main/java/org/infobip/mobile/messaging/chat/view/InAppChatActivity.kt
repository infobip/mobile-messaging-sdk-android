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
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetResult
import org.infobip.mobile.messaging.chat.models.MessagePayload
import org.infobip.mobile.messaging.chat.utils.applyInAppChatLanguage
import org.infobip.mobile.messaging.chat.utils.applyWindowInsets
import org.infobip.mobile.messaging.chat.utils.setStatusBarColor
import org.infobip.mobile.messaging.chat.utils.setSystemBarIconsColor
import org.infobip.mobile.messaging.chat.view.InAppChatThemeResolver.getChatViewTheme
import org.infobip.mobile.messaging.chat.view.styles.factory.StyleFactory
import org.infobip.mobile.messaging.dal.bundle.MessageBundleMapper
import org.infobip.mobile.messaging.logging.MobileMessagingLogger

class InAppChatActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "InAppChatActivity"

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
        runCatching {
            supportFragmentManager.beginTransaction()
                .replace(R.id.ib_in_app_chat_fragment, fragment, InAppChatFragment::class.java.simpleName)
                .commit()
        }.onFailure {
            MobileMessagingLogger.w(TAG, "Failed to show InAppChatFragment.", it)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun getEventsListener(): InAppChatFragment.EventsListener {
        return object : DefaultInAppChatFragmentEventsListener() {

            override fun onChatLoadingFinished(result: LivechatWidgetResult<Unit>) {
                if (result.isSuccess) {
                    handleIntent(this@InAppChatActivity.intent)
                }
            }

            override fun onExitChatPressed() {
                this@InAppChatActivity.finish()
            }

            override fun onChatWidgetInfoUpdated(widgetInfo: WidgetInfo) {
                super.onChatWidgetInfoUpdated(widgetInfo)
                val style = StyleFactory.create(this@InAppChatActivity, widgetInfo = widgetInfo).chatToolbarStyle()
                setStatusBarColor(style.statusBarBackgroundColor)
                setSystemBarIconsColor(style.lightStatusBarIcons)
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
            MobileMessagingLogger.w(TAG, "Failed to send initial livechat message.", it)
        }
    }

    private fun Intent?.extractLivechatAction(): OpenLivechatAction? {
        return this?.getBundleExtra(BroadcastParameter.EXTRA_MESSAGE)?.let { bundle ->
            OpenLivechatAction.parseFrom(Message.createFrom(bundle))
        }
    }

    private fun getInAppChatFragment(): InAppChatFragment {
        return fragment ?: InAppChatFragment().also {
            fragment = it
        }
    }

}