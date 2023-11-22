package org.infobip.mobile.messaging.chat.core;

import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethods.handleMessageDraftSend;
import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethods.handleMessageSend;
import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethods.handleMessageWithAttachmentSend;
import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethods.mobileChatPause;
import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethods.mobileChatResume;
import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethods.sendContextualData;
import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethods.setLanguage;
import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethods.showThreadList;
import static org.infobip.mobile.messaging.chat.utils.CommonUtils.isOSOlderThanKitkat;
import static org.infobip.mobile.messaging.util.StringUtils.isNotBlank;

import android.os.Handler;
import android.os.Looper;

import org.infobip.mobile.messaging.chat.attachments.InAppChatMobileAttachment;
import org.infobip.mobile.messaging.chat.view.InAppChatWebView;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.util.StringUtils;

public class InAppChatClientImpl implements InAppChatClient {

    private final InAppChatWebView webView;
    private static final String TAG = InAppChatClient.class.getSimpleName();
    private final Handler handler = new Handler(Looper.getMainLooper());

    public InAppChatClientImpl(InAppChatWebView webView) {
        this.webView = webView;
    }

    @Override
    public void sendChatMessage(String message) {
        if (canSendMessage(message)) {
            String script = buildWidgetMethodInvocation(handleMessageSend.name(), message);
            executeScript(script);
        }
    }

    @Override
    public void sendChatMessage(String message, InAppChatMobileAttachment attachment) {
        String base64UrlString = attachment.base64UrlString();
        String fileName = attachment.getFileName();

        // message can be null - its OK
        if (canSendMessage(base64UrlString)) {
            String script = buildWidgetMethodInvocation(handleMessageWithAttachmentSend.name(), isOSOlderThanKitkat(), message, base64UrlString, fileName);
            executeScript(script);
        } else {
            MobileMessagingLogger.e("[InAppChat] can't send attachment, base64 is empty");
        }
    }

    @Override
    public void sendInputDraft(String draft) {
        executeScript(buildWidgetMethodInvocation(handleMessageDraftSend.name(), isOSOlderThanKitkat(), draft));
    }

    @Override
    public void setLanguage(String language) {
        if (StringUtils.isNotBlank(language)) {
            Language widgetLanguage = Language.findLanguage(language);
            if (widgetLanguage == null) {
                MobileMessagingLogger.e("Language " + language + " is not supported. Used default language " + Language.ENGLISH.getLocale());
                widgetLanguage = Language.ENGLISH;
            }
            String script = buildWidgetMethodInvocation(setLanguage.name(), isOSOlderThanKitkat(), widgetLanguage.getLocale());
            executeScript(script);
        }
    }

    @Override
    public void sendContextualData(String data, InAppChatMultiThreadFlag multiThreadFlag) {
        if (!data.isEmpty()) {
            StringBuilder script = new StringBuilder();
            if (isOSOlderThanKitkat()) {
                script.append("javascript:");
            }
            script.append(sendContextualData.name()).append("(").append(data).append(", '").append(multiThreadFlag).append("')");
            executeScript(script.toString());
        }
    }

    @Override
    public void showThreadList() {
        executeScript(buildWidgetMethodInvocation(showThreadList.name(), isOSOlderThanKitkat()));
    }

    @Override
    public void mobileChatPause() {
        executeScript(buildWidgetMethodInvocation(mobileChatPause.name(), isOSOlderThanKitkat()));
    }

    @Override
    public void mobileChatResume() {
        executeScript(buildWidgetMethodInvocation(mobileChatResume.name(), isOSOlderThanKitkat()));
    }

    /**
     * Executes JS script on UI thread.
     *
     * @param script to be executed
     */
    private void executeScript(String script) {
        if (webView != null) {
            try {
                handler.post(() -> webView.evaluateJavascriptMethod(script, value -> {
                    if (value != null && !"null".equals(value)) {
                        MobileMessagingLogger.d(TAG, value);
                    }
                }));
            } catch (Exception e) {
                MobileMessagingLogger.e("Failed to execute webView JS script" + e.getMessage());
            }
        }
    }

    private boolean canSendMessage(String message) {
        return isNotBlank(message);
    }

    private String buildWidgetMethodInvocation(String methodName, String... params) {
        return this.buildWidgetMethodInvocation(methodName, true, params);
    }

    private String buildWidgetMethodInvocation(String methodName, boolean withPrefix, String... params) {
        StringBuilder builder = new StringBuilder();
        if (withPrefix) {
            builder.append("javascript:");
        }
        builder.append(methodName);

        if (params.length > 0) {
            String resultParamsStr = StringUtils.join("','", "('", "')", params);
            builder.append(resultParamsStr);
        } else {
            builder.append("()");
        }

        return builder.toString();
    }
}