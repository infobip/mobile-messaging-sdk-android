package org.infobip.mobile.messaging.chat.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.infobip.mobile.messaging.BroadcastParameter
import org.infobip.mobile.messaging.Message
import org.infobip.mobile.messaging.OpenLivechatAction
import org.infobip.mobile.messaging.chat.R
import org.infobip.mobile.messaging.chat.core.widget.LivechatWidgetResult
import org.infobip.mobile.messaging.chat.models.MessagePayload
import org.infobip.mobile.messaging.chat.utils.applyInAppChatLanguage
import org.infobip.mobile.messaging.chat.view.InAppChatThemeResolver.getChatViewTheme
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

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getChatViewTheme(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ib_activity_chat)
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
        sendInitialMessage(intent)
    }

    private fun getEventsListener(): InAppChatFragment.EventsListener {
        return object : DefaultInAppChatFragmentEventsListener() {

            override fun onChatLoadingFinished(result: LivechatWidgetResult<Unit>) {
                if (result.isSuccess) {
                    sendInitialMessage(this@InAppChatActivity.intent)
                }
            }

            override fun onExitChatPressed() {
                this@InAppChatActivity.finish()
            }

        }
    }

    private fun sendInitialMessage(intent: Intent?) {
        runCatching {
            intent?.extractLivechatMessage()?.let { message ->
                sendMessage(message)
                intent.removeExtra(BroadcastParameter.EXTRA_MESSAGE)
            }
        }.onFailure {
            MobileMessagingLogger.w(TAG, "Failed to send initial livechat message.", it)
        }
    }

    private fun sendMessage(message: String) {
        val fragment = getInAppChatFragment()
        val messagePayload = MessagePayload.Basic(message = message)
        if (fragment.isMultiThread) {
            fragment.createThread(messagePayload)
        } else {
            fragment.send(messagePayload)
        }
    }

    private fun Intent?.extractLivechatMessage(): String? {
        return this?.getBundleExtra(BroadcastParameter.EXTRA_MESSAGE)?.let { bundle ->
            OpenLivechatAction.parseFrom(Message.createFrom(bundle))?.keyword?.takeIf { it.isNotBlank() }
        }
    }

    private fun getInAppChatFragment(): InAppChatFragment {
        return fragment ?: InAppChatFragment().also {
            fragment = it
        }
    }

}