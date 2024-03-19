package org.infobip.mobile.messaging.chat.core;

import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethods.mobileChatPause;
import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethods.mobileChatResume;
import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethods.sendContextualData;
import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethods.sendDraft;
import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethods.sendMessage;
import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethods.sendMessageWithAttachment;
import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethods.setLanguage;
import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethods.setTheme;
import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethods.showThreadList;
import static org.infobip.mobile.messaging.util.StringUtils.isNotBlank;

import android.os.Handler;
import android.os.Looper;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.chat.attachments.InAppChatMobileAttachment;
import org.infobip.mobile.messaging.chat.view.InAppChatWebView;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.Locale;

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
            String script = buildWidgetMethodInvocation(sendMessage.name(), message);
            executeScript(script);
        }
    }

    @Override
    public void sendChatMessage(String message, InAppChatMobileAttachment attachment) {
        String base64UrlString = attachment.base64UrlString();
        String fileName = attachment.getFileName();

        // message can be null - its OK
        if (canSendMessage(base64UrlString)) {
            String script = buildWidgetMethodInvocation(sendMessageWithAttachment.name(), message, base64UrlString, fileName);
            executeScript(script);
        } else {
            MobileMessagingLogger.e("[InAppChat] can't send attachment, base64 is empty");
        }
    }

    @Override
    public void sendInputDraft(String draft) {
        executeScript(buildWidgetMethodInvocation(sendDraft.name(), draft));
    }

    @Override
    public void setLanguage(Locale locale) {
        if (locale != null) {
            Language widgetLanguage = Language.findLanguage(locale);
            if (widgetLanguage == null) {
                MobileMessagingLogger.e("Language " + locale + " is not supported. Used default language " + Language.ENGLISH.getLocale());
                widgetLanguage = Language.ENGLISH;
            }
            String script = buildWidgetMethodInvocation(setLanguage.name(), widgetLanguage.getLocale());
            executeScript(script);
        }
    }

    @Override
    public void sendContextualData(String data, InAppChatMultiThreadFlag multiThreadFlag) {
        if (!data.isEmpty()) {
            executeScript(sendContextualData.name() + "(" + data + ", '" + multiThreadFlag + "')");
        }
    }

    @Override
    public void showThreadList() {
        executeScript(buildWidgetMethodInvocation(showThreadList.name()));
    }

    @Override
    public void mobileChatPause(MobileMessaging.ResultListener<String> resultListener) {
        executeScript(buildWidgetMethodInvocation(mobileChatPause.name()), resultListener);
    }

    @Override
    public void mobileChatResume(MobileMessaging.ResultListener<String> resultListener) {
        executeScript(buildWidgetMethodInvocation(mobileChatResume.name()), resultListener);
    }

    @Override
    public void setWidgetTheme(String themeName, MobileMessaging.ResultListener<String> resultListener) {
        executeScript(buildWidgetMethodInvocation(setTheme.name(), themeName), resultListener);
    }

    /**
     * Executes JS script on UI thread.
     *
     * @param script to be executed
     */
    private void executeScript(String script) {
        executeScript(script, null);
    }

    /**
     * Executes JS script on UI thread with result listener.
     *
     * @param script         to be executed
     * @param resultListener notify about result
     */
    private void executeScript(String script, MobileMessaging.ResultListener<String> resultListener) {
        if (webView != null) {
            try {
                handler.post(() -> webView.evaluateJavascript(script, value -> {
                    String valueToLog = (value != null && !"null".equals(value)) ? ":" + value : "";
                    MobileMessagingLogger.d(TAG, "Called Widget API: " + script + valueToLog);
                    if (resultListener != null)
                        resultListener.onResult(new Result<>(valueToLog));
                }));
            } catch (Exception e) {
                if (resultListener != null)
                    resultListener.onResult(new Result<>(MobileMessagingError.createFrom(e)));
                MobileMessagingLogger.e("Failed to execute webView JS script" + e.getMessage());
            }
        } else if (resultListener != null) {
            resultListener.onResult(new Result<>(MobileMessagingError.createFrom(new IllegalStateException("InAppChatWebView is null."))));
        }
    }

    private boolean canSendMessage(String message) {
        return isNotBlank(message);
    }

    private String buildWidgetMethodInvocation(String methodName, String... params) {
        StringBuilder builder = new StringBuilder();
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