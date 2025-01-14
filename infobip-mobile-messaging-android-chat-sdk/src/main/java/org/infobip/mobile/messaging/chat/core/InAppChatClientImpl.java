package org.infobip.mobile.messaging.chat.core;

import android.os.Handler;
import android.os.Looper;

import org.infobip.mobile.messaging.MobileMessaging;
import org.infobip.mobile.messaging.chat.attachments.InAppChatMobileAttachment;
import org.infobip.mobile.messaging.chat.view.InAppChatWebView;
import org.infobip.mobile.messaging.logging.MobileMessagingLogger;
import org.infobip.mobile.messaging.mobileapi.MobileMessagingError;
import org.infobip.mobile.messaging.mobileapi.Result;
import org.infobip.mobile.messaging.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethod.mobileChatPause;
import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethod.mobileChatResume;
import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethod.sendContextualData;
import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethod.sendDraft;
import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethod.sendMessage;
import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethod.sendMessageWithAttachment;
import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethod.setLanguage;
import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethod.setTheme;
import static org.infobip.mobile.messaging.chat.core.InAppChatWidgetMethod.showThreadList;
import static org.infobip.mobile.messaging.util.StringUtils.isNotBlank;

public class InAppChatClientImpl implements InAppChatClient {

    private static final int MAX_ALLOWED_SCRIPT_LENGTH = 200;
    private static final int MAX_ALLOWED_ARGUMENT_LENGTH = 50;
    private static final int ARGUMENT_VISIBLE_PART_LENGTH = 15;

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
            MobileMessagingLogger.e(TAG,"[InAppChat] can't send attachment, base64 is empty");
        }
    }

    @Override
    public void sendChatMessageDraft(String draft) {
        executeScript(buildWidgetMethodInvocation(sendDraft.name(), draft));
    }

    @Override
    public void setLanguage(Locale locale) {
        if (locale != null) {
            Language widgetLanguage = Language.findLanguage(locale);
            if (widgetLanguage == null) {
                MobileMessagingLogger.e(TAG,"Language " + locale + " is not supported. Used default language " + Language.ENGLISH.getLocale());
                widgetLanguage = Language.ENGLISH;
            }
            String script = buildWidgetMethodInvocation(setLanguage.name(), widgetLanguage.getLocale());
            executeScript(script);
        }
    }

    @Override
    public void sendContextualData(String data, MultithreadStrategy multiThreadFlag, MobileMessaging.ResultListener<String> resultListener) {
        if (data == null || data.isEmpty()){
            resultListener.onResult(new Result<>(MobileMessagingError.createFrom(new IllegalArgumentException("Could not send contextual data. Data is null or empty."))));
            return;
        }
        if (multiThreadFlag == null){
            resultListener.onResult(new Result<>(MobileMessagingError.createFrom(new IllegalArgumentException("Could not send contextual data. MultiThreadFlag is null."))));
            return;
        }
        executeScript(sendContextualData.name() + "(" + data + ", '" + multiThreadFlag + "')", resultListener);
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
                    String valueToLog = (value != null && !"null".equals(value)) ? " => " + value : "";
                    String scriptToLog = shortenScript(script);
                    MobileMessagingLogger.d(TAG, "Called Widget API: " + scriptToLog + valueToLog);
                    if (resultListener != null)
                        resultListener.onResult(new Result<>(valueToLog));
                }));
            } catch (Exception e) {
                if (resultListener != null)
                    resultListener.onResult(new Result<>(MobileMessagingError.createFrom(e)));
                MobileMessagingLogger.e(TAG, "Failed to execute webView JS script " + shortenScript(script) + " " + e.getMessage());
            }
        } else if (resultListener != null) {
            resultListener.onResult(new Result<>(MobileMessagingError.createFrom(new IllegalStateException("Failed to execute webView JS script " + shortenScript(script) + " InAppChatWebView is null."))));
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

    private String shortenScript(String script) {
        if (script != null && script.length() > MAX_ALLOWED_SCRIPT_LENGTH) {
            StringBuilder builder = new StringBuilder();
            int methodNameEndIndex = script.indexOf("(");
            if (methodNameEndIndex > 0) {
                String methodName = script.substring(0, methodNameEndIndex);
                builder.append(methodName);
                String paramsSubstring = script.substring(methodNameEndIndex + 1, script.length() - 1);
                String[] paramsArray = paramsSubstring.split(",");
                if (paramsArray.length > 0) {
                    List<String> shortenedParams = new ArrayList<>();
                    for (String param : paramsArray) {
                        String value = param.replace("'", "");
                        if (value.length() > MAX_ALLOWED_ARGUMENT_LENGTH) {
                            value = value.substring(0, ARGUMENT_VISIBLE_PART_LENGTH) + "..." + value.substring(value.length() - ARGUMENT_VISIBLE_PART_LENGTH);
                        }
                        shortenedParams.add(value);
                    }
                    String params = StringUtils.join("','", "('", "')", shortenedParams.toArray(new String[0]));
                    builder.append(params);
                }
                else {
                    builder.append("()");
                }
            }
            return builder.toString();
        }
        return script;
    }
}